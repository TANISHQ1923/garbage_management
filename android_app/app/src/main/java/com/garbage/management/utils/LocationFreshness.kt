package com.garbage.management.utils

object LocationFreshness {
    fun isFresh(capturedElapsedNanos: Long, nowElapsedNanos: Long): Boolean =
        capturedElapsedNanos > 0 && capturedElapsedNanos <= nowElapsedNanos &&
            nowElapsedNanos - capturedElapsedNanos <= 60_000_000_000L
}
