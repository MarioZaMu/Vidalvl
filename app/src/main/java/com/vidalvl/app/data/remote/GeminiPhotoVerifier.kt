package com.vidalvl.app.data.remote

import android.util.Base64
import android.util.Log
import com.vidalvl.app.BuildConfig
import com.vidalvl.app.domain.verification.FailureKind
import com.vidalvl.app.domain.verification.PhotoVerifier
import com.vidalvl.app.domain.verification.VerificationResult
import kotlinx.coroutines.CancellationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.IOException

private const val TAG = "GeminiVerifier"

/** Asks Gemini whether a photo shows a mission done. Never logs the key or the photo. */
class GeminiPhotoVerifier(
    private val api: GeminiApi,
    private val apiKey: String,
    private val model: String,
    private val language: () -> String
) : PhotoVerifier {

    override suspend fun verify(
        photoJpeg: ByteArray,
        missionTitle: String,
        missionDescription: String?
    ): VerificationResult {
        if (apiKey.isBlank()) return VerificationResult.Failed(FailureKind.NOT_CONFIGURED)
        return try {
            val prompt = GeminiProtocol.buildPrompt(missionTitle, missionDescription, language())
            val photo = Base64.encodeToString(photoJpeg, Base64.NO_WRAP)
            val body = GeminiProtocol.buildRequestJson(prompt, photo)
                .toRequestBody("application/json".toMediaType())
            val answer = api.generateContent(model, apiKey, body).use { it.string() }
            // Debug builds only: lets us see what the model saw. Never the photo or the key.
            if (BuildConfig.DEBUG) Log.d(TAG, "answer=$answer")
            GeminiProtocol.parseVerdict(answer)
        } catch (e: CancellationException) {
            throw e
        } catch (e: HttpException) {
            Log.w(TAG, "HTTP ${e.code()}")
            // 429 is quota: tell the user to wait. Anything else (bad key, unknown model, 5xx) is the service's side.
            VerificationResult.Failed(if (e.code() == 429) FailureKind.DAILY_LIMIT else FailureKind.SERVICE)
        } catch (e: IOException) {
            VerificationResult.Failed(FailureKind.NETWORK)
        } catch (e: Exception) {
            Log.w(TAG, "Unexpected error: ${e.javaClass.simpleName}")
            VerificationResult.Failed(FailureKind.SERVICE)
        }
    }
}
