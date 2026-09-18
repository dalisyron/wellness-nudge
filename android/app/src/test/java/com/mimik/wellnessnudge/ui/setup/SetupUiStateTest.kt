package com.mimik.wellnessnudge.ui.setup

import com.mimik.wellnessnudge.bootstrap.BootstrapState
import com.mimik.wellnessnudge.bootstrap.ModelSetupItem
import com.mimik.wellnessnudge.ui.preview.PreviewData
import com.mimik.wellnessnudge.ui.setup.StepStatus.Active
import com.mimik.wellnessnudge.ui.setup.StepStatus.Done
import com.mimik.wellnessnudge.ui.setup.StepStatus.Failed
import com.mimik.wellnessnudge.ui.setup.StepStatus.Pending
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupUiStateTest {

    @Test
    fun eachPhaseActivatesItsChecklistRow() {
        val phases = BootstrapState.Phase.entries.filter { it != BootstrapState.Phase.READY }
        val active = phases.map { BootstrapState.Step(it).toSetupUiState().steps.indexOfFirst { step -> step.status == Active } }
        assertEquals(listOf(0, 1, 2, 3, 4, 4), active)

        val deploying = PreviewData.stepDeployMim.toSetupUiState()
        assertEquals(listOf(Done, Done, Done, Active, Pending), deploying.steps.map { it.status })
        assertEquals(SetupStatus.Working, deploying.status)
        assertTrue(deploying.models.isEmpty())
    }

    @Test
    fun aFailureMarksItsRowWithTheMessage() {
        val state = PreviewData.failedSignIn.toSetupUiState()

        assertEquals(listOf(Done, Failed, Pending, Pending, Pending), state.steps.map { it.status })
        assertEquals(PreviewData.failedSignIn.message, state.steps[1].message)
        assertEquals(SetupStatus.Failed, state.status)
    }

    @Test
    fun downloadsReportExactProgress() {
        val state = PreviewData.setupDownloading.toSetupUiState()
        val (writer, classifier) = state.models

        assertEquals(listOf(Done, Done, Done, Done, Active), state.steps.map { it.status })
        assertEquals("SmolLM2 360M · Q8_0 · 368 MB", writer.details)
        assertEquals("84 / 368 MB · 23%", writer.statusLine)
        assertEquals("84 of 368 MB", writer.progressDescription)
        assertEquals("Qwen3 1.7B · Q8_0 · 1.7 GB", classifier.details)
        assertEquals(ModelStatus.Waiting, classifier.status)
        assertEquals("Waiting in queue", classifier.statusLine)
    }

    @Test
    fun gigabyteDownloadsCountInGigabytesAndNeverShowDoneEarly() {
        val classifier = PreviewData.setupSecondModel.toSetupUiState().models[1]
        assertEquals("0.7 / 1.7 GB · 41%", classifier.statusLine)

        val item = PreviewData.setupDownloading.items[0]
        val complete = item.copy(downloadedBytes = item.totalBytes)
        assertEquals("368 / 368 MB · 99%", BootstrapState.Setup(listOf(complete)).toSetupUiState().models[0].statusLine)
    }

    @Test
    fun aDownloadWithoutASizeYetIsStarting() {
        val item = PreviewData.setupDownloading.items[0].copy(downloadedBytes = 0, totalBytes = 0)
        val card = BootstrapState.Setup(listOf(item)).toSetupUiState().models[0]

        assertEquals(ModelStatus.Starting, card.status)
        assertEquals("Starting download…", card.statusLine)
        assertNull(card.progressDescription)
    }

    @Test
    fun readyModelsCompleteTheChecklist() {
        val state = PreviewData.setupReady.toSetupUiState()

        assertTrue(state.steps.all { it.status == Done })
        assertEquals(SetupStatus.Ready, state.status)
        assertEquals(listOf("Ready · 368 MB on this phone", "Ready · 1.7 GB on this phone"), state.models.map { it.statusLine })
    }

    @Test
    fun aFailedDownloadPausesSetupUntilRetried() {
        val state = PreviewData.setupModelFailed.toSetupUiState()

        assertEquals(Failed, state.steps.last().status)
        assertNull(state.steps.last().message)
        assertEquals(SetupStatus.Paused, state.status)
        assertEquals(ModelStatus.Failed, state.models[1].status)
        assertEquals("Couldn't reach the download server. Check your internet and tap Retry.", state.models[1].statusLine)

        // While another model still downloads, setup is working rather than paused.
        val downloading = PreviewData.setupModelFailed.items[0].copy(state = ModelSetupItem.State.Downloading)
        val busy = BootstrapState.Setup(listOf(downloading, PreviewData.setupModelFailed.items[1])).toSetupUiState()
        assertEquals(Active, busy.steps.last().status)
        assertEquals(SetupStatus.Working, busy.status)
    }
}
