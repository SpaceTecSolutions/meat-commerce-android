package com.spacetecsolutions.meatapp.core.data.firebase.di

import com.spacetecsolutions.meatapp.core.data.firebase.appcheck.FirebaseAppCheckManager
import com.spacetecsolutions.meatapp.core.data.firebase.auth.FirebaseAuthenticationRepository
import com.spacetecsolutions.meatapp.core.data.firebase.admin.FirebaseAdminManagementRepository
import com.spacetecsolutions.meatapp.core.domain.repository.AdminManagementRepository
import com.spacetecsolutions.meatapp.core.data.firebase.config.FirebaseFeatureManagementRepository
import com.spacetecsolutions.meatapp.core.domain.repository.FeatureManagementRepository
import com.spacetecsolutions.meatapp.core.data.firebase.product.FirebaseProductLimitRepository
import com.spacetecsolutions.meatapp.core.domain.repository.ProductLimitRepository
import com.spacetecsolutions.meatapp.core.data.firebase.reports.FirebaseSuperAdminReportsRepository
import com.spacetecsolutions.meatapp.core.domain.repository.SuperAdminReportsRepository
import com.spacetecsolutions.meatapp.core.data.firebase.dashboard.FirebaseAdminDashboardRepository
import com.spacetecsolutions.meatapp.core.domain.repository.AdminDashboardRepository
import com.spacetecsolutions.meatapp.core.data.firebase.category.FirebaseCategoryRepository
import com.spacetecsolutions.meatapp.core.domain.repository.CategoryRepository
import com.spacetecsolutions.meatapp.core.data.firebase.product.FirebaseProductRepository
import com.spacetecsolutions.meatapp.core.domain.repository.ProductRepository
import com.spacetecsolutions.meatapp.core.data.firebase.user.FirebaseUserRepository
import com.spacetecsolutions.meatapp.core.domain.repository.AuthenticationRepository
import com.spacetecsolutions.meatapp.core.domain.repository.UserRepository
import com.spacetecsolutions.meatapp.core.domain.service.AppCheckManager
import com.spacetecsolutions.meatapp.core.data.firebase.home.FirebaseCustomerHomeRepository
import com.spacetecsolutions.meatapp.core.domain.repository.CustomerHomeRepository
import com.spacetecsolutions.meatapp.core.data.cart.InMemoryCartBadgeStore
import com.spacetecsolutions.meatapp.core.domain.service.CartBadgeStore
import com.spacetecsolutions.meatapp.core.data.firebase.cart.FirebaseCustomerCartRepository
import com.spacetecsolutions.meatapp.core.domain.repository.CustomerCartRepository
import com.spacetecsolutions.meatapp.core.data.firebase.address.FirebaseCustomerAddressRepository
import com.spacetecsolutions.meatapp.core.domain.repository.CustomerAddressRepository
import com.spacetecsolutions.meatapp.core.data.firebase.config.FirebaseDeliveryConfigurationRepository
import com.spacetecsolutions.meatapp.core.domain.repository.DeliveryConfigurationRepository
import com.spacetecsolutions.meatapp.core.data.firebase.config.FirebasePaymentConfigurationRepository
import com.spacetecsolutions.meatapp.core.domain.repository.PaymentConfigurationRepository
import com.spacetecsolutions.meatapp.core.data.firebase.checkout.FirebaseCheckoutRepository
import com.spacetecsolutions.meatapp.core.domain.repository.CheckoutRepository
import com.spacetecsolutions.meatapp.core.data.firebase.order.FirebaseCodOrderRepository
import com.spacetecsolutions.meatapp.core.domain.repository.CodOrderRepository
import com.spacetecsolutions.meatapp.core.data.firebase.payment.FirebaseRazorpayPaymentRepository
import com.spacetecsolutions.meatapp.core.domain.repository.RazorpayPaymentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.spacetecsolutions.meatapp.core.data.firebase.staff.FirebaseDeliveryStaffRepository
import com.spacetecsolutions.meatapp.core.domain.repository.DeliveryStaffRepository
import com.spacetecsolutions.meatapp.core.data.firebase.realtime.FirebaseLiveTrackingRepository
import com.spacetecsolutions.meatapp.core.domain.repository.LiveTrackingRepository
import com.spacetecsolutions.meatapp.core.data.firebase.customer.FirebaseAdminCustomerRepository
import com.spacetecsolutions.meatapp.core.domain.repository.AdminCustomerRepository
import com.spacetecsolutions.meatapp.core.data.firebase.promotion.FirebasePromotionRepository
import com.spacetecsolutions.meatapp.core.domain.repository.PromotionRepository
import com.spacetecsolutions.meatapp.core.data.firebase.messaging.FirebaseMessagingRepository
import com.spacetecsolutions.meatapp.core.domain.repository.MessagingRepository
import com.spacetecsolutions.meatapp.core.data.firebase.reports.FirebaseAdminReportsRepository
import com.spacetecsolutions.meatapp.core.domain.repository.AdminReportsRepository
import com.spacetecsolutions.meatapp.core.data.firebase.settings.FirebaseAdminShopSettingsRepository
import com.spacetecsolutions.meatapp.core.domain.repository.AdminShopSettingsRepository
import com.spacetecsolutions.meatapp.core.data.firebase.settings.FirebaseShopSupportRepository
import com.spacetecsolutions.meatapp.core.domain.repository.ShopSupportRepository
import com.spacetecsolutions.meatapp.core.data.firebase.audit.FirebaseAuditLogRepository
import com.spacetecsolutions.meatapp.core.domain.repository.AuditLogRepository
import com.spacetecsolutions.meatapp.core.data.firebase.home.FirebaseBannerRepository
import com.spacetecsolutions.meatapp.core.domain.repository.BannerRepository
import com.spacetecsolutions.meatapp.core.data.firebase.content.FirebaseCatalogContentRepository
import com.spacetecsolutions.meatapp.core.domain.repository.CatalogContentRepository
import com.spacetecsolutions.meatapp.core.data.firebase.staff.FirebaseStaffRepository
import com.spacetecsolutions.meatapp.core.domain.repository.StaffRepository

@Module
@InstallIn(SingletonComponent::class)
internal abstract class FirebaseBindings {
    @Binds @Singleton
    abstract fun bindStaffRepository(implementation: FirebaseStaffRepository): StaffRepository
    @Binds @Singleton
    abstract fun bindCatalogContentRepository(implementation: FirebaseCatalogContentRepository): CatalogContentRepository
    @Binds
    @Singleton
    abstract fun bindBannerRepository(implementation: FirebaseBannerRepository): BannerRepository
    @Binds
    @Singleton
    abstract fun bindShopSupportRepository(
        implementation: FirebaseShopSupportRepository,
    ): ShopSupportRepository
    @Binds
    @Singleton
    abstract fun bindAuditLogRepository(
        implementation: FirebaseAuditLogRepository,
    ): AuditLogRepository
    @Binds
    @Singleton
    abstract fun bindAdminShopSettingsRepository(
        implementation: FirebaseAdminShopSettingsRepository,
    ): AdminShopSettingsRepository
    @Binds
    @Singleton
    abstract fun bindAdminReportsRepository(
        implementation: FirebaseAdminReportsRepository,
    ): AdminReportsRepository
    @Binds
    @Singleton
    abstract fun bindMessagingRepository(
        implementation: FirebaseMessagingRepository,
    ): MessagingRepository
    @Binds
    @Singleton
    abstract fun bindPromotionRepository(
        implementation: FirebasePromotionRepository,
    ): PromotionRepository
    @Binds
    @Singleton
    abstract fun bindAdminCustomerRepository(
        implementation: FirebaseAdminCustomerRepository,
    ): AdminCustomerRepository
    @Binds
    @Singleton
    abstract fun bindLiveTrackingRepository(
        implementation: FirebaseLiveTrackingRepository,
    ): LiveTrackingRepository
    @Binds
    @Singleton
    abstract fun bindDeliveryStaffRepository(
        implementation: FirebaseDeliveryStaffRepository,
    ): DeliveryStaffRepository

    @Binds
    @Singleton
    abstract fun bindRazorpayPaymentRepository(
        implementation: FirebaseRazorpayPaymentRepository,
    ): RazorpayPaymentRepository

    @Binds
    @Singleton
    abstract fun bindCodOrderRepository(implementation: FirebaseCodOrderRepository): CodOrderRepository

    @Binds
    @Singleton
    abstract fun bindCheckoutRepository(implementation: FirebaseCheckoutRepository): CheckoutRepository

    @Binds
    @Singleton
    abstract fun bindPaymentConfigurationRepository(
        implementation: FirebasePaymentConfigurationRepository,
    ): PaymentConfigurationRepository

    @Binds
    @Singleton
    abstract fun bindDeliveryConfigurationRepository(
        implementation: FirebaseDeliveryConfigurationRepository,
    ): DeliveryConfigurationRepository

    @Binds
    @Singleton
    abstract fun bindCustomerAddressRepository(
        implementation: FirebaseCustomerAddressRepository,
    ): CustomerAddressRepository

    @Binds
    @Singleton
    abstract fun bindCustomerCartRepository(
        implementation: FirebaseCustomerCartRepository,
    ): CustomerCartRepository

    @Binds
    @Singleton
    abstract fun bindCartBadgeStore(implementation: InMemoryCartBadgeStore): CartBadgeStore

    @Binds
    @Singleton
    abstract fun bindCustomerHomeRepository(
        implementation: FirebaseCustomerHomeRepository,
    ): CustomerHomeRepository

    @Binds
    @Singleton
    abstract fun bindProductRepository(
        implementation: FirebaseProductRepository,
    ): ProductRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        implementation: FirebaseCategoryRepository,
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindAdminDashboardRepository(
        implementation: FirebaseAdminDashboardRepository,
    ): AdminDashboardRepository

    @Binds
    @Singleton
    abstract fun bindSuperAdminReportsRepository(
        implementation: FirebaseSuperAdminReportsRepository,
    ): SuperAdminReportsRepository

    @Binds
    @Singleton
    abstract fun bindProductLimitRepository(
        implementation: FirebaseProductLimitRepository,
    ): ProductLimitRepository

    @Binds
    @Singleton
    abstract fun bindFeatureManagementRepository(
        implementation: FirebaseFeatureManagementRepository,
    ): FeatureManagementRepository

    @Binds
    @Singleton
    abstract fun bindAdminManagementRepository(
        implementation: FirebaseAdminManagementRepository,
    ): AdminManagementRepository

    @Binds
    @Singleton
    abstract fun bindAppCheckManager(implementation: FirebaseAppCheckManager): AppCheckManager

    @Binds
    @Singleton
    abstract fun bindAuthenticationRepository(
        implementation: FirebaseAuthenticationRepository,
    ): AuthenticationRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(implementation: FirebaseUserRepository): UserRepository
}
