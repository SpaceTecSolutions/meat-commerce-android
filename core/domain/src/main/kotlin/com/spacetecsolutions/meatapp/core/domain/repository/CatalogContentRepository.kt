package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.*

interface CatalogContentRepository {
    suspend fun getSubcategories(categoryId: String, admin: Boolean): AppResult<List<ProductSubcategory>>
    suspend fun uploadSubcategoryImage(localUri: String): AppResult<SubcategoryImageUpload>
    suspend fun saveSubcategory(input: SubcategoryInput): AppResult<ProductSubcategory>
    suspend fun deleteSubcategory(id: String): AppResult<Unit>
    suspend fun getFaqs(admin: Boolean): AppResult<List<FaqEntry>>
    suspend fun saveFaq(input: FaqInput): AppResult<FaqEntry>
    suspend fun deleteFaq(id: String): AppResult<Unit>
}
