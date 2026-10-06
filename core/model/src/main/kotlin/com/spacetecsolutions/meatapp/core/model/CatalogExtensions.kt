package com.spacetecsolutions.meatapp.core.model

data class ProductSubcategory(
    val id: String,
    val categoryId: String,
    val name: String,
    val description: String = "",
    val active: Boolean = true,
    val sortOrder: Int = 0,
    val revision: Long = 0,
    val imageUrl: String? = null,
)

data class SubcategoryInput(
    val subcategoryId: String? = null,
    val categoryId: String,
    val name: String,
    val description: String,
    val sortOrder: Int,
    val active: Boolean,
    val expectedRevision: Long? = null,
    val imageUploadToken: String? = null,
)

data class SubcategoryImageUpload(val uploadToken: String)

data class FaqEntry(
    val id: String,
    val question: String,
    val answer: String,
    val active: Boolean = true,
    val sortOrder: Int = 0,
    val revision: Long = 0,
    val createdAtEpochMillis: Long = 0,
    val updatedAtEpochMillis: Long = 0,
    val createdBy: String? = null,
)

data class FaqInput(
    val faqId: String? = null,
    val question: String,
    val answer: String,
    val active: Boolean,
    val sortOrder: Int,
    val expectedRevision: Long? = null,
)

enum class StaffPermission {
    VIEW_ORDERS, UPDATE_ORDER_STATUS, ADJUST_FINAL_BILL, GENERATE_INVOICE,
    MANAGE_PRODUCTS, MANAGE_STOCK, MANAGE_FAQ,
}

/** UI-facing projection of trusted Staff permissions. The backend remains authoritative. */
data class StaffCapabilities(
    val canViewOrders: Boolean,
    val canUpdateOrderStatus: Boolean,
    val canAdjustFinalBill: Boolean,
    val canGenerateInvoice: Boolean,
    val canManageProducts: Boolean,
    val canManageStock: Boolean,
    val canManageFaq: Boolean,
) {
    val canOpenProducts: Boolean get() = canManageProducts || canManageStock
    val isOrdersReadOnly: Boolean get() = canViewOrders && !canUpdateOrderStatus &&
        !canAdjustFinalBill && !canGenerateInvoice
    val hasOperationalHome: Boolean get() = canOpenProducts || canManageFaq ||
        canUpdateOrderStatus || canAdjustFinalBill || canGenerateInvoice

    companion object {
        fun from(permissions: Set<StaffPermission>) = StaffCapabilities(
            canViewOrders = StaffPermission.VIEW_ORDERS in permissions,
            canUpdateOrderStatus = StaffPermission.UPDATE_ORDER_STATUS in permissions,
            canAdjustFinalBill = StaffPermission.ADJUST_FINAL_BILL in permissions,
            canGenerateInvoice = StaffPermission.GENERATE_INVOICE in permissions,
            canManageProducts = StaffPermission.MANAGE_PRODUCTS in permissions,
            canManageStock = StaffPermission.MANAGE_STOCK in permissions,
            canManageFaq = StaffPermission.MANAGE_FAQ in permissions,
        )
    }
}
