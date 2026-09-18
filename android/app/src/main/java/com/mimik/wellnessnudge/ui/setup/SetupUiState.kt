package com.mimik.wellnessnudge.ui.setup

import androidx.compose.runtime.Immutable
import com.mimik.wellnessnudge.bootstrap.BootstrapState
import com.mimik.wellnessnudge.bootstrap.BootstrapState.Phase
import com.mimik.wellnessnudge.bootstrap.ModelSetupItem
import com.mimik.wellnessnudge.ui.format.formatBytes
import com.mimik.wellnessnudge.ui.format.formatBytesIn
import kotlin.math.roundToInt
import com.mimik.wellnessnudge.bootstrap.Models as ModelCatalog

/** The steps of first-run setup, in order, with the copy the checklist shows for each. */
enum class SetupStep(val title: String, val detail: String) {
    Runtime("Start the mimOE runtime", "Embedded edge runtime on this phone"),

    // The app activates the runtime with its own bundled key: nobody signs in.
    SignIn("Activate the runtime", "Uses the app’s mimik key · no account needed"),
    Inference("Deploy AI inference", "mILM, the AI engine on this phone"),
    Mim("Deploy the wellness-nudge mim", "Your private coaching microservice"),
    Models("Download AI models", "$ModelCount, ${formatBytes(ModelBytes)}, one time · Wi-Fi recommended"),
}

enum class StepStatus { Done, Active, Pending, Failed }

/** One checklist row; [message] explains a failure. [detail] is the line under the title. */
@Immutable
data class StepState(
    val step: SetupStep,
    val status: StepStatus,
    val message: String? = null,
    val detail: String = step.detail,
)

/**
 * [Upcoming]: the model phase hasn't begun (setup is still at an earlier step, or stopped
 * there), so nothing is known about it yet beyond its name and size.
 */
enum class ModelStatus { Upcoming, Waiting, Starting, Downloading, Ready, Failed }

/**
 * One model card: [details] is the model and its size ("SmolLM2 360M · Q8_0 · 386 MB"),
 * [statusLine] what is happening to it ("88 / 386 MB · 23%"), and [progressDescription]
 * the progress as TalkBack reads it ("88 of 386 MB").
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

/**
 * Everything the setup screen shows. [models] lists the models from the first frame, so the
 * size of what is coming is clear before it starts; [modelPhase] is set once they download.
 */
@Immutable
data class SetupUiState(
    val steps: List<StepState>,
    val models: List<ModelCardState>,
    val status: SetupStatus,
    val modelPhase: Boolean = false,
)

/** Maps bootstrap progress onto the checklist, the model cards and the overall status. */
fun BootstrapState.toSetupUiState(): SetupUiState = when (this) {
    BootstrapState.NotStarted -> SetupUiState(stepsAt(0, StepStatus.Active), upcomingModels(), SetupStatus.Working)
    is BootstrapState.Step -> SetupUiState(stepsAt(phase.row, StepStatus.Active), upcomingModels(), SetupStatus.Working)
    is BootstrapState.Failed -> SetupUiState(
        steps = stepsAt(phase.row.coerceAtMost(SetupStep.Models.ordinal), StepStatus.Failed, message),
        models = upcomingModels(),
        status = SetupStatus.Failed,
    )
    is BootstrapState.Setup -> {
        val downloads = when {
            allReady -> StepStatus.Done
            anyInFlight || !anyFailed -> StepStatus.Active
            // Nothing runs until a retry; the failed model's card says why, with its Try again.
            else -> StepStatus.Failed
        }
        SetupUiState(
            steps = stepsAt(SetupStep.Models.ordinal, downloads).map { step ->
                if (step.step == SetupStep.Models && downloads == StepStatus.Done) {
                    step.copy(detail = "$ModelCount, ${formatBytes(items.sumOf { it.approxBytes })} on this phone")
                } else {
                    step
                }
            },
            models = items.map { it.toCardState() },
            status = when (downloads) {
                StepStatus.Done -> SetupStatus.Ready
                StepStatus.Failed -> SetupStatus.Paused
                else -> SetupStatus.Working
            },
            modelPhase = true,
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

/** The models before their phase: what they are and their size, and that they come next. */
private fun upcomingModels(): List<ModelCardState> = ModelCatalog.ALL.map { spec ->
    ModelCardState(
        id = spec.download.id,
        name = spec.displayName,
        details = "${spec.technicalName} · ${formatBytes(spec.approxBytes)}",
        status = ModelStatus.Upcoming,
        progress = 0f,
        statusLine = "Up next",
    )
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
            val done = formatBytesIn(downloadedBytes.coerceAtMost(totalBytes), totalBytes)
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
            statusLine = errorMessage ?: "Download failed. Tap Try again.",
        )
    }
}

/** "Two models": how many models the app downloads, in words. */
private val ModelCount: String = when (val count = ModelCatalog.ALL.size) {
    1 -> "One model"
    2 -> "Two models"
    3 -> "Three models"
    else -> "$count models"
}

/** What the models take together on the phone. */
private val ModelBytes: Long = ModelCatalog.ALL.sumOf { it.approxBytes }
