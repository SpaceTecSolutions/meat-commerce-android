package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.AuditLogPage
import com.spacetecsolutions.meatapp.core.model.PrivilegedAuditAction

interface AuditLogRepository {
    suspend fun getAuditLog(
        action: PrivilegedAuditAction? = null,
        cursor: String? = null,
        limit: Int = 40,
    ): AppResult<AuditLogPage>
}
