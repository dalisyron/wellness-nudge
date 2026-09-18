package com.mimik.wellnessnudge.snapshots

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

/** In-memory [NudgeApi] serving [PreviewData], for tests that need a real NudgeRepository. */
class FakeNudgeApi(
    private val history: List<NudgeHistoryItem> = PreviewData.history,
    private val tips: TipsResponse = PreviewData.tips,
) : NudgeApi {

    override suspend fun health() = Envelope(HealthStatus(status = "ok", mim = "wellness-nudge", version = "1.0.0"))

    override suspend fun createNudge(req: NudgeRequest): Envelope<NudgeResponse> {
        val latest = PreviewData.latest
        return Envelope(
            NudgeResponse(
                id = latest.id,
                nudge = latest.nudge,
                category = latest.category,
                model = latest.model,
                finishReason = "stop",
                usage = null,
                metricsUsed = latest.metrics,
            ),
        )
    }

    override suspend fun listHistory(limit: Int) =
        ListEnvelope(ListPayload(items = history.take(limit), total = history.size))

    override suspend fun updateFeedback(id: String, body: FeedbackRequest): Envelope<NudgeHistoryItem> =
        Envelope(history.first { it.id == id }.copy(helpful = body.helpful))

    override suspend fun getTips() = Envelope(tips)

    override suspend fun deleteNudge(id: String): Response<Unit> = Response.success(Unit)
}
