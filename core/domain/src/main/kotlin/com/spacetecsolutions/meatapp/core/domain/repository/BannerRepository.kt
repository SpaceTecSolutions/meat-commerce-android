package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.BannerImageUpload
import com.spacetecsolutions.meatapp.core.model.BannerInput
import com.spacetecsolutions.meatapp.core.model.PromotionBanner

interface BannerRepository {
    suspend fun getBanners(): AppResult<List<PromotionBanner>>
    suspend fun uploadImage(localUri: String): AppResult<BannerImageUpload>
    suspend fun save(input: BannerInput): AppResult<PromotionBanner>
    suspend fun setActive(id: String, active: Boolean, expectedRevision: Long): AppResult<PromotionBanner>
    suspend fun delete(id: String, expectedRevision: Long): AppResult<Unit>
}
