package com.vidalvl.app

import com.vidalvl.app.domain.verification.DailyUsageStore
import com.vidalvl.app.domain.verification.FailureKind
import com.vidalvl.app.domain.verification.LimitedPhotoVerifier
import com.vidalvl.app.domain.verification.PhotoVerifier
import com.vidalvl.app.domain.verification.VerificationLimits
import com.vidalvl.app.domain.verification.VerificationResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class LimitedPhotoVerifierTest {
    private class FakeUsage : DailyUsageStore {
        val counts = mutableMapOf<Long, Int>()
        override fun usedOn(day: Long) = counts[day] ?: 0
        override fun recordUse(day: Long) {
            counts[day] = usedOn(day) + 1
        }
    }

    private class FakeVerifier(var answer: VerificationResult) : PhotoVerifier {
        var calls = 0
        override suspend fun verify(photoJpeg: ByteArray, missionTitle: String, missionDescription: String?): VerificationResult {
            calls++
            return answer
        }
    }

    private fun verify(verifier: PhotoVerifier) = runBlocking { verifier.verify(ByteArray(1), "Tender la cama", null) }

    @Test
    fun verdicts_use_up_a_check() {
        val usage = FakeUsage()
        val inner = FakeVerifier(VerificationResult.Rejected("No se ve la cama"))
        val limited = LimitedPhotoVerifier(inner, usage) { 10L }

        verify(limited)
        inner.answer = VerificationResult.Verified
        verify(limited)

        assertEquals(2, usage.usedOn(10L))
    }

    @Test
    fun errors_do_not_use_up_a_check() {
        val usage = FakeUsage()
        val inner = FakeVerifier(VerificationResult.Failed(FailureKind.NETWORK))
        val limited = LimitedPhotoVerifier(inner, usage) { 10L }

        repeat(10) { verify(limited) }

        assertEquals(0, usage.usedOn(10L))
        assertEquals(10, inner.calls)
    }

    @Test
    fun the_daily_cap_blocks_without_calling_the_service() {
        val usage = FakeUsage().apply { counts[10L] = VerificationLimits.MAX_PER_DAY }
        val inner = FakeVerifier(VerificationResult.Verified)
        val limited = LimitedPhotoVerifier(inner, usage) { 10L }

        val result = verify(limited)

        assertEquals(VerificationResult.Failed(FailureKind.DAILY_LIMIT), result)
        assertEquals(0, inner.calls)
    }

    @Test
    fun a_new_day_starts_with_a_fresh_count() {
        val usage = FakeUsage().apply { counts[10L] = VerificationLimits.MAX_PER_DAY }
        val inner = FakeVerifier(VerificationResult.Verified)
        val limited = LimitedPhotoVerifier(inner, usage) { 11L }

        assertEquals(VerificationResult.Verified, verify(limited))
    }
}
