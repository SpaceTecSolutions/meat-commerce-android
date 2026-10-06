package com.spacetecsolutions.meatapp.core.model

data class AdminShopSettings(
    val shopId: String,
    val shopName: String,
    val address: String,
    val contactPhone: String,
    val contactEmail: String? = null,
    val supportPhone: String? = null,
    val supportWhatsApp: String? = null,
    val supportEmail: String? = null,
    val bannerTitle: String? = null,
    val bannerMessage: String? = null,
    val bannerEnabled: Boolean = false,
    val bannerEditingAllowed: Boolean = false,
    val profileName: String,
    val profileMobile: String,
    val revision: Long = 0,
)
