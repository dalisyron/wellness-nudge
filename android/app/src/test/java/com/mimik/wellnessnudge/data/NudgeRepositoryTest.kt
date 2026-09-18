package com.mimik.wellnessnudge.data

import com.mimik.wellnessnudge.api.NudgeApi
import com.mimik.wellnessnudge.api.NudgeRequest
import com.mimik.wellnessnudge.testing.FakeNudgeApi
import com.mimik.wellnessnudge.ui.preview.PreviewData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

class NudgeRepositoryTest {

    private var now = 1_000L

    // Fake calls run in the caller until they wait on a gate, so tests can interleave them.
    private fun repository(api: NudgeApi) = NudgeRepository(
        api = api,
        elapsedRealtime = { now },
        wallClock = { PreviewData.now },
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
        assertEquals(PreviewData.history.size + 1, repository.historyTotal.value)
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
    fun aRepeatedRequestWhileRunningIsIgnored() {
        val release = CompletableDeferred<Unit>()
        val api = FakeNudgeApi(onCreate = { release.await() })
        val repository = repository(api)

        repository.generate(PreviewData.request)
        val running = repository.generation.value
        repository.generate(PreviewData.request.copy())

        assertTrue(repository.generation.value === running)
        release.complete(Unit)
        assertTrue(repository.generation.value is GenerationState.Success)
        assertEquals(1, api.created.size)
    }

    /** mILM serves one inference at a time; a second call alongside another fails. */
    @Test
    fun aNewRequestWaitsForTheModelAndItsClockStartsWhenTheModelTakesIt() {
        val first = CompletableDeferred<Unit>()
        val second = NudgeRequest(sleepHours = 8.0, userGoal = "Keep it up")
        val api = FakeNudgeApi(onCreate = { if (it == PreviewData.request) first.await() else now += 3_000 })
        val repository = repository(api)

        repository.generate(PreviewData.request)
        repository.generate(second)

        val waiting = repository.generation.value as GenerationState.Running
        assertTrue(waiting.waiting)
        assertEquals(listOf(PreviewData.request), api.created)

        now += 20_000
        first.complete(Unit)

        val success = repository.generation.value as GenerationState.Success
        assertEquals(second, success.request)
        assertEquals(listOf(PreviewData.request, second), api.created)
        // Measured from when the model took it, not from when it was asked for.
        assertEquals(3_000L, success.latencyMs)
    }

    @Test
    fun aRequestReplacedWhileWaitingNeverReachesTheModel() {
        val first = CompletableDeferred<Unit>()
        val second = NudgeRequest(sleepHours = 7.0)
        val third = NudgeRequest(sleepHours = 8.0)
        val api = FakeNudgeApi(onCreate = { if (it == PreviewData.request) first.await() })
        val repository = repository(api)

        repository.generate(PreviewData.request)
        repository.generate(second)
        repository.generate(third)
        first.complete(Unit)

        assertEquals(listOf(PreviewData.request, third), api.created)
        assertEquals(third, (repository.generation.value as GenerationState.Success).request)
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

    /** Helpful, then Not really at once: the answer to Helpful mustn't flip the choice back. */
    @Test
    fun anOlderAnswerDoesntOverrideANewerChoice() = runBlocking {
        val gates = mapOf("yes" to CompletableDeferred<Unit>(), "no" to CompletableDeferred<Unit>())
        val repository = repository(FakeNudgeApi(onFeedback = { _, helpful -> gates.getValue(helpful).await() }))
        val id = PreviewData.latest.id
        repository.history()
        fun shown() = repository.historyFlow.value!!.first { it.id == id }.feedback

        val helpful = launch(start = CoroutineStart.UNDISPATCHED) { repository.setFeedback(id, Feedback.Helpful) }
        val notReally = launch(start = CoroutineStart.UNDISPATCHED) { repository.setFeedback(id, Feedback.NotHelpful) }
        assertEquals(Feedback.NotHelpful, shown())

        gates.getValue("yes").complete(Unit)
        helpful.join()
        assertEquals(Feedback.NotHelpful, shown())

        gates.getValue("no").complete(Unit)
        notReally.join()
        assertEquals(Feedback.NotHelpful, shown())
    }

    /** Both writes fail: the caches end where the mim is, not on the first choice. */
    @Test
    fun failuresRollBackToWhatTheMimConfirmed() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val repository = repository(
            FakeNudgeApi(onFeedback = { _, _ ->
                gate.await()
                throw IOException("offline")
            }),
        )
        val id = PreviewData.latest.id
        repository.history()
        fun shown() = repository.historyFlow.value!!.first { it.id == id }.feedback

        val writes = listOf(Feedback.Helpful, Feedback.NotHelpful).map { feedback ->
            async(start = CoroutineStart.UNDISPATCHED) { runCatching { repository.setFeedback(id, feedback) } }
        }
        gate.complete(Unit)
        writes.forEach { assertTrue(it.await().isFailure) }

        assertEquals(Feedback.Unset, shown())
    }

    @Test
    fun aHistoryLoadKeepsARatingOnItsWay() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val repository = repository(FakeNudgeApi(onFeedback = { _, _ -> gate.await() }))
        val id = PreviewData.latest.id
        repository.history()

        val write = launch(start = CoroutineStart.UNDISPATCHED) { repository.setFeedback(id, Feedback.Helpful) }
        repository.history()

        assertEquals(Feedback.Helpful, repository.historyFlow.value!!.first { it.id == id }.feedback)
        gate.complete(Unit)
        write.join()
    }

    @Test
    fun aHistoryLoadKeepsANudgeMadeWhileItWasOnItsWay() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        var holdHistory = false
        val repository = repository(FakeNudgeApi(onHistory = { if (holdHistory) gate.await() }))
        repository.history()

        holdHistory = true
        val load = launch(start = CoroutineStart.UNDISPATCHED) { repository.history() }
        repository.generate(PreviewData.request)
        val made = (repository.generation.value as GenerationState.Success).item
        gate.complete(Unit)
        load.join()

        assertEquals(made.id, repository.historyFlow.value!!.first().id)
        assertEquals(PreviewData.history.size + 1, repository.historyTotal.value)
    }

    @Test
    fun deleteRemovesTheNudgeButLeavesTheResultOnScreen() = runBlocking {
        val repository = repository(FakeNudgeApi())
        repository.history()
        repository.generate(PreviewData.request)
        val id = (repository.generation.value as GenerationState.Success).item.id

        repository.delete(id)

        assertTrue(repository.historyFlow.value!!.none { it.id == id })
        assertEquals(PreviewData.history.size, repository.historyTotal.value)
        assertNull(repository.nudge(id))
        // The Nudge screen keeps showing the result while it leaves.
        assertEquals(id, (repository.generation.value as GenerationState.Success).item.id)
    }

    @Test
    fun aDeletedNudgeLeavesForYou() = runBlocking {
        val repository = repository(FakeNudgeApi())
        repository.tips()
        val quoted = PreviewData.tips.items!!.first().helpfulNudges!!.first().id

        repository.delete(quoted)

        assertTrue(repository.tipsFlow.value!!.items!!.none { card -> card.helpfulNudges!!.any { it.id == quoted } })
    }

    /** For you quotes helpful nudges older than the newest 100 the history lists. */
    @Test
    fun anOlderHelpfulNudgeOpensFromForYou() = runBlocking {
        val repository = repository(FakeNudgeApi(history = PreviewData.history.take(2), total = 140))
        repository.tips()
        val quote = PreviewData.tips.items!!.first()

        val nudge = repository.nudge(quote.helpfulNudges!!.last().id)!!

        assertEquals(quote.category, nudge.category)
        assertEquals(Feedback.Helpful, nudge.feedback)
        assertEquals(140, repository.historyTotal.value)
        assertNull(nudge.metrics)
    }

    @Test
    fun theRuntimeCountsAsUpOnceItAnswers() = runBlocking {
        val repository = repository(FakeNudgeApi(onCreate = { throw ConnectException("refused") }))
        assertNull(repository.runtimeUp.value)

        repository.generate(PreviewData.request)
        assertFalse(repository.runtimeUp.value!!)

        repository.health()
        assertTrue(repository.runtimeUp.value!!)
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
