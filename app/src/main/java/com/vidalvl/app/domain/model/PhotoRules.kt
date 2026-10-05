package com.vidalvl.app.domain.model

/** Rules for the photos sent to verify a mission. */
object PhotoRules {
    /** Longest side, in pixels, of the photo that gets sent. Enough to see a made bed, small enough to be cheap and private. */
    const val MAX_SIDE_PX = 1024
    const val JPEG_QUALITY = 80

    /** Scales (width, height) down so the longest side is at most [maxSide], keeping the aspect ratio. Never scales up. */
    fun targetSize(width: Int, height: Int, maxSide: Int = MAX_SIDE_PX): Pair<Int, Int> {
        val longest = maxOf(width, height)
        if (longest <= maxSide) return width to height
        val factor = maxSide.toDouble() / longest
        return maxOf(1, (width * factor).toInt()) to maxOf(1, (height * factor).toInt())
    }
}
