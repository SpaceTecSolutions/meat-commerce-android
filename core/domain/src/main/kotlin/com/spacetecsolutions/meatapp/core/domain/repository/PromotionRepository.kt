package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.Promotion

interface PromotionRepository {
    suspend fun getAdminPromotions(): AppResult<List<Promotion>>
    suspend fun savePromotion(promotion: Promotion): AppResult<Promotion>
    suspend fun setPromotionActive(id: String, active: Boolean, expectedRevision: Long): AppResult<Promotion>
}
