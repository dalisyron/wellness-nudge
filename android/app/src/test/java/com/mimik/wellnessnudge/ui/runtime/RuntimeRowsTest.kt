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
                "smollm2-360m · 368 MB",
                "qwen3-1.7b · 1.7 GB",
                "12 nudges in on-device storage",
            ),
            rows.map { it.detail },
        )
        assertEquals(
            listOf(RowStatus.Running, RowStatus.Running, RowStatus.Running, RowStatus.Ready, RowStatus.Ready, RowStatus.Ready),
            rows.map { it.status },
        )
    }

    @Test
    fun partsThatDontAnswerShowAsUnavailable() {
        val info = PreviewData.runtimeInfo.copy(
            mimHealth = null,
            models = PreviewData.runtimeInfo.models.map { it.copy(ready = false) },
            nudgeCount = null,
        )
        val rows = info.toRows()

        assertEquals("/wellness-nudge/v1 · not responding", rows[1].detail)
        assertEquals(RowStatus.Unavailable, rows[1].status)
        assertEquals(RowStatus.Unavailable, rows[2].status)
        assertEquals("smollm2-360m · not downloaded", rows[3].detail)
        assertEquals(RowStatus.Unavailable, rows[3].status)
        assertEquals("Kept in on-device storage", rows[5].detail)
    }
}
