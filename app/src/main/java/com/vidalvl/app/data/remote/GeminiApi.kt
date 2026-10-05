package com.vidalvl.app.data.remote

import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

/**
 * Gemini generateContent endpoint. The body is built and read as raw JSON (see [GeminiProtocol]),
 * so no converter or reflection is needed. The key travels in a header, never in the URL.
 */
interface GeminiApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model", encoded = true) model: String,
        @Header("x-goog-api-key") apiKey: String,
        @Body body: RequestBody
    ): ResponseBody

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/"

        // No logging interceptor on purpose: it would print the key and the photo.
        fun create(): GeminiApi {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(45, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .build()
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .build()
                .create(GeminiApi::class.java)
        }
    }
}
