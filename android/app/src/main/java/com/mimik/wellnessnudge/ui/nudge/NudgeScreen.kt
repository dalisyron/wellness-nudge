package com.mimik.wellnessnudge.ui.nudge

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.mimik.wellnessnudge.api.NudgeRequest
import com.mimik.wellnessnudge.data.Feedback
import com.mimik.wellnessnudge.ui.components.CircleIconButton
import com.mimik.wellnessnudge.ui.components.DaybreakBackground
import com.mimik.wellnessnudge.ui.components.GradientButton
import com.mimik.wellnessnudge.ui.components.NudgeOrb
import com.mimik.wellnessnudge.ui.components.OrbMode
import com.mimik.wellnessnudge.ui.components.SecondaryButton
import com.mimik.wellnessnudge.ui.theme.WellnessMotion
import com.mimik.wellnessnudge.ui.theme.WellnessSpacing
import com.mimik.wellnessnudge.ui.theme.WellnessTheme

/**
 * The nudge, full screen (`nudge/new` and `nudge/saved/{id}`): the model thinking on the
 * phone, the nudge it wrote, revealed word by word when fresh, and the user's verdict. One orb
 * carries through the stages: it thinks at the center, then settles beside the result or dims
 * above an error. Stateless: [NudgeRoute] wires it to [NudgeViewModel].
 *
 * Messages (a rating or a delete that failed) show in a snackbar above the footer on
 * whichever stage is up, so one never waits to pop up over the next nudge.
 */
@Composable
fun NudgeScreen(
    state: NudgeUiState,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onTryAnother: (NudgeRequest) -> Unit,
    onRetryLoad: () -> Unit,
    onFeedback: (Feedback) -> Unit,
    onDeleteRequest: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onDeleteDismiss: () -> Unit,
    onRevealed: (nudgeId: String) -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val latestOnMessageShown by rememberUpdatedState(onMessageShown)
    state.message?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            latestOnMessageShown()
        }
    }
    DaybreakBackground(modifier) {
        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
        ) {
            // First to be read, drawn over the stage so the orb's halo passes under the buttons.
            TopBar(
                showDelete = state.content is NudgeContent.Result,
                deleteEnabled = !state.deleting,
                onBack = onBack,
                onDelete = onDeleteRequest,
                modifier = Modifier.zIndex(1f),
            )
            NudgeStage(
                content = state.content,
                deleting = state.deleting,
                onBack = onBack,
                onDone = onDone,
                onTryAnother = onTryAnother,
                onRetryLoad = onRetryLoad,
                onFeedback = onFeedback,
                onRevealed = onRevealed,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(top = TopBarGap + TopBarButtonSize),
            )
            // Above the footer, lined up with the screen margin (Material's snackbar pads itself by 12 dp).
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(2f)
                    .navigationBarsPadding()
                    .padding(bottom = FooterClearance)
                    .padding(horizontal = WellnessSpacing.ScreenMargin - 12.dp),
            )
        }
    }
    if (state.confirmingDelete) {
        DeleteNudgeDialog(onConfirm = onDeleteConfirm, onDismiss = onDeleteDismiss)
    }
}

/** An orb placement in a stage: the orb is drawn at [size] in [mode] wherever a stage calls it. */
internal typealias OrbSlot = @Composable (size: Dp, mode: OrbMode, modifier: Modifier) -> Unit

/**
 * The stages, crossfading. The orb is one movable instance that the showing stage holds, so
 * its orbit carries on through a change of stage; while the stages trade places it flies, as a
 * shared element, from the old stage's slot to the new one's and changes mode on the way.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun NudgeStage(
    content: NudgeContent,
    deleting: Boolean,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onTryAnother: (NudgeRequest) -> Unit,
    onRetryLoad: () -> Unit,
    onFeedback: (Feedback) -> Unit,
    onRevealed: (nudgeId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val orb = remember {
        movableContentOf { orbModifier: Modifier, size: Dp, mode: OrbMode -> NudgeOrb(size, orbModifier, mode) }
    }
    val showingKey = content.stageKey
    SharedTransitionLayout(modifier) {
        AnimatedContent(
            targetState = content,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = { stageTransition() },
            contentKey = { it.stageKey },
            label = "nudgeStage",
        ) { stage ->
            // A stage on its way out keeps a stand-in of the same size for the orb to fly from.
            val holdsOrb = stage.stageKey == showingKey
            val orbSlot: OrbSlot = { size, mode, slotModifier ->
                val shared = slotModifier.sharedElement(
                    state = rememberSharedContentState(OrbKey),
                    animatedVisibilityScope = this@AnimatedContent,
                    boundsTransform = OrbFlight,
                )
                if (holdsOrb) orb(shared, size, mode) else Spacer(shared.size(size))
            }
            when (stage) {
                NudgeContent.Loading -> LoadingContent()
                is NudgeContent.Generating -> GeneratingContent(stage, orbSlot)
                is NudgeContent.Result -> ResultContent(
                    result = stage,
                    orb = orbSlot,
                    entrance = this,
                    deleting = deleting,
                    onFeedback = onFeedback,
                    onTryAnother = onTryAnother,
                    onDone = onDone,
                    onRevealed = onRevealed,
                )
                is NudgeContent.GenerationFailed -> FailedContent(
                    title = "Couldn’t finish that nudge",
                    body = stage.message,
                    orb = orbSlot,
                    onBack = onBack,
                    retry = { retryModifier ->
                        GradientButton(
                            text = "Try again",
                            onClick = { onTryAnother(stage.request) },
                            modifier = retryModifier,
                            icon = Icons.Rounded.Refresh,
                        )
                    },
                )
                NudgeContent.LoadFailed -> FailedContent(
                    title = "Couldn’t open that nudge",
                    body = "The on-device service didn’t answer. Give it another try.",
                    orb = orbSlot,
                    onBack = onBack,
                    retry = { retryModifier ->
                        SecondaryButton(
                            text = "Try again",
                            onClick = onRetryLoad,
                            modifier = retryModifier,
                            icon = Icons.Rounded.Refresh,
                        )
                    },
                )
                // Only a delete makes a nudge the caches and the mim can't find; say what is known.
                NudgeContent.Missing -> FailedContent(
                    title = "Couldn’t find that nudge",
                    body = "It may have been deleted from this phone.",
                    orb = orbSlot,
                    onBack = onBack,
                    retry = null,
                )
            }
        }
    }
}

/**
 * The old stage fades out, then the new one fades in, so two layouts never show at once while
 * the orb flies between them. A result brings its own entrance (see [entrance]): its sections
 * rise in one after another behind the landing orb.
 */
private fun AnimatedContentTransitionScope<NudgeContent>.stageTransition(): ContentTransform {
    val enter = if (targetState is NudgeContent.Result) {
        EnterTransition.None
    } else {
        fadeIn(tween(StageFadeMillis, delayMillis = WellnessMotion.SmallMillis, easing = WellnessMotion.Easing))
    }
    val exit = fadeOut(tween(WellnessMotion.SmallMillis, easing = WellnessMotion.Easing))
    // No size animation or clip: the orb's halo spills past the stage.
    return (enter togetherWith exit).using(null)
}

/** Stages that change in place keep their key: the ticking generation, a rating, a reload. */
private val NudgeContent.stageKey: Any
    get() = when (this) {
        NudgeContent.Loading -> "loading"
        is NudgeContent.Generating -> "generating"
        is NudgeContent.Result -> nudge.id
        is NudgeContent.GenerationFailed, NudgeContent.LoadFailed, NudgeContent.Missing -> "failed"
    }

/**
 * Fades in and rises into place as the stage appears, [order] steps after the first section,
 * so a result settles top to bottom.
 */
@Composable
internal fun AnimatedVisibilityScope.entrance(order: Int, rise: Dp = SectionRise): Modifier {
    val risePx = with(LocalDensity.current) { rise.roundToPx() }
    val delay = EntranceDelayMillis + order * EntranceStaggerMillis
    return Modifier.animateEnterExit(
        enter = fadeIn(tween(EntranceMillis, delay, WellnessMotion.Easing)) +
            slideInVertically(tween(EntranceMillis + 150, delay, WellnessMotion.Easing)) { risePx },
        exit = ExitTransition.None,
        label = "entrance",
    )
}

@Composable
private fun TopBar(
    showDelete: Boolean,
    deleteEnabled: Boolean,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = WellnessSpacing.ScreenMargin)
            .padding(top = TopBarGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", onClick = onBack)
        Spacer(Modifier.weight(1f))
        AnimatedVisibility(
            visible = showDelete,
            enter = fadeIn(tween(WellnessMotion.ScreenMillis, easing = WellnessMotion.Easing)),
            exit = fadeOut(tween(WellnessMotion.SmallMillis, easing = WellnessMotion.Easing)),
        ) {
            CircleIconButton(Icons.Rounded.DeleteOutline, contentDescription = "Delete nudge", onClick = onDelete, enabled = deleteEnabled)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteNudgeDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    BasicAlertDialog(onDismissRequest = onDismiss) {
        DeleteNudgeDialogContent(onConfirm = onConfirm, onDismiss = onDismiss)
    }
}

/**
 * The delete confirmation, drawn as Material's AlertDialog draws itself. [DeleteNudgeDialog]
 * shows it in a dialog window; snapshot tests, which can't capture one, draw it in place.
 */
@Composable
internal fun DeleteNudgeDialogContent(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = WellnessTheme.colors
    Surface(
        modifier = modifier,
        shape = AlertDialogDefaults.shape,
        color = AlertDialogDefaults.containerColor,
        tonalElevation = AlertDialogDefaults.TonalElevation,
        // Ink has no shadows: a hairline sets the dialog off the dimmed screen, as on other floating surfaces.
        border = if (colors.isDark) BorderStroke(1.dp, colors.hairlineStrong) else null,
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(
                text = "Delete this nudge?",
                modifier = Modifier.semantics { heading() },
                // Material's dialog title size; headlineSmall is the larger nudge hero.
                style = MaterialTheme.typography.headlineMedium,
                color = AlertDialogDefaults.titleContentColor,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "It will be removed from this phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = AlertDialogDefaults.textContentColor,
            )
            Spacer(Modifier.height(24.dp))
            Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                TextButton(onClick = onConfirm) { Text("Delete", color = colors.dangerText) }
            }
        }
    }
}

private const val OrbKey = "nudgeOrb"

/** The orb's flight between stages: calm and eased, the length of a reveal. */
@OptIn(ExperimentalSharedTransitionApi::class)
private val OrbFlight = BoundsTransform { _, _ -> tween(WellnessMotion.RevealMillis, easing = WellnessMotion.Easing) }

private val TopBarGap = 8.dp
private val TopBarButtonSize = 44.dp

/** A stage's footer: its 60 dp buttons and their 12 dp margins. */
private val FooterClearance = FooterButtonHeight + 24.dp
private val SectionRise = 16.dp

private const val StageFadeMillis = 300
private const val EntranceMillis = 400
private const val EntranceDelayMillis = 150
private const val EntranceStaggerMillis = 70
