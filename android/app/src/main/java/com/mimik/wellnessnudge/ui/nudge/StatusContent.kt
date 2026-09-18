package com.mimik.wellnessnudge.ui.nudge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mimik.wellnessnudge.ui.components.OrbMode
import com.mimik.wellnessnudge.ui.components.SecondaryButton
import com.mimik.wellnessnudge.ui.components.SkeletonBlock
import com.mimik.wellnessnudge.ui.components.WellnessCard
import com.mimik.wellnessnudge.ui.theme.WellnessShapes
import com.mimik.wellnessnudge.ui.theme.WellnessSpacing
import com.mimik.wellnessnudge.ui.theme.WellnessTheme

/**
 * Something went wrong: the orb, gone still, over a title and a friendly explanation, with
 * Back and, when there is one, a way to [retry] (it gets the modifier that sizes it).
 */
@Composable
internal fun FailedContent(
    title: String,
    body: String,
    orb: OrbSlot,
    onBack: () -> Unit,
    retry: (@Composable (Modifier) -> Unit)?,
) {
    val colors = WellnessTheme.colors
    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                // Weighted to the bottom, so the group sits a little above center, where the eye expects it.
                .padding(start = 32.dp, end = 32.dp, top = 24.dp, bottom = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            orb(FailedOrbSize, OrbMode.Still, Modifier)
            // Clear of the halo, which spills a quarter of the orb's size.
            Spacer(Modifier.height(36.dp))
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineLarge.copy(lineBreak = LineBreak.Heading),
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge.copy(lineBreak = LineBreak.Heading),
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
        StageFooter {
            val back: @Composable (Modifier) -> Unit = { backModifier ->
                SecondaryButton(text = "Back", onClick = onBack, modifier = backModifier)
            }
            if (retry == null) {
                back(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = FooterButtonHeight),
                )
            } else {
                ButtonPair(
                    first = { back(Modifier.heightIn(min = FooterButtonHeight)) },
                    second = { retry(Modifier.heightIn(min = FooterButtonHeight)) },
                )
            }
        }
    }
}

/** A saved nudge on its way: placeholders in the shape of the result. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LoadingContent() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = WellnessSpacing.ScreenMargin)
            .padding(top = 20.dp)
            .clearAndSetSemantics { contentDescription = "Loading your nudge" },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SkeletonBlock(Modifier.size(ResultOrbSize), CircleShape)
            Spacer(Modifier.width(14.dp))
            Column {
                SkeletonBlock(Modifier.size(width = 132.dp, height = 11.dp))
                Spacer(Modifier.height(11.dp))
                SkeletonBlock(Modifier.size(width = 76.dp, height = 24.dp), WellnessShapes.Pill)
            }
        }
        Spacer(Modifier.height(20.dp))
        WellnessCard(Modifier.fillMaxWidth()) {
            // Lines on the nudge's own 35 sp rhythm.
            listOf(1f, 1f, 1f, 0.92f, 0.46f).forEachIndexed { index, width ->
                if (index > 0) Spacer(Modifier.height(15.dp))
                SkeletonBlock(
                    Modifier
                        .fillMaxWidth(width)
                        .height(20.dp),
                    WellnessShapes.Pill,
                )
            }
        }
        Spacer(Modifier.height(WellnessSpacing.SectionGap))
        SkeletonBlock(Modifier.size(width = 92.dp, height = 11.dp))
        Spacer(Modifier.height(WellnessSpacing.EyebrowGap))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(104, 84, 80, 112, 92, 98, 188).forEach { width ->
                SkeletonBlock(Modifier.size(width = width.dp, height = 32.dp), WellnessShapes.Pill)
            }
        }
    }
}

private val FailedOrbSize = 80.dp
