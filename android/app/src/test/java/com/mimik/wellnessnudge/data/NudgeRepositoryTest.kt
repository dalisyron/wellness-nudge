package com.mimik.wellnessnudge.data

import com.mimik.wellnessnudge.api.NudgeApi
import com.mimik.wellnessnudge.testing.FakeNudgeApi
import com.mimik.wellnessnudge.ui.preview.PreviewData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class NudgeRepositoryTest {

    private var now = 1_000L

    private fun repository(api: NudgeApi) = NudgeRepository(
        api = api,
        elapsedRealtime = { now },
        wallClock = { PreviewData.now },
        // Fake calls complete immediately, so everything settles before generate() returns.
        dispatcher = Dispatchers.Unconfined,
    )

    @Test
    fun generationSucceedsWithMeasuredLatencyAndJoinsTheHistory() = runBlocking {
        val repository = repository(FakeNudgeApi(onCreate = { now += 4_213 }))
        repository.history()

        repository.generate(PreviewData.request)

        val success = repository.generation.value as GenerationState.Success
        assertEquals(4_213L, success.latencyMs)
        assertEquals(PreviewData.request, success.request)
        assertEquals(PreviewData.now + 60_000, success.item.ts)
        assertEquals(Feedback.Unset, success.item.feedback)
        assertEquals(PreviewData.request.userGoal, success.item.toRequest().userGoal)
        assertEquals(success.item, repository.historyFlow.value!!.first())
        assertEquals(4_213L, repository.nudge(success.item.id)!!.latencyMs)
    }

    @Test
    fun timeoutsFailWithFriendlyCopyAndKeepTheRequest() {
        val repository = repository(FakeNudgeApi(onCreate = { throw SocketTimeoutException("timeout") }))

        repository.generate(PreviewData.request)

        val failed = repository.generation.value as GenerationState.Failed
        assertEquals("The on-device model took too long to answer. Give it another try.", failed.message)
        assertEquals(PreviewData.request, failed.request)
    }

    @Test
    fun resetDropsAResultThatArrivesLater() {
        lateinit var repository: NudgeRepository
        repository = repository(FakeNudgeApi(onCreate = { repository.resetGeneration() }))

        repository.generate(PreviewData.request)

        assertEquals(GenerationState.Idle, repository.generation.value)
    }

    @Test
    fun feedbackUpdatesTheCaches() = runBlocking {
        val repository = repository(FakeNudgeApi())
        val id = PreviewData.latest.id
        repository.history()

        repository.setFeedback(id, Feedback.Helpful)

        assertEquals(Feedback.Helpful, repository.historyFlow.value!!.first { it.id == id }.feedback)
    }

    @Test
    fun failedFeedbackRollsBack() = runBlocking {
        val repository = repository(FakeNudgeApi(failFeedback = IOException("offline")))
        val id = PreviewData.latest.id
        repository.history()

        try {
            repository.setFeedback(id, Feedback.Helpful)
            fail("Expected the feedback call to fail")
        } catch (expected: IOException) {
            assertEquals(Feedback.Unset, repository.historyFlow.value!!.first { it.id == id }.feedback)
        }
    }

    @Test
    fun deleteRemovesTheNudgeButLeavesTheResultOnScreen() = runBlocking {
        val repository = repository(FakeNudgeApi())
        repository.history()
        repository.generate(PreviewData.request)
        val id = (repository.generation.value as GenerationState.Success).item.id

        repository.delete(id)

        assertTrue(repository.historyFlow.value!!.none { it.id == id })
        // The Nudge screen keeps showing the result while it leaves.
        assertEquals(id, (repository.generation.value as GenerationState.Success).item.id)
    }

    @Test
    fun aRepeatedRequestWhileRunningIsIgnored() {
        val release = CountDownLatch(1)
        val calls = AtomicInteger()
        val executor = Executors.newSingleThreadExecutor()
        val repository = NudgeRepository(
            api = FakeNudgeApi(onCreate = {
                calls.incrementAndGet()
                release.await()
            }),
            elapsedRealtime = { now },
            wallClock = { PreviewData.now },
            dispatcher = executor.asCoroutineDispatcher(),
        )
        try {
            repository.generate(PreviewData.request)
            val running = repository.generation.value

            repository.generate(PreviewData.request.copy())

            assertTrue(repository.generation.value === running)
            release.countDown()
            runBlocking { withTimeout(5_000) { repository.generation.first { it is GenerationState.Success } } }
            assertEquals(1, calls.get())
        } finally {
            release.countDown()
            repository.close()
            executor.shutdownNow()
        }
    }

    @Test
    fun savedNudgesRebuildTheirRequest() {
        val request = PreviewData.history[4].toRequest()

        assertEquals(7.0, request.sleepHours)
        assertEquals(60, request.restingHR)
        assertEquals(9500, request.stepsYesterday)
        assertEquals("Train consistently for my 10K", request.userGoal)
        assertNull(PreviewData.latest.copy(metrics = null, userGoal = "").toRequest().userGoal)
    }
}
