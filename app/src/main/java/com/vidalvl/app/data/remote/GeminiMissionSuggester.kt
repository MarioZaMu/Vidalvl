package com.vidalvl.app.data.remote

import android.util.Log
import com.vidalvl.app.BuildConfig
import com.vidalvl.app.domain.model.Attribute
import com.vidalvl.app.domain.suggestions.MissionSuggester
import com.vidalvl.app.domain.suggestions.SuggestionContext
import com.vidalvl.app.domain.suggestions.SuggestionResult
import com.vidalvl.app.domain.verification.FailureKind
import kotlinx.coroutines.CancellationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.IOException

private const val TAG = "GeminiSuggester"

/** Asks Gemini for mission ideas. Text only: no photos are involved, and the key is never logged. */
class GeminiMissionSuggester(
    private val api: GeminiApi,
    private val apiKey: String,
    private val model: String
) : MissionSuggester {

    override suspend fun suggest(
        context: SuggestionContext,
        dailyCount: Int,
        weeklyCount: Int,
        language: String
    ): SuggestionResult {
        if (apiKey.isBlank()) return SuggestionResult.Failed(FailureKind.NOT_CONFIGURED)
        return try {
            val prompt = GeminiMissionProtocol.buildPrompt(context, dailyCount, weeklyCount, language)
            val body = GeminiMissionProtocol.buildRequestJson(prompt)
                .toRequestBody("application/json".toMediaType())
            val answer = api.generateContent(model, apiKey, body).use { it.string() }
            if (BuildConfig.DEBUG) Log.d(TAG, "answer=$answer")
            val fallbackAttribute = context.goals.firstOrNull()?.attribute ?: Attribute.DISCIPLINE
            GeminiMissionProtocol.parseBatch(answer, fallbackAttribute)
                ?.let { SuggestionResult.Success(it) }
                ?: SuggestionResult.Failed(FailureKind.UNREADABLE_ANSWER)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            Log.w(TAG, "HTTP ${e.code()}")
            SuggestionResult.Failed(if (e.code() == 429) FailureKind.DAILY_LIMIT else FailureKind.SERVICE)
        } catch (e: IOException) {
            SuggestionResult.Failed(FailureKind.NETWORK)
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected error: ${e.javaClass.simpleName}")
            SuggestionResult.Failed(FailureKind.SERVICE)
        }
    }
}
