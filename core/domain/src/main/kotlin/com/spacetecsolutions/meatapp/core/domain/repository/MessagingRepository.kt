package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.AppNotification
import kotlinx.coroutines.flow.Flow

interface MessagingRepository {
    fun observeUnreadCount(): Flow<AppResult<Int>>
    suspend fun getRegistrationToken(): AppResult<String>
    suspend fun registerDeviceToken(token: String): AppResult<Unit>
    suspend fun deleteRegistrationToken(): AppResult<Unit>
    suspend fun getNotificationHistory(limit: Int = 50): AppResult<List<AppNotification>>
    fun observeNotificationHistory(limit: Int = 50): Flow<AppResult<List<AppNotification>>>
    suspend fun markNotificationRead(notificationId: String): AppResult<Unit>
    suspend fun markAllNotificationsRead(): AppResult<Unit>
    suspend fun clearAllNotifications(): AppResult<Unit>
}
