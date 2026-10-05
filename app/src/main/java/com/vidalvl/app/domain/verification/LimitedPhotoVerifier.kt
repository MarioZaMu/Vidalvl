package com.vidalvl.app.domain.verification

/** Remembers how many checks were made on each day. */
interface DailyUsageStore {
    fun usedOn(day: Long): Int
    fun recordUse(day: Long)
}

object VerificationLimits {
    /** Checks per day. Keeps the free quota from running out by accident. */
    const val MAX_PER_DAY = 5

    /** Failed attempts (rejected or errors) after which the user may complete the mission without a check. */
    const val ATTEMPTS_BEFORE_SKIP = 2
}

/**
 * Applies the daily cap around another verifier. Only real verdicts (verified or rejected) use up a check,
 * so network errors never cost the user an attempt.
 */
class LimitedPhotoVerifier(
    private val inner: PhotoVerifier,
    private val usage: DailyUsageStore,
    private val today: () -> Long
) : PhotoVerifier {
    override suspend fun verify(
        photoJpeg: ByteArray,
        missionTitle: String,
        missionDescription: String?
    ): VerificationResult {
        val day = today()
        if (usage.usedOn(day) >= VerificationLimits.MAX_PER_DAY) {
            return VerificationResult.Failed(FailureKind.DAILY_LIMIT)
        }
        val result = inner.verify(photoJpeg, missionTitle, missionDescription)
        if (result is VerificationResult.Verified || result is VerificationResult.Rejected) {
            usage.recordUse(day)
        }
        return result
    }
}
