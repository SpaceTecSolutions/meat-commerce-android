package com.spacetecsolutions.meatapp.core.data.preferences

import com.spacetecsolutions.meatapp.core.model.UserRole
import kotlinx.coroutines.flow.Flow

interface AppPreferences {
    val darkThemeEnabled: Flow<Boolean?>
    val lastAuthenticatedRole: Flow<UserRole?>

    suspend fun setDarkThemeEnabled(enabled: Boolean)
    suspend fun setLastAuthenticatedRole(role: UserRole?)
}
