package com.spacetecsolutions.meatapp.core.model

data class CreateDeliveryStaffRequest(
    val displayName: String,
    val mobileNumber: String,
    val password: String,
)

data class UpdateDeliveryStaffRequest(
    val userId: String,
    val displayName: String,
    val mobileNumber: String,
)
