package com.mimik.wellnessnudge.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

interface NudgeApi {

    @POST("nudge")
    suspend fun createNudge(@Body req: NudgeRequest): Envelope<NudgeResponse>

    @GET("history")
    suspend fun listHistory(@Query("limit") limit: Int = 20): ListEnvelope<NudgeHistoryItem>

    @PUT("nudges/{id}/feedback")
    suspend fun updateFeedback(
        @Path("id") id: String,
        @Body body: FeedbackRequest,
    ): Envelope<NudgeHistoryItem>

    @GET("tips")
    suspend fun getTips(): Envelope<TipsResponse>

    @POST("nudges/{id}/clear")
    suspend fun deleteNudge(@Path("id") id: String): Response<Unit>

    companion object {
        fun create(baseUrl: String, apiKey: String): NudgeApi {
            val log = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val ok = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val req = chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $apiKey")
                        .addHeader("Accept", "application/json")
                        .build()
                    chain.proceed(req)
                }
                .addInterceptor(log)
                // Local loopback — but the model is slow on first token.
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build()
            return Retrofit.Builder()
                .baseUrl(baseUrl.trimEnd('/') + "/")
                .client(ok)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(NudgeApi::class.java)
        }
    }
}
