package com.mimik.wellnessnudge.ui.runtime

import com.mimik.wellnessnudge.ui.preview.PreviewData
import org.junit.Assert.assertEquals
import org.junit.Test

class RuntimeRowsTest {

    @Test
    fun rowsDescribeTheStackOnThePhone() {
        val rows = PreviewData.runtimeInfo.toRows()

        assertEquals(
            listOf("mimOE runtime", "wellness-nudge mim", "AI inference", "Nudge writer", "Goal classifier", "Your data"),
            rows.map { it.title },
        )
        assertEquals(
            listOf(
                "Embedded · port 8083",
                "/wellness-nudge/v1 · healthy",
                "mILM · OpenAI-compatible API",
                "smollm2-360m · 386 MB",
                "qwen3-1.7b · 1.8 GB",
                "12 nudges in on-device storage",
            ),
            rows.map { it.detail },
        )
        // Stored nudges are a count, not something to vouch for: no mark.
        assertEquals(
            listOf(RowStatus.Running, RowStatus.Running, RowStatus.Running, RowStatus.Ready, RowStatus.Ready, RowStatus.None),
            rows.map { it.status },
        )
        assertEquals(List(6) { null }, rows.map { it.problem })
    }

    @Test
    fun partsThatDontAnswerShowAsUnavailable() {
        val info = PreviewData.runtimeInfo.copy(
            mimHealth = null,
            models = PreviewData.runtimeInfo.models.map { it.copy(ready = false) },
            nudgeCount = null,
            runtimeReady = false,
        )
        val rows = info.toRows()

        assertEquals(RowStatus.Unavailable, rows[0].status)
        assertEquals("Not running", rows[0].problem)
        assertEquals("/wellness-nudge/v1", rows[1].detail)
        assertEquals("Not responding", rows[1].problem)
        assertEquals(RowStatus.Unavailable, rows[1].status)
        assertEquals(RowStatus.Unavailable, rows[2].status)
        assertEquals("smollm2-360m", rows[3].detail)
        assertEquals("Not downloaded", rows[3].problem)
        assertEquals(RowStatus.Unavailable, rows[3].status)
        assertEquals("Kept in on-device storage", rows[5].detail)
    }

    @Test
    fun theSummarySaysWhenNewNudgesMayFail() {
        val info = PreviewData.runtimeInfo
        val classifierOnly = info.copy(models = info.models.mapIndexed { index, model -> if (index == 1) model.copy(ready = false) else model })
        val mimAndClassifier = classifierOnly.copy(mimHealth = null)

        // Nudges don't need the goal classifier.
        assertEquals("1 part isn’t working.", brokenSummary(classifierOnly.toRows().filter { it.status == RowStatus.Unavailable }))
        assertEquals(
            "2 parts aren’t working. New nudges may fail until they’re back.",
            brokenSummary(mimAndClassifier.toRows().filter { it.status == RowStatus.Unavailable }),
        )
    }
}
