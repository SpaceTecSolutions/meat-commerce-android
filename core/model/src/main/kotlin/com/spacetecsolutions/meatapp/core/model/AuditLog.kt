package com.spacetecsolutions.meatapp.core.model

enum class PrivilegedAuditAction {
    ADMIN_CREATED,
    ADMIN_ACTIVATED,
    ADMIN_DISABLED,
    FEATURE_CHANGED,
    PRODUCT_LIMIT_CHANGED,
    PAYMENT_FEATURE_CHANGED,
}

data class AuditLogEntry(
    val id: String,
    val action: PrivilegedAuditAction,
    val actorUserId: String,
    val actorDisplayName: String,
    val targetId: String? = null,
    val summary: String,
    val occurredAtEpochMillis: Long,
    val correlationId: String,
)

data class AuditLogPage(
    val entries: List<AuditLogEntry>,
    val nextCursor: String? = null,
)
