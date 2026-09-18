package com.mimik.wellnessnudge.snapshots

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.R
import com.mimik.wellnessnudge.ui.components.CategoryBadge
import com.mimik.wellnessnudge.ui.components.CircleIconButton
import com.mimik.wellnessnudge.ui.components.EmptyState
import com.mimik.wellnessnudge.ui.components.FloatingNavBar
import com.mimik.wellnessnudge.ui.components.GradientButton
import com.mimik.wellnessnudge.ui.components.IconBadge
import com.mimik.wellnessnudge.ui.components.IndeterminateMeter
import com.mimik.wellnessnudge.ui.components.LinearMeter
import com.mimik.wellnessnudge.ui.components.MetricTile
import com.mimik.wellnessnudge.ui.components.NudgeOrb
import com.mimik.wellnessnudge.ui.components.OnDevicePill
import com.mimik.wellnessnudge.ui.components.OrbCanvas
import com.mimik.wellnessnudge.ui.components.OrbMode
import com.mimik.wellnessnudge.ui.components.RuntimeStatus
import com.mimik.wellnessnudge.ui.components.ScreenTitle
import com.mimik.wellnessnudge.ui.components.SecondaryButton
import com.mimik.wellnessnudge.ui.components.SectionHeader
import com.mimik.wellnessnudge.ui.components.SkeletonBlock
import com.mimik.wellnessnudge.ui.components.SleepDuration
import com.mimik.wellnessnudge.ui.components.SleepStagesBar
import com.mimik.wellnessnudge.ui.components.StatusDot
import com.mimik.wellnessnudge.ui.components.SuggestionChip
import com.mimik.wellnessnudge.ui.components.TextAction
import com.mimik.wellnessnudge.ui.components.WellnessBottomSheetFrame
import com.mimik.wellnessnudge.ui.components.WellnessCard
import com.mimik.wellnessnudge.ui.components.WellnessSlider
import com.mimik.wellnessnudge.ui.format.CategoryStyle
import com.mimik.wellnessnudge.ui.format.formatSleep
import com.mimik.wellnessnudge.ui.format.formatSteps
import com.mimik.wellnessnudge.ui.navigation.TopLevelTab
import com.mimik.wellnessnudge.ui.nudge.SignalChips
import com.mimik.wellnessnudge.ui.nudge.toSignals
import com.mimik.wellnessnudge.ui.preview.PreviewData
import com.mimik.wellnessnudge.ui.theme.Daybreak
import com.mimik.wellnessnudge.ui.theme.WellnessShapes
import com.mimik.wellnessnudge.ui.theme.WellnessSpacing
import com.mimik.wellnessnudge.ui.theme.WellnessTheme
import org.junit.Rule
import org.junit.Test

/** Every Daybreak component on device-sized pages, in the dark and light themes. */
@OptIn(ExperimentalLayoutApi::class)
class ComponentGalleryTest {

    @get:Rule
    val paparazzi = wellnessPaparazzi()

    @Test
    fun orb() = paparazzi.snapshotThemes("orb") {
        GalleryPage("NudgeOrb", "Still poses of each mode. The halo spills past the layout bounds.") {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center,
            ) {
                NudgeOrb(size = 160.dp, mode = OrbMode.Idle)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
            ) {
                LabeledOrb(120.dp, OrbMode.Thinking, "Thinking · 120")
                LabeledOrb(88.dp, OrbMode.Idle, "Idle · 88")
                LabeledOrb(72.dp, OrbMode.Still, "Still · 72")
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                LabeledOrb(44.dp, OrbMode.Idle, "Idle · 44")
                Spacer(Modifier.width(40.dp))
                Text("Good morning", style = MaterialTheme.typography.displayLarge)
                Spacer(Modifier.width(10.dp))
                NudgeOrb(size = 22.dp)
            }
            // Above an error: unlit, without its halo.
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                LabeledOrb(80.dp, OrbMode.Dimmed, "Dimmed · 80")
            }
        }
    }

    /** The idle orbit across its 12 s loop, and the thinking pulse, frame by frame. */
    @Test
    fun orbMotion() = paparazzi.snapshotThemes("orb_motion") {
        GalleryPage("Orb motion", "Idle frames 3.5 s apart across the sway cycle, then thinking frames across a breath.") {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 4,
            ) {
                for (frame in 0 until 12) {
                    val seconds = frame * 3.5f
                    OrbFrame("$seconds s", orbit = seconds, breath = 0f, energy = 0f)
                }
                for (step in 0 until 4) {
                    OrbFrame("breath ${step * 25}%", orbit = step * 0.75f, breath = 0.8f * step, energy = 1f)
                }
            }
        }
    }

    @Test
    fun actions() = paparazzi.snapshotThemes("actions") { ActionsPage() }

    /** Disabled and in-progress variants, which must stay legible and look intentional. */
    @Test
    fun states() = paparazzi.snapshotThemes("states") {
        GalleryPage("States", "Disabled controls and work in progress.") {
            Section("Gradient button") {
                GradientButton(text = "Generate nudge", onClick = {}, modifier = Modifier.fillMaxWidth(), enabled = false)
                GradientButton(
                    text = "Setting up…",
                    onClick = {},
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false,
                    loading = true,
                )
            }
            Section("Secondary and icon buttons") {
                Row(horizontalArrangement = Arrangement.spacedBy(WellnessSpacing.ItemGap), verticalAlignment = Alignment.CenterVertically) {
                    SecondaryButton(text = "Try another", onClick = {}, modifier = Modifier.weight(1f), icon = Icons.Rounded.Refresh, enabled = false)
                    CircleIconButton(Icons.Rounded.DeleteOutline, contentDescription = "Delete", onClick = {}, enabled = false)
                }
            }
            Section("Runtime") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OnDevicePill(status = RuntimeStatus.Starting, onClick = {})
                    OnDevicePill(status = RuntimeStatus.Error, onClick = {})
                }
                IndeterminateMeter(brush = WellnessTheme.colors.daybreak)
            }
        }
    }

    @Test
    fun chips() = paparazzi.snapshotThemes("chips") { ChipsPage() }

    @Test
    fun cards() = paparazzi.snapshotThemes("cards") {
        val colors = WellnessTheme.colors
        GalleryPage("Cards and metrics", "Surfaces, tiles and meters, as the Today screen composes them.") {
            Column {
                SectionHeader("Last night", action = { TextAction("Sample day", Icons.Rounded.Shuffle, onClick = {}) })
                Spacer(Modifier.height(WellnessSpacing.EyebrowGap))
                SleepCard()
            }
            Column {
                SectionHeader("Body")
                Spacer(Modifier.height(WellnessSpacing.EyebrowGap))
                Row(horizontalArrangement = Arrangement.spacedBy(WellnessSpacing.ItemGap)) {
                    MetricTile(
                        icon = Icons.Rounded.Favorite,
                        label = "Resting HR",
                        value = "64",
                        unit = "bpm",
                        color = colors.restingHr,
                        modifier = Modifier.weight(1f),
                        onClick = {},
                        meterProgress = (64f - 40f) / 70f,
                    )
                    MetricTile(
                        icon = Icons.Rounded.MonitorHeart,
                        label = "HRV",
                        value = "45",
                        unit = "ms",
                        color = colors.hrv,
                        modifier = Modifier.weight(1f),
                        onClick = {},
                        meterProgress = (45f - 10f) / 110f,
                    )
                }
                Spacer(Modifier.height(WellnessSpacing.ItemGap))
                MetricTile(
                    icon = Icons.AutoMirrored.Rounded.DirectionsWalk,
                    label = "Steps yesterday",
                    value = formatSteps(7000),
                    unit = null,
                    color = colors.steps,
                    onClick = {},
                    meterProgress = 0.7f,
                    meterCaption = "of 10k",
                    shape = WellnessShapes.Card,
                )
            }
        }
    }

    @Test
    fun surfaces() = paparazzi.snapshotThemes("surfaces") {
        val colors = WellnessTheme.colors
        GalleryPage("Surfaces and inputs", "The AI card, progress, editors and loading placeholders.") {
            Section("Nudge card") {
                WellnessCard(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.5.dp, colors.daybreakBorder),
                    glow = Daybreak.Orchid.copy(alpha = 0.18f),
                ) {
                    Text(PreviewData.latest.nudge, style = MaterialTheme.typography.headlineSmall)
                }
            }
            Section("Meters") {
                LinearMeter(progress = 0.23f, brush = colors.daybreak)
                LinearMeter(progress = 0.64f, brush = colors.daybreak)
                LinearMeter(progress = 1f, brush = colors.daybreak)
                IndeterminateMeter(brush = colors.daybreak)
            }
            Section("Sliders") {
                LabeledSlider("Total sleep", formatSleep(6.5f), 6.5f, 0f..12f, colors.sleep, steps = 47)
                LabeledSlider("HRV", "45 ms", 45f, 10f..120f, colors.hrv)
            }
            Section("Loading") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SkeletonBlock(Modifier.size(32.dp), CircleShape)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SkeletonBlock(
                            Modifier
                                .fillMaxWidth(0.4f)
                                .height(12.dp),
                        )
                        SkeletonBlock(
                            Modifier
                                .fillMaxWidth()
                                .height(12.dp),
                        )
                    }
                }
                SkeletonBlock(
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    WellnessShapes.Tile,
                )
            }
        }
    }

    @Test
    fun navigation() = paparazzi.snapshotThemes("navigation") {
        GalleryPage("Navigation and empty states", "The floating tab bar with each tab selected.") {
            Column {
                SectionHeader("Floating nav bar")
                Spacer(Modifier.height(WellnessSpacing.EyebrowGap))
                TopLevelTab.entries.indices.forEach { selected ->
                    FloatingNavBar(
                        items = TopLevelTab.entries.map { it.item },
                        selectedIndex = selected,
                        onSelect = {},
                        // The bar brings its own screen margins, so let it span the full width.
                        modifier = Modifier.bleed(WellnessSpacing.ScreenMargin),
                    )
                }
            }
            EmptyState(
                title = "Your journal is empty",
                body = "Every nudge you generate is saved here, on this phone.",
                actionText = "Create your first nudge",
                onAction = {},
                modifier = Modifier.padding(top = 20.dp),
            )
        }
    }

    /** The sheet as the editors and runtime sheet use it, drawn in place of its dialog window. */
    @Test
    fun sheet() = paparazzi.snapshotThemes("sheet") {
        val colors = WellnessTheme.colors
        Box(Modifier.fillMaxSize()) {
            GalleryPage("Sheets", "WellnessBottomSheet over a screen: 32 dp corners, surface color, quiet handle.") {}
            WellnessBottomSheetFrame {
                Column(
                    Modifier.padding(horizontal = WellnessSpacing.ScreenMargin),
                    verticalArrangement = Arrangement.spacedBy(WellnessSpacing.SectionGap),
                ) {
                    Text("Last night’s sleep", style = MaterialTheme.typography.titleLarge)
                    EditorRow("Total sleep", formatSleep(6.5f), 6.5f, 0f..12f, colors.sleep, steps = 47)
                    EditorRow("Deep sleep", "15%", 15f, 0f..40f, colors.deepSleep)
                    SecondaryButton(text = "Done", onClick = {}, modifier = Modifier.fillMaxWidth())
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    /** Stock Material overlays on the derived color scheme: an alert dialog and a snackbar. */
    @Test
    fun dialog() = paparazzi.snapshotThemes("dialog") {
        GalleryPage("Dialogs and snackbars", "Material components on the Daybreak color scheme.") {
            Section("Alert dialog") {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { DeleteDialogMock() }
            }
            Section("Snackbar") {
                // As SnackbarHost draws one: the action in the snackbar's own action color.
                Snackbar(
                    action = {
                        TextButton(
                            onClick = {},
                            colors = ButtonDefaults.textButtonColors(contentColor = SnackbarDefaults.actionColor),
                        ) { Text("Try again") }
                    },
                ) { Text("Couldn’t save your feedback.") }
            }
        }
    }

    @Test
    fun appIcon() = paparazzi.snapshotThemes("app_icon") {
        GalleryPage("App icon", "Adaptive icon under common launcher masks, the themed icon, and the splash.") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                AdaptiveIcon(112.dp, CircleShape)
                AdaptiveIcon(112.dp, RoundedCornerShape(30))
                AdaptiveIcon(112.dp, RoundedCornerShape(12))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ThemedIcon(88.dp, background = Color(0xFFE6E0FF), glyph = Color(0xFF2E2477))
                ThemedIcon(88.dp, background = Color(0xFF2E2A45), glyph = Color(0xFFD9D2FF))
                AdaptiveIcon(56.dp, CircleShape)
                AdaptiveIcon(40.dp, CircleShape)
            }
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AdaptiveIcon(200.dp, CircleShape)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ActionsPage() {
    GalleryPage("Actions", "One gradient call to action per screen; everything else stays quiet.") {
        Section("Gradient button") {
            GradientButton(text = "Generate nudge", onClick = {}, modifier = Modifier.fillMaxWidth())
            GradientButton(text = "Generate nudge", onClick = {}, modifier = Modifier.fillMaxWidth(), loading = true)
            GradientButton(
                text = "Start using Wellness Nudge",
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
                icon = null,
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
            )
        }
        Section("Secondary button") {
            Row(horizontalArrangement = Arrangement.spacedBy(WellnessSpacing.ItemGap)) {
                SecondaryButton(text = "Try another", onClick = {}, modifier = Modifier.weight(1f), icon = Icons.Rounded.Refresh)
                SecondaryButton(text = "Done", onClick = {}, modifier = Modifier.weight(1f))
            }
        }
        Section("Icon and text actions") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", onClick = {})
                Spacer(Modifier.width(12.dp))
                CircleIconButton(Icons.Rounded.DeleteOutline, contentDescription = "Delete", onClick = {})
                Spacer(Modifier.weight(1f))
                TextAction(text = "Sample day", icon = Icons.Rounded.Shuffle, onClick = {})
                TextAction(text = "Try again", icon = Icons.Rounded.Refresh, onClick = {})
            }
        }
        Section("On-device pill") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OnDevicePill(status = RuntimeStatus.Ready, onClick = {})
                OnDevicePill(status = RuntimeStatus.Starting, onClick = {})
                OnDevicePill(status = RuntimeStatus.Error, onClick = {})
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChipsPage() {
    val colors = WellnessTheme.colors
    GalleryPage("Chips and badges", "Choices, categories and the signals a nudge was grounded in.") {
        Section("Suggestion chips") {
            // Chips lay out 48 dp tall around a 36 dp pill, which spaces the rows.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Sleep better tonight", "Lower stress", "Feel less tired", "Recover from training", "Exercise more today")
                    .forEachIndexed { index, text -> SuggestionChip(text = text, selected = index == 0, onClick = {}) }
            }
        }
        Section("Filters") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Helpful", "Not helpful", "Unrated").forEachIndexed { index, text ->
                    SuggestionChip(text = text, selected = index == 0, onClick = {}, checkWhenSelected = true)
                }
            }
        }
        Section("Categories") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CategoryStyle.Slugs.forEach { CategoryBadge(category = it) }
            }
        }
        Section("Signals") {
            // As the Nudge screen lays them out: a dot per metric, a flag for the goal.
            SignalChips(PreviewData.request.toSignals())
        }
        Section("Icon badges and status") {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryStyle.Slugs.take(5).forEach { IconBadge(CategoryStyle.of(it).icon, CategoryStyle.of(it).color) }
                IconBadge(Icons.Rounded.Bedtime, colors.sleep, size = 40.dp)
                Spacer(Modifier.weight(1f))
                StatusDot(colors.success, pulsing = true)
                StatusDot(colors.warning)
                StatusDot(colors.danger)
            }
        }
    }
}

@Composable
private fun EditorRow(
    label: String,
    value: String,
    current: Float,
    range: ClosedFloatingPointRange<Float>,
    color: Color,
    steps: Int = 0,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.titleSmall, color = WellnessTheme.colors.textSecondary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(Icons.Rounded.Remove, contentDescription = "Less", onClick = {})
            Spacer(Modifier.width(16.dp))
            WellnessSlider(
                value = current,
                onValueChange = {},
                valueRange = range,
                color = color,
                modifier = Modifier.weight(1f),
                steps = steps,
                valueDescription = value,
            )
            Spacer(Modifier.width(16.dp))
            CircleIconButton(Icons.Rounded.Add, contentDescription = "More", onClick = {})
        }
    }
}

/**
 * Material's AlertDialog as it draws itself (its dialog window can't be captured): the same
 * shape, container and text colors and text buttons, read from the theme.
 */
@Composable
private fun DeleteDialogMock() {
    Surface(
        modifier = Modifier.widthIn(min = 280.dp, max = 340.dp),
        shape = AlertDialogDefaults.shape,
        color = AlertDialogDefaults.containerColor,
        tonalElevation = AlertDialogDefaults.TonalElevation,
    ) {
        Column(Modifier.padding(24.dp)) {
            Text(
                "Delete this nudge?",
                style = MaterialTheme.typography.headlineMedium,
                color = AlertDialogDefaults.titleContentColor,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "It will be removed from this phone.",
                style = MaterialTheme.typography.bodyMedium,
                color = AlertDialogDefaults.textContentColor,
            )
            Spacer(Modifier.height(24.dp))
            Row(Modifier.align(Alignment.End)) {
                TextButton(onClick = {}) { Text("Cancel") }
                TextButton(onClick = {}) { Text("Delete", color = WellnessTheme.colors.dangerText) }
            }
        }
    }
}

@Composable
internal fun GalleryPage(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = WellnessSpacing.ScreenMargin)
            .padding(top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(WellnessSpacing.SectionGap),
    ) {
        ScreenTitle(eyebrow = "Component gallery", title = title, subtitle = subtitle)
        content()
    }
}

@Composable
internal fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        SectionHeader(title)
        Spacer(Modifier.height(WellnessSpacing.EyebrowGap))
        Column(verticalArrangement = Arrangement.spacedBy(WellnessSpacing.ItemGap), content = content)
    }
}

@Composable
private fun LabeledOrb(size: Dp, mode: OrbMode, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        NudgeOrb(size = size, mode = mode)
        Spacer(Modifier.height(size / 4 + 16.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = WellnessTheme.colors.textSecondary)
    }
}

@Composable
private fun OrbFrame(label: String, orbit: Float, breath: Float, energy: Float) {
    Column(
        Modifier.width(96.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(96.dp), contentAlignment = Alignment.Center) {
            OrbCanvas(size = 64.dp, orbit = { orbit }, breath = { breath }, energy = { energy })
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = WellnessTheme.colors.textSecondary)
    }
}

@Composable
private fun SleepCard() {
    val colors = WellnessTheme.colors
    WellnessCard(modifier = Modifier.fillMaxWidth(), onClick = {}) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.Bedtime, colors.sleep)
            Spacer(Modifier.width(10.dp))
            Text("Sleep", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Icon(Icons.Rounded.Edit, contentDescription = null, tint = colors.textDisabled, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.height(16.dp))
        Row {
            SleepDuration(6.5f, Modifier.alignByBaseline())
            Spacer(Modifier.width(8.dp))
            Text(
                "asleep",
                style = WellnessTheme.type.metricUnit,
                color = colors.textSecondary,
                modifier = Modifier.alignByBaseline(),
            )
        }
        Spacer(Modifier.height(18.dp))
        SleepStagesBar(deepPct = 15f, remPct = 18f)
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: String,
    current: Float,
    range: ClosedFloatingPointRange<Float>,
    color: Color,
    steps: Int = 0,
) {
    Column {
        Row {
            Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.titleSmall, color = WellnessTheme.colors.textSecondary)
        }
        WellnessSlider(value = current, onValueChange = {}, valueRange = range, color = color, steps = steps)
    }
}

/** The adaptive icon layers (108 dp canvas) cropped to the 72 dp visible area under [mask]. */
@Composable
private fun AdaptiveIcon(size: Dp, mask: Shape) {
    Box(
        Modifier
            .size(size)
            .clip(mask),
        contentAlignment = Alignment.Center,
    ) {
        Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.requiredSize(size * 1.5f))
        Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.requiredSize(size * 1.5f))
    }
}

/** Android 13 themed icon: the monochrome layer tinted over a wallpaper-derived color. */
@Composable
private fun ThemedIcon(size: Dp, background: Color, glyph: Color) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painterResource(R.drawable.ic_launcher_monochrome),
            contentDescription = null,
            modifier = Modifier.requiredSize(size * 1.5f),
            colorFilter = ColorFilter.tint(glyph),
        )
    }
}

/** Widens the element by [horizontal] on each side, into the page margins. */
private fun Modifier.bleed(horizontal: Dp) = layout { measurable, constraints ->
    val extra = horizontal.roundToPx() * 2
    val placeable = measurable.measure(
        constraints.copy(minWidth = constraints.minWidth + extra, maxWidth = constraints.maxWidth + extra),
    )
    layout(placeable.width - extra, placeable.height) { placeable.place(-extra / 2, 0) }
}
