package com.mimik.wellnessnudge.testing

import com.mimik.wellnessnudge.api.Envelope
import com.mimik.wellnessnudge.api.FeedbackRequest
import com.mimik.wellnessnudge.api.HealthStatus
import com.mimik.wellnessnudge.api.ListEnvelope
import com.mimik.wellnessnudge.api.ListPayload
import com.mimik.wellnessnudge.api.NudgeApi
import com.mimik.wellnessnudge.api.NudgeHistoryItem
import com.mimik.wellnessnudge.api.NudgeRequest
import com.mimik.wellnessnudge.api.NudgeResponse
import com.mimik.wellnessnudge.api.TipsResponse
import com.mimik.wellnessnudge.ui.preview.PreviewData
import retrofit2.Response

/**
 * In-memory [NudgeApi] serving [PreviewData]. The hooks run inside the calls, so a test can
 * advance a clock, hold a call open (await something) or fail it (throw): [onCreate] for
 * each createNudge, [onFeedback] for each feedback write, [onHistory] for each history load.
 * [failFeedback] makes every feedback write fail. [created] records the requests that
 * reached the model. [total] is what the history reports as stored in all.
 */
class FakeNudgeApi(
    private val history: List<NudgeHistoryItem> = PreviewData.history,
    private val tips: TipsResponse = PreviewData.tips,
    private val onCreate: suspend (NudgeRequest) -> Unit = {},
    private val failFeedback: Throwable? = null,
    private val onFeedback: suspend (id: String, helpful: String) -> Unit = { _, _ -> },
    private val onHistory: suspend () -> Unit = {},
    private val total: Int = history.size,
) : NudgeApi {

    val created = mutableListOf<NudgeRequest>()

    override suspend fun health() = Envelope(HealthStatus(status = "ok", mim = "wellness-nudge", version = "1.0.0"))

    override suspend fun createNudge(req: NudgeRequest): Envelope<NudgeResponse> {
        created += req
        onCreate(req)
        return Envelope(
            NudgeResponse(
                id = "nudge_${PreviewData.now + 60_000 * created.size}",
                nudge = PreviewData.latest.nudge,
                category = PreviewData.latest.category,
                model = "smollm2-360m",
                finishReason = "stop",
                usage = null,
                metricsUsed = null,
            ),
        )
    }

    override suspend fun listHistory(limit: Int): ListEnvelope<NudgeHistoryItem> {
        onHistory()
        return ListEnvelope(ListPayload(items = history.take(limit), total = total))
    }

    override suspend fun updateFeedback(id: String, body: FeedbackRequest): Envelope<NudgeHistoryItem> {
        onFeedback(id, body.helpful)
        failFeedback?.let { throw it }
        return Envelope(history.first { it.id == id }.copy(helpful = body.helpful))
    }

    override suspend fun getTips() = Envelope(tips)

    override suspend fun deleteNudge(id: String): Response<Unit> = Response.success(Unit)
}
