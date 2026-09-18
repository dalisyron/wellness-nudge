package com.mimik.wellnessnudge.ui.nudge

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.ui.theme.WellnessMotion
import com.mimik.wellnessnudge.ui.theme.rememberAnimationsEnabled

/**
 * [text] whose words, when [reveal] is set, fade in and rise into place one after another:
 * 30 ms apart, 400 ms each, after [delayMillis]. It is laid out, and read by TalkBack, as one
 * text throughout; each word is drawn on its own only while it arrives, at the place the full
 * layout gives it. With animations off it appears whole. [onRevealed] follows a reveal.
 *
 * The words are laid out one by one only when the first is due, not on the busy frame the
 * screen appears in, and never again once the reveal is over.
 */
@Composable
internal fun WordRevealText(
    text: String,
    style: TextStyle,
    color: Color,
    reveal: Boolean,
    onRevealed: () -> Unit,
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
) {
    val animationsEnabled = rememberAnimationsEnabled()
    // Settled once per text: a reveal under way plays out even if the state stops asking for one.
    val playing = remember(text) { reveal && animationsEnabled }
    val words = remember(text) { wordRanges(text) }
    val duration = revealMillis(words.size)
    val elapsed = remember(text) { Animatable(if (playing) 0f else duration.toFloat()) }
    val latestOnRevealed by rememberUpdatedState(onRevealed)
    LaunchedEffect(text) {
        if (playing) elapsed.animateTo(duration.toFloat(), tween(duration, delayMillis, LinearEasing))
        if (reveal) latestOnRevealed()
    }

    val measurer = rememberTextMeasurer(cacheSize = 0)
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val done by remember(text) { derivedStateOf { elapsed.value >= duration } }
    val rise = with(LocalDensity.current) { WordRise.toPx() }
    Text(
        text = text,
        modifier = modifier.drawWithCache {
            // Read here, so the cache is rebuilt when the layout changes or the reveal ends.
            val full = layout
            val animating = playing && !done
            var pieces: List<WordPiece>? = null
            onDrawWithContent {
                val time = elapsed.value
                if (!animating || time >= duration) {
                    drawContent()
                } else if (time > 0f && full != null) {
                    val shownPieces = pieces ?: pieces(full, words, measurer, style).also { pieces = it }
                    for (piece in shownPieces) {
                        val progress = ((time - piece.order * WordStaggerMillis) / WordMillis).coerceIn(0f, 1f)
                        // Words arrive in reading order: once one hasn't started, none after it has.
                        if (progress == 0f) break
                        val shown = WellnessMotion.Easing.transform(progress)
                        val topLeft = Offset(piece.left, piece.top + rise * (1f - shown))
                        drawText(piece.layout, color, topLeft, alpha = shown)
                    }
                }
            }
        },
        color = color,
        style = style,
        onTextLayout = { layout = it },
    )
}

/** How long revealing [words] words takes, stagger included. */
internal fun revealMillis(words: Int): Int = if (words == 0) 0 else (words - 1) * WordStaggerMillis + WordMillis

/** The ranges of the words in [text]: runs of non-whitespace. */
internal fun wordRanges(text: String): List<IntRange> = WordPattern.findAll(text).map { it.range }.toList()

/** A word (or the part of one on a line, when it is too long for a line) laid out on its own. */
private class WordPiece(val layout: TextLayoutResult, val left: Float, val top: Float, val order: Int)

/** Lays out each word by itself, placed on the full layout's baseline where the full text has it. */
private fun pieces(
    full: TextLayoutResult,
    words: List<IntRange>,
    measurer: TextMeasurer,
    style: TextStyle,
): List<WordPiece> {
    val text = full.layoutInput.text
    val pieces = ArrayList<WordPiece>(words.size)
    words.forEachIndexed { order, word ->
        var start = word.first
        val end = word.last + 1
        while (start < end) {
            val line = full.getLineForOffset(start)
            val lineEnd = full.getLineEnd(line).coerceIn(start + 1, end)
            val layout = measurer.measure(text.subSequence(start, lineEnd), style, softWrap = false, maxLines = 1)
            pieces += WordPiece(
                layout = layout,
                left = full.getHorizontalPosition(start, usePrimaryDirection = true),
                top = full.getLineBaseline(line) - layout.firstBaseline,
                order = order,
            )
            start = lineEnd
        }
    }
    return pieces
}

private val WordPattern = Regex("\\S+")
private val WordRise = 6.dp
private const val WordStaggerMillis = 30
private const val WordMillis = 400
