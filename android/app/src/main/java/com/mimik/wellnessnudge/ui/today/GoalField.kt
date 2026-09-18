package com.mimik.wellnessnudge.ui.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.ui.components.paperShadow
import com.mimik.wellnessnudge.ui.theme.WellnessMotion
import com.mimik.wellnessnudge.ui.theme.WellnessShapes
import com.mimik.wellnessnudge.ui.theme.WellnessTheme
import com.mimik.wellnessnudge.ui.theme.rememberAnimationsEnabled

/**
 * The goal input: one line on a raised field whose border turns accent while it has focus,
 * led by a quiet flag and ended by a clear button once there is text. At rest, a goal too
 * long for the line ends in an ellipsis. The keyboard's Done key clears focus.
 */
@Composable
internal fun GoalField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    onFocusChange: (Boolean) -> Unit = {},
) {
    val colors = WellnessTheme.colors
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    val borderTarget = if (focused) colors.accent else colors.controlBorder
    val border = if (rememberAnimationsEnabled()) {
        animateColorAsState(
            targetValue = borderTarget,
            animationSpec = tween(WellnessMotion.SmallMillis, easing = WellnessMotion.Easing),
            label = "goalBorder",
        ).value
    } else {
        borderTarget
    }
    // The field's own copy keeps the cursor and the keyboard's composing text. The line scrolls
    // to its cursor: outside focus the cursor rests at the start, under the resting text below;
    // focus moves it to the end, ready to add to (a tap then moves it where it lands).
    fun cursorPlaced(text: String) = TextFieldValue(text, if (focused) TextRange(text.length) else TextRange.Zero)
    var edited by remember { mutableStateOf(TextFieldValue(value)) }
    // A goal set from outside: a suggestion, a sample day, clearing.
    val fieldValue = if (edited.text == value) edited else cursorPlaced(value)
    val textStyle = MaterialTheme.typography.bodyLarge
    BasicTextField(
        value = fieldValue,
        onValueChange = {
            edited = it
            if (it.text != value) onValueChange(it.text)
        },
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged {
                if (focused != it.isFocused) {
                    focused = it.isFocused
                    edited = cursorPlaced(value)
                    onFocusChange(it.isFocused)
                }
            },
        textStyle = textStyle.copy(color = colors.textPrimary),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        singleLine = true,
        cursorBrush = SolidColor(colors.accent),
        decorationBox = { innerTextField ->
            Row(
                Modifier
                    .heightIn(min = FieldHeight)
                    .paperShadow(colors, WellnessShapes.Input, elevation = 3.dp)
                    .clip(WellnessShapes.Input)
                    .background(colors.controlFill)
                    .border(1.dp, border, WellnessShapes.Input)
                    .padding(start = 18.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Flag,
                    contentDescription = null,
                    tint = colors.textDisabled,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .weight(1f)
                        .padding(vertical = 16.dp),
                ) {
                    val resting = !focused && value.isNotEmpty()
                    when {
                        value.isEmpty() -> Text(
                            text = "What would help today?",
                            style = textStyle,
                            color = colors.textTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        // The field would cut a long goal off mid-word. It stays underneath,
                        // invisible, to take the tap.
                        resting -> Text(
                            text = value,
                            modifier = Modifier.clearAndSetSemantics {},
                            style = textStyle,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Box(Modifier.alpha(if (resting) 0f else 1f)) { innerTextField() }
                }
                ClearButton(visible = value.isNotEmpty(), onClick = { onValueChange("") })
            }
        },
    )
}

/** Clears the goal. Its room is kept while hidden, so the text never reflows. */
@Composable
private fun ClearButton(visible: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(ClearButtonSize), contentAlignment = Alignment.Center) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(WellnessMotion.SmallMillis)) + scaleIn(initialScale = 0.6f),
            exit = fadeOut(tween(WellnessMotion.SmallMillis)) + scaleOut(targetScale = 0.6f),
        ) {
            Box(
                Modifier
                    .size(ClearButtonSize)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Clear goal",
                    tint = WellnessTheme.colors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

private val FieldHeight = 56.dp
private val ClearButtonSize = 48.dp
