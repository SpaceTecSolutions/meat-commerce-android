package com.spacetecsolutions.meatapp.core.model

data class AdminCustomerSummary(
    val id: String,
    val displayName: String,
    val mobileNumber: String,
    val active: Boolean,
    val totalOrders: Long,
    val totalSpendingMinor: Long,
    val currencyCode: String = "INR",
    val statusManagementAllowed: Boolean = false,
    val revision: Long = 0,
) {
    init {
        require(id.isNotBlank() && mobileNumber.isNotBlank())
        require(totalOrders >= 0 && totalSpendingMinor >= 0 && revision >= 0)
    }
}

data class AdminCustomerDetails(
    val customer: AdminCustomerSummary,
    val joinedAtEpochMillis: Long,
    val orders: List<AdminCustomerOrder> = emptyList(),
    val highestOrderMinor: Long = 0,
)

data class AdminCustomerOrder(
    val id: String,
    val displayNumber: String,
    val createdAtEpochMillis: Long,
    val productSummary: String,
    val totalMinor: Long,
    val paymentMethod: CheckoutPaymentMethod,
    val paymentStatus: PaymentStatus,
    val orderStatus: OrderStatus,
) {
    init { require(id.isNotBlank() && totalMinor >= 0 && createdAtEpochMillis >= 0) }
}
