package com.spacetecsolutions.meatapp.core.model

data class CreateAdminRequest(
    val displayName: String,
    val mobileNumber: String,
    val password: String,
)

data class UpdateAdminRequest(
    val userId: String,
    val displayName: String,
    val mobileNumber: String,
)
