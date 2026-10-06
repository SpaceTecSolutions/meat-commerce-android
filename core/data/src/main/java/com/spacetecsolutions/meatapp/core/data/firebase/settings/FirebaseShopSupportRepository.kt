package com.spacetecsolutions.meatapp.core.data.firebase.settings

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.ShopSupportRepository
import com.spacetecsolutions.meatapp.core.model.ShopSupport
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

internal class FirebaseShopSupportRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : ShopSupportRepository {
    override suspend fun getSupport(): AppResult<ShopSupport> = try {
        val uid = auth.currentUser?.uid
        val shopId = if (uid == null) "default" else firestore.collection("users").document(uid)
            .get(Source.DEFAULT).await().getString("shopId").orEmpty()
        if (shopId.isBlank()) return AppResult.Failure(AppError.Validation(message = "Shop is not configured"))
        val shop = firestore.collection("shops").document(shopId).get(Source.DEFAULT).await()
        AppResult.Success(ShopSupport(
            shopName = shop.getString("shopName") ?: shop.getString("displayName").orEmpty(),
            email = shop.getString("supportEmail").clean() ?: shop.getString("contactEmail").clean(),
            whatsappNumber = shop.getString("supportWhatsApp").clean(),
            callNumber = shop.getString("supportPhone").clean() ?: shop.getString("contactPhone").clean(),
            address = shop.getString("address").clean(),
        ))
    } catch (error: Exception) {
        AppResult.Failure(AppError.Unknown(error))
    }
}

private fun String?.clean() = this?.trim()?.takeIf(String::isNotBlank)
