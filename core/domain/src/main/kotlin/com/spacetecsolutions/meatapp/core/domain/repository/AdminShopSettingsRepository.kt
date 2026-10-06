package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.AdminShopSettings

interface AdminShopSettingsRepository {
    suspend fun getSettings(): AppResult<AdminShopSettings>
    suspend fun saveSettings(settings: AdminShopSettings): AppResult<AdminShopSettings>
    suspend fun saveAppSettings(settings: AdminShopSettings): AppResult<AdminShopSettings>
    suspend fun saveSupportSettings(settings: AdminShopSettings): AppResult<AdminShopSettings>
    suspend fun updateProfile(firstName: String, lastName: String): AppResult<Unit>
    suspend fun changePassword(currentPassword: String, newPassword: String): AppResult<Unit>
}
