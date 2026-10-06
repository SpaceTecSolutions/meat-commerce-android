package com.spacetecsolutions.meatapp.feature.superadmin.admin

import com.spacetecsolutions.meatapp.core.model.User

data class AdminFormState(
    val userId: String? = null,
    val name: String = "",
    val mobile: String = "",
    val password: String = "",
    val nameError: String? = null,
    val mobileError: String? = null,
    val passwordError: String? = null,
) {
    val editing: Boolean get() = userId != null
}

data class AdminStatusConfirmation(
    val admin: User,
    val activate: Boolean,
)

data class AdminManagementUiState(
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val submitting: Boolean = false,
    val query: String = "",
    val admins: List<User> = emptyList(),
    val error: String? = null,
    val form: AdminFormState? = null,
    val confirmation: AdminStatusConfirmation? = null,
    val message: AdminMessage? = null,
) {
    val filteredAdmins: List<User>
        get() = admins.filter {
            query.isBlank() || it.displayName.contains(query, ignoreCase = true) ||
                it.mobileNumber.contains(query.filter(Char::isDigit))
        }
}

data class AdminMessage(val text: String, val success: Boolean)
