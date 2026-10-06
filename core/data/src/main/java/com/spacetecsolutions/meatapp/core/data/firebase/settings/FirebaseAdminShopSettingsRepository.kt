package com.spacetecsolutions.meatapp.core.data.firebase.settings

import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.domain.repository.AdminShopSettingsRepository
import com.spacetecsolutions.meatapp.core.model.AdminShopSettings
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

internal class FirebaseAdminShopSettingsRepository @Inject constructor(
    private val functions: FirebaseFunctions,
) : AdminShopSettingsRepository {
    override suspend fun getSettings() = settingsCall("adminGetShopSettings")
    override suspend fun saveSettings(settings: AdminShopSettings) = settingsCall("adminSaveShopSettings", mapOf(
        "section" to "SHOP",
        "shopName" to settings.shopName, "address" to settings.address,
        "contactPhone" to settings.contactPhone, "contactEmail" to settings.contactEmail,
        "expectedRevision" to settings.revision,
    ))
    override suspend fun saveAppSettings(settings: AdminShopSettings) = settingsCall("adminSaveShopSettings", mapOf(
        "section" to "APP",
        "bannerTitle" to settings.bannerTitle, "bannerMessage" to settings.bannerMessage,
        "bannerEnabled" to settings.bannerEnabled,
        "expectedRevision" to settings.revision,
    ))
    override suspend fun saveSupportSettings(settings: AdminShopSettings) = settingsCall("adminSaveShopSettings", mapOf(
        "section" to "SUPPORT",
        "supportPhone" to settings.supportPhone, "supportWhatsApp" to settings.supportWhatsApp,
        "supportEmail" to settings.supportEmail,
        "expectedRevision" to settings.revision,
    ))
    override suspend fun updateProfile(firstName: String, lastName: String): AppResult<Unit> = try {
        call("adminUpdateProfile", mapOf("firstName" to firstName, "lastName" to lastName))
        AppResult.Success(Unit)
    } catch (error: Exception) { AppResult.Failure(error.settingsError()) }
    override suspend fun changePassword(currentPassword: String, newPassword: String): AppResult<Unit> = try {
        call("adminChangePassword", mapOf("currentPassword" to currentPassword, "newPassword" to newPassword))
        AppResult.Success(Unit)
    } catch (error: Exception) { AppResult.Failure(error.settingsError()) }

    private suspend fun settingsCall(name: String, payload: Map<String, Any?> = emptyMap()): AppResult<AdminShopSettings> = try {
        val response = call(name, payload)
        AppResult.Success((response["settings"] as? Map<*, *> ?: response).settings())
    } catch (error: Exception) { AppResult.Failure(error.settingsError()) }
    private suspend fun call(name: String, payload: Map<String, Any?>) =
        functions.getHttpsCallable(name).call(payload).await().data as? Map<*, *> ?: error("Invalid settings response")
}

private fun Map<*, *>.settings() = AdminShopSettings(
    shopId = this["shopId"] as? String ?: error("Missing shop"),
    shopName = this["shopName"] as? String ?: "", address = this["address"] as? String ?: "",
    contactPhone = this["contactPhone"] as? String ?: "", contactEmail = this["contactEmail"] as? String,
    supportPhone = this["supportPhone"] as? String, supportWhatsApp = this["supportWhatsApp"] as? String,
    supportEmail = this["supportEmail"] as? String,
    bannerTitle = this["bannerTitle"] as? String, bannerMessage = this["bannerMessage"] as? String,
    bannerEnabled = this["bannerEnabled"] as? Boolean ?: false,
    bannerEditingAllowed = this["bannerEditingAllowed"] as? Boolean ?: false,
    profileName = this["profileName"] as? String ?: "", profileMobile = this["profileMobile"] as? String ?: "",
    revision = (this["revision"] as? Number)?.toLong() ?: 0,
)
private fun Throwable.settingsError(): AppError = when ((this as? FirebaseFunctionsException)?.code) {
    FirebaseFunctionsException.Code.UNAUTHENTICATED -> AppError.InvalidCredentials
    FirebaseFunctionsException.Code.PERMISSION_DENIED -> AppError.Forbidden
    FirebaseFunctionsException.Code.INVALID_ARGUMENT -> AppError.Validation(message = "Check the entered details")
    FirebaseFunctionsException.Code.FAILED_PRECONDITION -> AppError.Validation(message = "This setting is not authorized")
    FirebaseFunctionsException.Code.ABORTED -> AppError.Validation(message = "Settings changed. Refresh and retry")
    FirebaseFunctionsException.Code.UNAVAILABLE -> AppError.Network
    else -> AppError.Unknown(this)
}
