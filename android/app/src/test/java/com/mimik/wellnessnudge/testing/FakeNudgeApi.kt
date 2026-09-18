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
 * In-memory [NudgeApi] serving [PreviewData]. [onCreate] runs inside createNudge (to
 * advance a clock, or throw to simulate a failure); [failFeedback] makes feedback fail.
 */
class FakeNudgeApi(
    private val history: List<NudgeHistoryItem> = PreviewData.history,
    private val tips: TipsResponse = PreviewData.tips,
    private val onCreate: () -> Unit = {},
    private val failFeedback: Throwable? = null,
) : NudgeApi {

    override suspend fun health() = Envelope(HealthStatus(status = "ok", mim = "wellness-nudge", version = "1.0.0"))

    override suspend fun createNudge(req: NudgeRequest): Envelope<NudgeResponse> {
        onCreate()
        return Envelope(
            NudgeResponse(
                id = "nudge_${PreviewData.now + 60_000}",
                nudge = PreviewData.latest.nudge,
                category = PreviewData.latest.category,
                model = "smollm2-360m",
                finishReason = "stop",
                usage = null,
                metricsUsed = null,
            ),
        )
    }

    override suspend fun listHistory(limit: Int) =
        ListEnvelope(ListPayload(items = history.take(limit), total = history.size))

    override suspend fun updateFeedback(id: String, body: FeedbackRequest): Envelope<NudgeHistoryItem> {
        failFeedback?.let { throw it }
        return Envelope(history.first { it.id == id }.copy(helpful = body.helpful))
    }

    override suspend fun getTips() = Envelope(tips)

    override suspend fun deleteNudge(id: String): Response<Unit> = Response.success(Unit)
}
