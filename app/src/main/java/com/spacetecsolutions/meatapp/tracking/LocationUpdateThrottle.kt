package com.spacetecsolutions.meatapp.tracking

import android.location.Location

internal class LocationUpdateThrottle(
    private val minimumTimeMillis: Long = 15_000,
    private val minimumDistanceMeters: Float = 25f,
) {
    private var last: Location? = null

    fun shouldPublish(candidate: Location): Boolean {
        val previous = last
        val accepted = previous == null ||
            candidate.time - previous.time >= minimumTimeMillis ||
            candidate.distanceTo(previous) >= minimumDistanceMeters
        if (accepted) last = candidate
        return accepted
    }
}
