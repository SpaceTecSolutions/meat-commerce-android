package com.spacetecsolutions.meatapp.core.model

data class DeliveryRoute(
    val encodedPolyline: String,
    val distanceMeters: Long,
    val durationSeconds: Long,
    val generatedAtEpochMillis: Long,
    val destinationLatitude: Double,
    val destinationLongitude: Double,
) {
    init {
        require(encodedPolyline.isNotBlank())
        require(distanceMeters >= 0 && durationSeconds >= 0 && generatedAtEpochMillis >= 0)
        require(destinationLatitude in -90.0..90.0 && destinationLongitude in -180.0..180.0)
    }
}
