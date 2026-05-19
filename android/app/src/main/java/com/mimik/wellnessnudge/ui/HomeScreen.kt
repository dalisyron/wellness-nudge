package com.mimik.wellnessnudge.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import android.content.Context
import com.mimik.wellnessnudge.api.NudgeApi
import com.mimik.wellnessnudge.api.NudgeRequest
import com.mimik.wellnessnudge.demo.MockNudgeInputs
import com.mimik.wellnessnudge.ui.components.HintPanel
import com.mimik.wellnessnudge.ui.components.PrimaryActionButton
import com.mimik.wellnessnudge.ui.components.RuntimeStatus
import com.mimik.wellnessnudge.ui.components.SectionLabel
import com.mimik.wellnessnudge.ui.components.WellnessTopBar
import kotlinx.coroutines.launch

// Persisted under "wellness_demo" SharedPrefs. The pointer survives app
// restarts so the demo button keeps cycling through fresh entries even
// across process kills.
private const val KEY_MOCK_IDX = "mock_idx"

private data class MetricSliderSpec(
    val icon: ImageVector,
    val label: String,
    val unit: String,
    val min: Float,
    val max: Float,
    val sliderSteps: Int = 0,
    val format: (Float) -> String,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(api: NudgeApi, onGenerated: (id: String) -> Unit) {
    var sleepHours by remember { mutableFloatStateOf(6.5f) }
    var deepPct by remember { mutableFloatStateOf(15f) }
    var remPct by remember { mutableFloatStateOf(18f) }
    var restingHR by remember { mutableFloatStateOf(64f) }
    var hrvMs by remember { mutableFloatStateOf(45f) }
    var steps by remember { mutableFloatStateOf(7000f) }
    var userGoal by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val demoPrefs = remember {
        context.applicationContext.getSharedPreferences("wellness_demo", Context.MODE_PRIVATE)
    }
    // Indication-less clickable so taps on empty space dismiss the keyboard.
    // Children that handle their own taps (sliders, button, text field)
    // consume the gesture before it reaches us, so this only fires when
    // the user taps "nothing".
    val outsideTapSource = remember { MutableInteractionSource() }
    // Attached to the Generate button. When the goal field gets focus we
    // scroll the button into view; because the field sits directly above
    // it, both stay visible above the keyboard rather than just the field.
    val ctaIntoView = remember { BringIntoViewRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .clickable(
                interactionSource = outsideTapSource,
                indication = null,
                onClick = { focusManager.clearFocus() },
            )
            .verticalScroll(scrollState)
            .imePadding(),
    ) {
        WellnessTopBar(status = RuntimeStatus.Ready)

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionLabel(text = "Today's signals")

            MetricSliderRow(
                left = MetricSliderSpec(
                    icon = Icons.Outlined.Bed,
                    label = "Sleep",
                    unit = "h",
                    min = 0f, max = 12f,
                    format = { "%.1f".format(it) },
                ) to (sleepHours to { v: Float -> sleepHours = v }),
                right = MetricSliderSpec(
                    icon = Icons.Outlined.NightsStay,
                    label = "Deep sleep",
                    unit = "%",
                    min = 0f, max = 40f,
                    format = { it.toInt().toString() },
                ) to (deepPct to { v: Float -> deepPct = v }),
            )
            MetricSliderRow(
                left = MetricSliderSpec(
                    icon = Icons.Outlined.Psychology,
                    label = "REM",
                    unit = "%",
                    min = 0f, max = 40f,
                    format = { it.toInt().toString() },
                ) to (remPct to { v: Float -> remPct = v }),
                right = MetricSliderSpec(
                    icon = Icons.Outlined.MonitorHeart,
                    label = "Resting HR",
                    unit = "bpm",
                    min = 40f, max = 110f,
                    format = { it.toInt().toString() },
                ) to (restingHR to { v: Float -> restingHR = v }),
            )
            MetricSliderRow(
                left = MetricSliderSpec(
                    icon = Icons.Outlined.MonitorHeart,
                    label = "HRV",
                    unit = "ms",
                    min = 10f, max = 120f,
                    format = { it.toInt().toString() },
                ) to (hrvMs to { v: Float -> hrvMs = v }),
                right = MetricSliderSpec(
                    icon = Icons.Outlined.DirectionsWalk,
                    label = "Steps",
                    unit = "",
                    min = 0f, max = 25_000f,
                    format = { it.toInt().toString() },
                ) to (steps to { v: Float -> steps = v }),
            )

            TextButton(
                onClick = {
                    // Round-robin through the hidden mock seed list. The
                    // pointer is persisted across launches in SharedPreferences
                    // so successive taps — even across app restarts —
                    // surface a fresh entry until we've exhausted the list
                    // and wrap back to the start.
                    val all = MockNudgeInputs.ALL
                    val idx = (demoPrefs.getInt(KEY_MOCK_IDX, 0)).coerceIn(0, all.size - 1)
                    val sample = all[idx]
                    sleepHours = sample.sleepHours
                    deepPct = sample.deepSleepPct
                    remPct = sample.remSleepPct
                    restingHR = sample.restingHR
                    hrvMs = sample.hrvMs
                    steps = sample.stepsYesterday
                    userGoal = sample.userGoal
                    // Drop any stale error and unfocus the text field so the
                    // goal placeholder swaps to the new value cleanly.
                    error = null
                    focusManager.clearFocus()
                    demoPrefs.edit().putInt(KEY_MOCK_IDX, (idx + 1) % all.size).apply()
                },
                modifier = Modifier.padding(start = 2.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.RestartAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Load sample metrics and goal",
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(2.dp))
            SectionLabel(text = "Your focus")
            GoalField(
                value = userGoal,
                onValueChange = { userGoal = it },
                onDone = { focusManager.clearFocus() },
                onFocused = {
                    scope.launch {
                        // Compose's TextField re-fires its own bringIntoView
                        // for the cursor several times as the IME animates
                        // up, which makes any bringIntoView-based scroll
                        // fight the field. Sidestep that by explicitly
                        // scrolling to the end of the content once the IME
                        // has settled — the hint panel sits below the
                        // button, so maxValue keeps the field, the button,
                        // and a bit of the hint all visible above keyboard.
                        delay(450)
                        scrollState.animateScrollTo(scrollState.maxValue)
                    }
                },
            )

            Spacer(Modifier.height(12.dp))
            PrimaryActionButton(
                text = "Generate nudge",
                loading = loading,
                onClick = {
                    error = null
                    loading = true
                    scope.launch {
                        try {
                            val resp = api.createNudge(
                                NudgeRequest(
                                    sleepHours = sleepHours.toDouble(),
                                    deepSleepPct = deepPct.toDouble(),
                                    remSleepPct = remPct.toDouble(),
                                    restingHR = restingHR.toInt(),
                                    hrvMs = hrvMs.toInt(),
                                    stepsYesterday = steps.toInt(),
                                    userGoal = userGoal.ifBlank { null },
                                )
                            ).data
                            if (resp == null) error = "Empty response from local runtime"
                            else onGenerated(resp.id)
                        } catch (t: Throwable) {
                            error = t.message ?: "Generation failed"
                        } finally {
                            loading = false
                        }
                    }
                },
            )

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(Modifier.height(12.dp))
            HintPanel(
                icon = Icons.Outlined.AutoAwesome,
                text = if (loading) "Generating locally…"
                else "Enter today's signals to get one practical nudge.",
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun GoalField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    onFocused: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Flag,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        },
        placeholder = {
            Text(
                "What are you trying to improve today?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
        ),
        textStyle = MaterialTheme.typography.bodyLarge,
        // singleLine so the IME shows Done instead of Enter; capitalization
        // matches normal sentence input.
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = Modifier
            .fillMaxWidth()
            .onFocusEvent { if (it.isFocused) onFocused() },
    )
}

@Composable
private fun MetricSliderRow(
    left: Pair<MetricSliderSpec, Pair<Float, (Float) -> Unit>>,
    right: Pair<MetricSliderSpec, Pair<Float, (Float) -> Unit>>,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MetricSliderCard(spec = left.first, value = left.second.first, onValueChange = left.second.second, modifier = Modifier.weight(1f))
        MetricSliderCard(spec = right.first, value = right.second.first, onValueChange = right.second.second, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MetricSliderCard(
    spec: MetricSliderSpec,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = spec.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = spec.label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = spec.format(value),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (spec.unit.isNotEmpty()) {
                Spacer(Modifier.width(4.dp))
                Box(modifier = Modifier.padding(bottom = 4.dp)) {
                    Text(
                        text = spec.unit,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = spec.min..spec.max,
            steps = spec.sliderSteps,
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant,
            ),
        )
    }
}
