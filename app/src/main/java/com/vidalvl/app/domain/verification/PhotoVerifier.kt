package com.vidalvl.app.domain.verification

enum class FailureKind { NETWORK, SERVICE, UNREADABLE_ANSWER, DAILY_LIMIT, NOT_CONFIGURED }

sealed interface VerificationResult {
    /** The photo shows the mission was done. */
    data object Verified : VerificationResult

    /** The photo does not show it; [reason] is a short, kind explanation. Retrying is always allowed. */
    data class Rejected(val reason: String) : VerificationResult

    /** The check could not be completed (no network, service down...). Says nothing about the photo. */
    data class Failed(val kind: FailureKind) : VerificationResult
}

/** Decides whether a photo proves a mission. The photo is a JPEG and is never stored. */
interface PhotoVerifier {
    suspend fun verify(photoJpeg: ByteArray, missionTitle: String, missionDescription: String?): VerificationResult
}
