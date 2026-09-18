package com.mimik.wellnessnudge.bootstrap

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.mimik.mimoeclient.MimOEClient
import com.mimik.mimoeclient.MimOERequestError
import com.mimik.mimoeclient.MimOERequestResponse
import com.mimik.mimoeclient.MimOEResponseHandler
import com.mimik.mimoeclient.Util
import com.mimik.mimoeclient.authobject.DeveloperTokenLoginConfig
import com.mimik.mimoeclient.microserviceobjects.MicroserviceDeploymentConfig
import com.mimik.mimoeclient.microserviceobjects.MicroserviceDeploymentStatus
import com.mimik.mimoeclient.milm.MimOEClientMilm
import com.mimik.mimoeclient.milm.model.MilmModel
import com.mimik.mimoeclient.milm.model.ModelDownload
import com.mimik.mimoeclient.milm.model.ModelStatus
import com.mimik.mimoeclient.mimoeservice.MimOEConfig
import com.mimik.mimoeclient.model.AccessTokenPayload
import com.mimik.wellnessnudge.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.awaitResponse
import java.io.IOException
import java.io.InputStream
import java.time.Instant
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Thin wrapper around the mimik SDK that exposes coroutine-friendly steps.
 *
 * Lifecycle:
 *   1. startRuntime()         — boots the embedded mimOE runtime
 *   2. loginWithDeveloperToken() — auths against console.mimik.com (mID)
 *   3. deployMilm()           — uploads + starts the AI inference mim
 *   4. deployWellnessNudge()  — uploads + starts our mim, with INFERENCE_* env vars
 *   5. queueModel(...) / pollModels() — manage GGUF downloads via mILM
 *
 * All blocking SDK calls are wrapped so callers can suspend.
 */
class EdgeRuntime(private val context: Context) {

    private val mimOELicense: String by lazy {
        context.resources.openRawResource(
            context.resources.getIdentifier("mimoe_license", "raw", context.packageName)
        ).bufferedReader().use { it.readText().trim() }
    }

    // NOTE: SDK 3.18.0 doesn't expose a clean toggle for mesh / LAN
    // discovery. Tried three knobs that didn't help:
    //   - .capability(1) (AI-only bit): ignored, supernode discovery still ran
    //   - Stripping CHANGE_WIFI_MULTICAST_STATE: SDK crashes on
    //     MulticastLock.acquire (it assumes the permission is granted)
    //   - .useNetworkInterface("lo"): only affects the inbound bind; the
    //     runtime's outbound mDS calls still reach LAN peers.
    // Practical workaround: don't run another mimOE node on the same LAN.
    // The mesh-join is identity metadata only — no biometric/inference data
    // is shared with peers — see [[project-android-app]] in memory.
    val client: MimOEClient = MimOEClient(
        context,
        MimOEConfig()
            .logLevel("debug")
            .license(mimOELicense)
    )

    private val gson = Gson()

    /**
     * Block on a thread-pool until the runtime is up. Idempotent: when setup is retried after
     * a later step failed, the runtime is already running, and the SDK refuses a second start
     * (startMimOESynchronously returns false while its service is bound), so reuse it.
     */
    @Throws(IllegalStateException::class)
    fun startRuntime(): Int {
        if (!client.isMimOEReady) {
            val started = client.startMimOESynchronously()
            check(started) { "MimOEClient.startMimOESynchronously() returned false" }
        }
        // mILM uses the same API key our mim already expects via INFERENCE_API_KEY=1234.
        MimOEClientMilm.setMilmApiKey(context, "1234")
        return client.mimOEPort
    }

    /** Has the developer token already expired? Quick local check via JWT exp claim. */
    fun developerTokenExpired(): Boolean = try {
        val payload = Util.getJWTPayload(
            BuildConfig.MIMIK_DEVELOPER_ID_TOKEN,
            AccessTokenPayload::class.java
        )
        Instant.ofEpochSecond(payload.exp).isBefore(Instant.now())
    } catch (t: Throwable) {
        Log.w(TAG, "developerTokenExpired: failed to parse — assuming expired", t)
        true
    }

    /**
     * Authenticate against console.mimik.com using the bundled developer
     * token. Retries on transient "Failed to fetch edge ID token" errors:
     * mimOE's JSON-RPC endpoint isn't always ready the instant
     * `startMimOESynchronously()` returns, so the first login attempt can
     * race the runtime's internal identity bootstrap.
     */
    suspend fun loginWithDeveloperToken() {
        val maxAttempts = 5
        var lastError: Throwable? = null
        for (attempt in 1..maxAttempts) {
            try {
                attemptLogin()
                return
            } catch (t: Throwable) {
                lastError = t
                Log.w(TAG, "login attempt $attempt failed: ${t.message}")
                // Most "edge ID token" failures resolve within ~1 s after
                // mimOE starts. Back off a little between attempts.
                delay(500L * attempt)
            }
        }
        throw lastError ?: IOException("loginWithDeveloperToken failed after $maxAttempts attempts")
    }

    private suspend fun attemptLogin(): Unit = suspendCancellableCoroutine { cont ->
        val cfg = DeveloperTokenLoginConfig().apply {
            setAuthorizationRootUri(MID_URL)
            developerToken = BuildConfig.MIMIK_DEVELOPER_ID_TOKEN
            clientId = BuildConfig.MIMIK_CLIENT_ID
        }
        client.loginWithDeveloperToken(context, cfg, object : MimOEResponseHandler {
            override fun onError(err: MimOERequestError) {
                cont.resumeWithException(
                    IOException("loginWithDeveloperToken failed: ${err.errorMessage}")
                )
            }
            override fun onResponse(resp: MimOERequestResponse) {
                cont.resume(Unit)
            }
        })
    }

    /** Deploy mILM. Returns the status object so callers can inspect errors. */
    fun deployMilm(): MicroserviceDeploymentStatus {
        val raw = context.resources.openRawResource(
            context.resources.getIdentifier("milm_v1_1_7_0", "raw", context.packageName)
        )
        return MimOEClientMilm.deployDefaultMilmMicroservice(context, client, raw)
    }

    /**
     * Upload + deploy our wellness-nudge mim. Mirrors how MimOEClientMilm
     * does it for the AI inference mim:
     *   - Pass the mimik access token (not the developer client ID) as the
     *     first arg — the SDK uses it as a Bearer token for MCM calls.
     *   - Set resourceStream + apiRootUri on the config; deployMimOEMicroservice
     *     auto-uploads the image, no separate upload call needed.
     */
    fun deployWellnessNudge(
        wellnessApiKey: String,
        defaultInferenceModel: String,
    ): MicroserviceDeploymentStatus {
        val accessToken = client.mimikAccessToken
            ?: error("No mimikAccessToken — login must complete before deploying mims")

        // On Android mILM serves the OpenAI-compatible endpoint at
        // /{clientId}/milm/v1/chat/completions (not the desktop's
        // /mimik-ai/openai/v1/...). Tell our mim where to find it.
        val inferencePath = "/${BuildConfig.MIMIK_CLIENT_ID}/milm/v1/chat/completions"

        // Remove any existing container first so we always pick up the
        // current tar + env vars on rebuild. Errors are non-fatal —
        // first launch just has nothing to remove.
        val removeCfg = MicroserviceDeploymentConfig().apply {
            name = WELLNESS_IMAGE_NAME
            containerName = WELLNESS_CONTAINER_NAME
        }
        try {
            client.removeMimOEMicroservice(accessToken, removeCfg)
        } catch (t: Throwable) {
            Log.d(TAG, "removeMimOEMicroservice: ${t.message}")
        }

        val cfg = MicroserviceDeploymentConfig().apply {
            name = WELLNESS_IMAGE_NAME
            containerName = WELLNESS_CONTAINER_NAME
            filename = WELLNESS_TAR
            resourceStream = context.assets.open("mims/$WELLNESS_TAR")
            apiRootUri = Uri.parse(WELLNESS_API_ROOT)
            envVariables = mapOf(
                "API_KEY" to wellnessApiKey,
                "INFERENCE_API_KEY" to "1234",
                "INFERENCE_MODEL" to defaultInferenceModel,
                "INFERENCE_URL_PATH" to inferencePath,
            )
        }
        return client.deployMimOEMicroservice(accessToken, cfg)
    }

    /** List models known to mILM right now. */
    fun listModels(): List<MilmModel> = try {
        val resp = MimOEClientMilm.getMilmProvider(context, client).getModels().execute()
        if (resp.isSuccessful) resp.body()?.data ?: emptyList() else emptyList()
    } catch (t: Throwable) {
        Log.w(TAG, "listModels failed", t)
        emptyList()
    }

    /**
     * Queue a model for download. Emits progress via the [onProgress] callback.
     * Suspends until the download stream closes (success or error).
     */
    suspend fun queueModelWithProgress(
        model: ModelDownload,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ) = suspendCancellableCoroutine<Unit> { cont ->
        val call: Call<ResponseBody> = MimOEClientMilm.getMilmProvider(context, client)
            .queueModel(model)
        val thread = Thread({
            try {
                val response = call.execute()
                if (!response.isSuccessful) {
                    cont.resumeWithException(
                        IOException("queueModel HTTP ${response.code()} ${response.message()}")
                    )
                    return@Thread
                }
                response.body()?.byteStream()?.bufferedReader()?.use { reader ->
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.startsWith("data:")) {
                            try {
                                val status = gson.fromJson(line.substring(5).trim(), ModelStatus::class.java)
                                onProgress(status.size, status.totalSize)
                                if (status.size == status.totalSize && status.totalSize > 0) {
                                    break
                                }
                            } catch (t: Throwable) {
                                Log.w(TAG, "queueModel parse: ${t.message} (line=$line)")
                            }
                        }
                    }
                }
                cont.resume(Unit)
            } catch (t: Throwable) {
                cont.resumeWithException(t)
            }
        }, "milm-queue-${model.id}").apply { isDaemon = true }
        cont.invokeOnCancellation { thread.interrupt() }
        thread.start()
    }

    /** Use mILM's chat completion as a smoke test — same path our mim uses internally. */
    suspend fun smokeTestInference(modelId: String): String? {
        val provider = MimOEClientMilm.getMilmProvider(context, client)
        val query = com.mimik.mimoeclient.milm.model.MilmQuery(
            model = modelId,
            messages = listOf(com.mimik.mimoeclient.milm.model.Message("user", "Say hi in 5 words.")),
            temperature = 0.4,
            maxTokens = 32,
            stream = false,
        )
        val resp = provider.sendCompletion(query).awaitResponse()
        return if (resp.isSuccessful) resp.body()?.string() else null
    }

    companion object {
        private const val TAG = "EdgeRuntime"
        const val MID_URL = "https://devconsole-mid.mimik.com"
        private const val WELLNESS_TAR = "wellness-nudge-v1-1.0.0.tar"
        private const val WELLNESS_IMAGE_NAME = "wellness-nudge-v1"
        private const val WELLNESS_CONTAINER_NAME = "wellness-nudge"
        const val WELLNESS_API_ROOT = "/wellness-nudge/v1"
    }
}
