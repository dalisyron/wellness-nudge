package com.mimik.wellnessnudge.bootstrap

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mimik.wellnessnudge.BuildConfig
import com.mimik.wellnessnudge.api.NudgeApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Drives one-time setup: starts the embedded runtime, logs in, deploys
 * mILM + the wellness-nudge mim, then provisions on-device language models.
 *
 * UI states:
 * - Brief [BootstrapState.Step] frames during prep (a few seconds).
 * - [BootstrapState.Setup] gates the main app behind explicit model
 *   downloads. The user must tap Continue from the setup screen.
 * - [BootstrapState.Ready] unlocks the main app.
 * - [BootstrapState.Failed] for pre-Setup failures (token expired etc.).
 */
class BootstrapViewModel(app: Application) : AndroidViewModel(app) {

    private val edge = EdgeRuntime(app.applicationContext)
    private val _state = MutableStateFlow<BootstrapState>(BootstrapState.NotStarted)
    val state: StateFlow<BootstrapState> = _state.asStateFlow()

    private val prefs = app.getSharedPreferences("wellness_nudge", Context.MODE_PRIVATE)

    val edgeRuntime: EdgeRuntime get() = edge

    // Used by the runtime sheet; created on first use, once the runtime (and its port) is up.
    private val runtimeApi: NudgeApi by lazy {
        NudgeApi.create(mimBaseUrl(edge.client.mimOEPort), BuildConfig.WELLNESS_API_KEY)
    }

    // The run in progress, so a second start can't race it.
    private var runJob: Job? = null

    // Hash of the mim this run deployed. Saved only once every model is in place, so an
    // interrupted first-run download can't unlock the offline fast path with models missing.
    private var deployedHash: String? = null

    /**
     * Starts setup from the beginning. Only [BootstrapState.NotStarted] starts a run: the
     * activity calls this on every onCreate, and a recreation (theme, font size, locale) must
     * not restart setup that is running, waiting for the user, or showing a failure.
     */
    fun start() {
        if (_state.value != BootstrapState.NotStarted || runJob?.isActive == true) return
        _state.value = BootstrapState.Step(BootstrapState.Phase.START_RUNTIME)
        runJob = viewModelScope.launch(Dispatchers.IO) { run() }
    }

    fun retry() {
        if (runJob?.isActive == true) return
        _state.value = BootstrapState.NotStarted
        start()
    }

    /**
     * Triggered by the user from the setup screen once every model is
     * Ready. Transitions to [BootstrapState.Ready], which unlocks the
     * main app.
     */
    fun continueToMain() {
        val current = _state.value as? BootstrapState.Setup ?: return
        if (!current.allReady) return
        transitionToReady()
    }

    /**
     * Re-attempt a single failed model download. Other models are
     * untouched.
     */
    fun retryModel(modelId: String) {
        val current = _state.value as? BootstrapState.Setup ?: return
        val idx = current.items.indexOfFirst { it.id == modelId }
        if (idx < 0) return
        val spec = Models.ALL.firstOrNull { it.download.id == modelId } ?: return
        viewModelScope.launch(Dispatchers.IO) { downloadModel(spec, idx) }
    }

    /**
     * Collects what the runtime sheet shows: the runtime port, the mim's health, which models
     * mILM can serve, and how many nudges are stored. Meant for [BootstrapState.Ready].
     */
    suspend fun loadRuntimeInfo(): RuntimeInfo = withContext(Dispatchers.IO) {
        val readyIds = edge.listModels().filter { it.readyToUse }.map { it.id }.toSet()
        RuntimeInfo(
            port = edge.client.mimOEPort,
            mimApiRoot = EdgeRuntime.WELLNESS_API_ROOT,
            mimHealth = attempt { runtimeApi.health().data },
            models = Models.ALL.map { spec ->
                RuntimeModel(
                    id = spec.download.id,
                    displayName = spec.displayName,
                    technicalName = spec.technicalName,
                    sizeBytes = spec.approxBytes,
                    ready = spec.download.id in readyIds,
                )
            },
            nudgeCount = attempt { runtimeApi.listHistory(limit = 1).data?.total },
        )
    }

    private suspend fun <T> attempt(block: suspend () -> T?): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        Log.d(TAG, "runtime info: ${t.message}")
        null
    }

    private suspend fun run() {
        try {
            // 1. Start the embedded mimOE runtime.
            _state.value = BootstrapState.Step(BootstrapState.Phase.START_RUNTIME)
            val port = withContext(Dispatchers.IO) { edge.startRuntime() }
            Log.i(TAG, "mimOE up on port $port")

            // 1a. Offline fast-path. If the previously-deployed mim is the
            // SAME version we'd deploy now (same tar hash) AND it's healthy,
            // we can skip login, redeploy, and model checks entirely. This
            // makes airplane-mode launches work after the first WiFi setup.
            //
            // If the bundled tar has changed (we shipped a mim update), the
            // hash differs and we fall through to the full bootstrap so the
            // new version gets deployed.
            val bundledHash = bundledMimHash()
            val lastDeployedHash = prefs.getString(KEY_DEPLOYED_HASH, null)
            if (bundledHash == lastDeployedHash && existingMimIsHealthy(port)) {
                Log.i(TAG, "Existing mim matches bundle ($bundledHash) and is healthy — fast-path to Ready")
                transitionToReady()
                return
            }
            Log.i(TAG, "Full bootstrap needed (bundle=$bundledHash, deployed=$lastDeployedHash)")

            // 2. Login with developer token.
            _state.value = BootstrapState.Step(BootstrapState.Phase.LOGIN)
            if (edge.developerTokenExpired()) {
                fail(BootstrapState.Phase.LOGIN,
                    "Your developer token has expired. Refresh it on console.mimik.com and rebuild the app.")
                return
            }
            try {
                edge.loginWithDeveloperToken()
            } catch (t: Throwable) {
                val msg = t.message.orEmpty()
                val friendly = if (msg.contains("edge ID token", ignoreCase = true) ||
                    msg.contains("Connection failed", ignoreCase = true)) {
                    "We couldn’t reach the mimik identity service to set up your local AI. " +
                        "This usually means the phone is offline or on an IPv6-only cellular " +
                        "network. Connect to Wi-Fi and tap Try again."
                } else {
                    "Couldn’t activate the mimik runtime: $msg"
                }
                fail(BootstrapState.Phase.LOGIN, friendly, t)
                return
            }

            // 3. Deploy mILM (AI inference microservice).
            _state.value = BootstrapState.Step(BootstrapState.Phase.DEPLOY_MILM)
            val milm = withContext(Dispatchers.IO) { edge.deployMilm() }
            if (milm.error != null) {
                Log.w(TAG, "mILM deploy: ${milm.error.message}")
            }

            // 4. Deploy the wellness-nudge mim.
            _state.value = BootstrapState.Step(BootstrapState.Phase.DEPLOY_MIM)
            val wellness = withContext(Dispatchers.IO) {
                edge.deployWellnessNudge(
                    wellnessApiKey = BuildConfig.WELLNESS_API_KEY,
                    defaultInferenceModel = Models.SMOLLM2.download.id,
                )
            }
            if (wellness.error != null) {
                Log.w(TAG, "wellness-nudge deploy: ${wellness.error.message}")
            } else {
                // Remember we deployed this exact tar so the next launch can fast-path if the
                // bundle hasn't changed, once the models are in place too (rememberDeployment).
                deployedHash = bundledHash
            }

            // 5. Snapshot what mILM already has and build the setup items.
            _state.value = BootstrapState.Step(BootstrapState.Phase.QUEUE_MODELS)
            val existing = edge.listModels()
            val readyIds = existing.filter { it.readyToUse }.map { it.id }.toSet()
            val initialItems = Models.ALL.map { spec ->
                ModelSetupItem(
                    id = spec.download.id,
                    displayName = spec.displayName,
                    technicalName = spec.technicalName,
                    purpose = spec.purpose,
                    approxBytes = spec.approxBytes,
                    state = if (spec.download.id in readyIds) {
                        ModelSetupItem.State.Ready
                    } else {
                        ModelSetupItem.State.Pending
                    },
                )
            }

            // 6. Shortcut: if every model is already on the device,
            // we skip the setup screen and unlock the app immediately.
            if (initialItems.all { it.state == ModelSetupItem.State.Ready }) {
                Log.i(TAG, "All models cached — skipping setup screen")
                transitionToReady()
                return
            }

            _state.value = BootstrapState.Setup(initialItems)

            // 7. Download each missing model sequentially. mILM serves
            // one model at a time anyway, so we don't parallelize.
            initialItems.forEachIndexed { idx, item ->
                if (item.state == ModelSetupItem.State.Ready) return@forEachIndexed
                val spec = Models.ALL[idx]
                downloadModel(spec, idx)
            }

            // State stays as Setup — the user taps Continue to unlock.
        } catch (t: Throwable) {
            Log.e(TAG, "Bootstrap failed", t)
            val phase = (_state.value as? BootstrapState.Step)?.phase
                ?: BootstrapState.Phase.START_RUNTIME
            fail(phase, t.message ?: "Unknown error", t)
        }
    }

    private suspend fun downloadModel(spec: ModelSpec, idx: Int) {
        updateItem(idx) {
            it.copy(
                state = ModelSetupItem.State.Downloading,
                errorMessage = null,
                downloadedBytes = 0L,
                totalBytes = 0L,
            )
        }
        try {
            edge.queueModelWithProgress(spec.download) { downloaded, total ->
                updateItem(idx) {
                    it.copy(downloadedBytes = downloaded, totalBytes = total)
                }
            }
            // mILM's SSE stream closes on success AND failure — confirm
            // by asking mILM whether the model is actually usable.
            val verified = edge.listModels().firstOrNull { it.id == spec.download.id }
            if (verified?.readyToUse == true) {
                updateItem(idx) {
                    it.copy(
                        state = ModelSetupItem.State.Ready,
                        downloadedBytes = it.totalBytes.takeIf { t -> t > 0 } ?: it.approxBytes,
                    )
                }
                Log.i(TAG, "Model ${spec.download.id} ready")
                if ((_state.value as? BootstrapState.Setup)?.allReady == true) rememberDeployment()
            } else {
                updateItem(idx) {
                    it.copy(
                        state = ModelSetupItem.State.Failed,
                        errorMessage = "Download didn’t complete. Check your connection and tap Try again.",
                    )
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Download failed for ${spec.download.id}", t)
            updateItem(idx) {
                it.copy(
                    state = ModelSetupItem.State.Failed,
                    errorMessage = friendlyError(t),
                )
            }
        }
    }

    private fun friendlyError(t: Throwable): String {
        val msg = t.message.orEmpty()
        return when {
            msg.contains("Unable to resolve", ignoreCase = true) ||
                msg.contains("Failed to connect", ignoreCase = true) ||
                msg.contains("UnknownHost", ignoreCase = true) ->
                "Couldn’t reach the download server. Check your internet and tap Try again."
            msg.contains("404", ignoreCase = true) ->
                "This model file moved or is no longer available."
            else -> "Download failed. Tap Try again."
        }
    }

    private fun updateItem(idx: Int, transform: (ModelSetupItem) -> ModelSetupItem) {
        val current = _state.value as? BootstrapState.Setup ?: return
        if (idx !in current.items.indices) return
        val newItems = current.items.toMutableList().also {
            it[idx] = transform(it[idx])
        }
        _state.value = current.copy(items = newItems)
    }

    /**
     * SHA-256 of the bundled wellness-nudge tar. Used to detect when a
     * fresh APK ships a different mim version, so we know to force-redeploy
     * instead of fast-pathing on the previously-deployed version.
     */
    private suspend fun bundledMimHash(): String = withContext(Dispatchers.IO) {
        val context = getApplication<Application>().applicationContext
        val digest = MessageDigest.getInstance("SHA-256")
        context.assets.open("mims/wellness-nudge-v1-1.0.0.tar").use { input ->
            val buf = ByteArray(16 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                digest.update(buf, 0, n)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }.take(16)
    }

    /**
     * Probe the wellness-nudge mim's `/healthcheck` directly over loopback.
     * If it answers 200, the mim is deployed AND running, which means we
     * already have everything we need on the device — no internet required.
     * Bounded to a short timeout so we don't slow down the "fresh install"
     * path where the mim genuinely isn't deployed yet.
     */
    private suspend fun existingMimIsHealthy(port: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "${mimBaseUrl(port)}/healthcheck"
            val client = OkHttpClient.Builder()
                .connectTimeout(2, TimeUnit.SECONDS)
                .readTimeout(2, TimeUnit.SECONDS)
                .callTimeout(3, TimeUnit.SECONDS)
                .build()
            val req = Request.Builder().url(url).get().build()
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (t: Throwable) {
            Log.d(TAG, "healthcheck probe failed: ${t.message}")
            false
        }
    }

    /** Lets the next launch take the offline fast path: the mim is deployed and every model is ready. */
    private fun rememberDeployment() {
        deployedHash?.let { prefs.edit().putString(KEY_DEPLOYED_HASH, it).apply() }
    }

    private fun transitionToReady() {
        rememberDeployment()
        _state.value = BootstrapState.Ready(
            mimBaseUrl = mimBaseUrl(edge.client.mimOEPort),
            apiKey = BuildConfig.WELLNESS_API_KEY,
        )
    }

    private fun mimBaseUrl(port: Int): String =
        "http://127.0.0.1:$port/${BuildConfig.MIMIK_CLIENT_ID}${EdgeRuntime.WELLNESS_API_ROOT}"

    private fun fail(phase: BootstrapState.Phase, message: String, cause: Throwable? = null) {
        _state.value = BootstrapState.Failed(phase, message, cause)
    }

    companion object {
        private const val TAG = "Bootstrap"
        private const val KEY_DEPLOYED_HASH = "deployed_mim_hash"
    }
}
