package com.spacetecsolutions.meatapp.core.data.firebase.audit

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.domain.repository.AuditLogRepository
import com.spacetecsolutions.meatapp.core.model.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirebaseAuditLogRepository @Inject constructor(
    private val functions: FirebaseFunctions,
) : AuditLogRepository {
    override suspend fun getAuditLog(action: PrivilegedAuditAction?, cursor: String?, limit: Int): AppResult<AuditLogPage> = try {
        val payload = buildMap<String, Any> {
            action?.let { put("action", it.name) }
            cursor?.takeIf(String::isNotBlank)?.let { put("cursor", it) }
            put("limit", limit.coerceIn(1, 100))
        }
        val response = functions.getHttpsCallable("superAdminGetAuditLog").call(payload).await().data as? Map<*, *>
            ?: return AppResult.Failure(AppError.Unknown())
        val entries = (response["entries"] as? List<*>).orEmpty().mapNotNull { (it as? Map<*, *>)?.entry() }
        AppResult.Success(AuditLogPage(entries, response["nextCursor"] as? String))
    } catch (error: Exception) { AppResult.Failure(error.auditError()) }
}

private fun Map<*, *>.entry(): AuditLogEntry? = runCatching { AuditLogEntry(
    id = this["id"] as String,
    action = enumValueOf(this["action"] as String),
    actorUserId = this["actorUserId"] as String,
    actorDisplayName = this["actorDisplayName"] as? String ?: "Super Admin",
    targetId = this["targetId"] as? String,
    summary = this["summary"] as String,
    occurredAtEpochMillis = (this["occurredAtEpochMillis"] as Number).toLong(),
    correlationId = this["correlationId"] as String,
) }.getOrNull()
private fun Throwable.auditError(): AppError = when ((this as? FirebaseFunctionsException)?.code) {
    FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.Unauthorized
    FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppError.Forbidden
    FirebaseFunctionsException.Code.INVALID_ARGUMENT -> AppError.Validation(message = "Invalid audit filter")
    FirebaseFunctionsException.Code.UNAVAILABLE -> AppError.Network
    else -> AppError.Unknown(this)
}
