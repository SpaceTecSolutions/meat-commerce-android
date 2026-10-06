package com.spacetecsolutions.meatapp.core.model

data class ProductCategory(
    val id: String,
    val name: String,
    val description: String = "",
    val imageUrl: String? = null,
    val active: Boolean = true,
    val sortOrder: Int = 0,
    val productCount: Int = 0,
    val activeProductCount: Int = 0,
    val revision: Long = 0L,
)

data class CategoryInput(
    val categoryId: String? = null,
    val name: String,
    val description: String,
    val imageUploadToken: String? = null,
    val expectedRevision: Long? = null,
    val sortOrder: Int = 0,
    val active: Boolean = true,
)

data class CategoryImageUpload(val uploadToken: String)
