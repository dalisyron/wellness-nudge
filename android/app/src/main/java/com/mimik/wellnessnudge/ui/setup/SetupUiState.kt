package com.mimik.wellnessnudge.ui.setup

import androidx.compose.runtime.Immutable
import com.mimik.wellnessnudge.bootstrap.BootstrapState
import com.mimik.wellnessnudge.bootstrap.BootstrapState.Phase
import com.mimik.wellnessnudge.bootstrap.ModelSetupItem
import com.mimik.wellnessnudge.ui.format.formatBytes
import java.util.Locale
import kotlin.math.roundToInt

/** The steps of first-run setup, in order, with the copy the checklist shows for each. */
enum class SetupStep(val title: String, val detail: String) {
    Runtime("Start the mimOE runtime", "Embedded edge runtime on this phone"),
    SignIn("Sign in", "mimik developer identity"),
    Inference("Deploy AI inference", "mILM, a local OpenAI-compatible API"),
    Mim("Deploy the wellness-nudge mim", "Your private coaching microservice"),
    Models("Download AI models", "Two models, about 2.2 GB, one time"),
}

enum class StepStatus { Done, Active, Pending, Failed }

/** One checklist row; [message] explains a failure. */
@Immutable
data class StepState(val step: SetupStep, val status: StepStatus, val message: String? = null)

enum class ModelStatus { Waiting, Starting, Downloading, Ready, Failed }

/**
 * One model card: [details] is the model and its size ("SmolLM2 360M · Q8_0 · 368 MB"),
 * [statusLine] what is happening to it ("84 / 368 MB · 23%"), and [progressDescription]
 * the progress as TalkBack reads it ("84 of 368 MB").
 */
@Immutable
data class ModelCardState(
    val id: String,
    val name: String,
    val details: String,
    val status: ModelStatus,
    val progress: Float,
    val statusLine: String,
    val progressDescription: String? = null,
)

/**
 * Where setup stands as a whole, which sets the orb and the bottom action: [Paused] means a
 * model download failed and nothing else is running, so setup waits for a retry.
 */
enum class SetupStatus { Working, Ready, Paused, Failed }

/** Everything the setup screen shows. [models] stays empty until the model phase. */
@Immutable
data class SetupUiState(
    val steps: List<StepState>,
    val models: List<ModelCardState>,
    val status: SetupStatus,
)

/** Maps bootstrap progress onto the checklist, the model cards and the overall status. */
fun BootstrapState.toSetupUiState(): SetupUiState = when (this) {
    BootstrapState.NotStarted -> SetupUiState(stepsAt(0, StepStatus.Active), emptyList(), SetupStatus.Working)
    is BootstrapState.Step -> SetupUiState(stepsAt(phase.row, StepStatus.Active), emptyList(), SetupStatus.Working)
    is BootstrapState.Failed -> SetupUiState(
        steps = stepsAt(phase.row.coerceAtMost(SetupStep.Models.ordinal), StepStatus.Failed, message),
        models = emptyList(),
        status = SetupStatus.Failed,
    )
    is BootstrapState.Setup -> {
        val downloads = when {
            allReady -> StepStatus.Done
            anyInFlight || !anyFailed -> StepStatus.Active
            // Nothing runs until a retry; the failed model's card says why, with its Retry.
            else -> StepStatus.Failed
        }
        SetupUiState(
            steps = stepsAt(SetupStep.Models.ordinal, downloads),
            models = items.map { it.toCardState() },
            status = when (downloads) {
                StepStatus.Done -> SetupStatus.Ready
                StepStatus.Failed -> SetupStatus.Paused
                else -> SetupStatus.Working
            },
        )
    }
    is BootstrapState.Ready -> SetupUiState(stepsAt(SetupStep.entries.size, StepStatus.Active), emptyList(), SetupStatus.Ready)
}

/** Rows before [current] are done, [current] has [status], and the rest are pending. */
private fun stepsAt(current: Int, status: StepStatus, message: String? = null): List<StepState> =
    SetupStep.entries.map { step ->
        when {
            step.ordinal < current -> StepState(step, StepStatus.Done)
            step.ordinal == current -> StepState(step, status, message)
            else -> StepState(step, StepStatus.Pending)
        }
    }

/** The checklist row a bootstrap phase belongs to; READY is past the last row. */
private val Phase.row: Int
    get() = when (this) {
        Phase.START_RUNTIME -> SetupStep.Runtime.ordinal
        Phase.LOGIN -> SetupStep.SignIn.ordinal
        Phase.DEPLOY_MILM -> SetupStep.Inference.ordinal
        Phase.DEPLOY_MIM -> SetupStep.Mim.ordinal
        Phase.QUEUE_MODELS, Phase.DOWNLOAD_MODELS -> SetupStep.Models.ordinal
        Phase.READY -> SetupStep.entries.size
    }

private fun ModelSetupItem.toCardState(): ModelCardState {
    val size = formatBytes(approxBytes)
    val fraction = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
    val card = ModelCardState(
        id = id,
        name = displayName,
        details = "$technicalName · $size",
        status = ModelStatus.Waiting,
        progress = 0f,
        statusLine = "Waiting in queue",
    )
    return when (state) {
        ModelSetupItem.State.Pending -> card
        // mILM reports the size with its first progress event.
        ModelSetupItem.State.Downloading -> if (totalBytes <= 0) {
            card.copy(status = ModelStatus.Starting, statusLine = "Starting download…")
        } else {
            val done = amountIn(downloadedBytes.coerceAtMost(totalBytes), totalBytes)
            val total = formatBytes(totalBytes)
            // Rounded, but never 100% until mILM confirms the model.
            val percent = (fraction * 100).roundToInt().coerceAtMost(99)
            card.copy(
                status = ModelStatus.Downloading,
                progress = fraction,
                statusLine = "$done / $total · $percent%",
                progressDescription = "$done of $total",
            )
        }
        ModelSetupItem.State.Ready -> card.copy(
            status = ModelStatus.Ready,
            progress = 1f,
            statusLine = "Ready · $size on this phone",
        )
        ModelSetupItem.State.Failed -> card.copy(
            status = ModelStatus.Failed,
            progress = fraction,
            statusLine = errorMessage ?: "Download failed. Tap Retry to try again.",
        )
    }
}

/** [bytes] as a bare number in the unit [formatBytes] picks for [total]: 84 (MB), 0.7 (GB). */
private fun amountIn(bytes: Long, total: Long): String =
    if (total >= GiB) String.format(Locale.US, "%.1f", bytes.toDouble() / GiB) else "${bytes / MiB}"

private const val MiB = 1024L * 1024
private const val GiB = MiB * 1024
