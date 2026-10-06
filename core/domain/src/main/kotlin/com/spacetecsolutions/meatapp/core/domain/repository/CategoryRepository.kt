package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.CategoryImageUpload
import com.spacetecsolutions.meatapp.core.model.CategoryInput
import com.spacetecsolutions.meatapp.core.model.ProductCategory

interface CategoryRepository {
    suspend fun getAdminCategories(): AppResult<List<ProductCategory>>
    suspend fun getActiveCategories(): AppResult<List<ProductCategory>>
    suspend fun uploadCategoryImage(localUri: String): AppResult<CategoryImageUpload>
    suspend fun createCategory(input: CategoryInput): AppResult<ProductCategory>
    suspend fun updateCategory(input: CategoryInput): AppResult<ProductCategory>
    suspend fun setCategoryActive(
        categoryId: String,
        active: Boolean,
        expectedRevision: Long,
    ): AppResult<ProductCategory>
    suspend fun updateCategoryOrder(categoryIds: List<String>): AppResult<List<ProductCategory>>
    suspend fun deleteCategory(categoryId: String, expectedRevision: Long): AppResult<Unit>
}
