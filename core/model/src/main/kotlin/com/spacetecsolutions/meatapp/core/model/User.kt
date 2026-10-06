package com.spacetecsolutions.meatapp.core.model

data class User(
    val id: String,
    val mobileNumber: String,
    val displayName: String,
    val firstName: String = displayName.substringBefore(' '),
    val lastName: String = displayName.substringAfter(' ', ""),
    val role: UserRole,
    val shopId: String? = null,
    val active: Boolean = true,
    val createdAtEpochMillis: Long = 0L,
    val updatedAtEpochMillis: Long = 0L,
    val lastLoginAtEpochMillis: Long = 0L,
    val email: String? = null,
    val permissions: Set<StaffPermission> = emptySet(),
) {
    init {
        require(id.isNotBlank()) { "User id cannot be blank" }
        require(mobileNumber.isNotBlank()) { "Mobile number cannot be blank" }
    }
}
