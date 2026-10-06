package com.spacetecsolutions.meatapp.feature.admin.staff

import com.spacetecsolutions.meatapp.core.model.User

data class StaffForm(
    val userId: String? = null,
    val name: String = "",
    val mobile: String = "",
    val password: String = "",
    val error: String? = null,
) { val editing get() = userId != null }

data class DeliveryStaffUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val submitting: Boolean = false,
    val staff: List<User> = emptyList(),
    val form: StaffForm? = null,
    val statusChange: User? = null,
    val error: String? = null,
    val message: StaffMessage? = null,
)

data class StaffMessage(val text: String, val success: Boolean)
