import {initializeApp} from "firebase-admin/app";

initializeApp();

export {authSignInWithMobilePassword} from "./sign-in.js";
export {
  customerQuoteGuestCart,
  customerMergeGuestCart,
} from "./customer-guest-cart.js";

export {
  authVerifyCustomerRegistration,
  authSignInCustomerWithPhone,
  authRegisterCustomer,
  authVerifyPasswordResetCode,
  authResetPassword,
} from "./customer-auth.js";
export {
  superAdminCreateAdmin,
  superAdminListAdmins,
  superAdminSetAdminActive,
  superAdminUpdateAdmin,
} from "./admin-management.js";
export {
  getAllowedFeatureConfig,
  superAdminUpdateFeatureConfig,
} from "./feature-management.js";
export {
  superAdminGetProductLimitStatus,
  superAdminUpdateProductLimit,
} from "./product-limit.js";
export {adminGetReport, superAdminGetReport} from "./reports.js";
export {superAdminGetAuditLog} from "./superadmin-audit.js";
export {adminGetDashboard} from "./admin-dashboard.js";
export {
  adminGetPaymentConfig,
  adminUpdatePaymentConfig,
  adminGetDeliveryConfig,
  adminUpdateDeliveryConfig,
} from "./admin-configuration.js";
export {
  adminGetShopSettings,
  adminSaveShopSettings,
  adminUpdateProfile,
  adminChangePassword,
} from "./admin-settings.js";
export {
  adminGetCustomers,
  adminGetCustomerDetails,
  adminSetCustomerActive,
} from "./admin-customers.js";
export {
  adminGetOrders,
  adminConfirmCodOrder,
  adminStartPreparingOrder,
  adminMarkOrderReadyForDelivery,
  adminCancelCodOrder,
  adminAssignOrderToSelf,
  adminAssignDeliveryUser,
  adminStartAssignedDelivery,
  adminCompleteDelivery,
  adminReportCodMismatch,
} from "./admin-orders.js";
export {
  adminBeginCategoryImageUpload,
  adminCreateCategory,
  adminDeleteCategory,
  adminGetCategories,
  adminSetCategoryActive,
  adminUpdateCategory,
  adminUpdateCategoryOrder,
  customerGetActiveCategories,
} from "./category-management.js";
export {
  adminBeginProductImageUpload,
  adminArchiveProduct,
  adminCreateProduct,
  adminGetProducts,
  adminSetProductActive,
  adminUpdateProduct,
  customerGetProduct,
  customerGetProducts,
  customerAddToCart,
} from "./product-management.js";
export {
  customerGetCart,
  customerSetCartQuantity,
  customerRemoveCartItem,
} from "./customer-cart.js";
export {
  customerGetAddresses,
  customerCreateAddress,
  customerUpdateAddress,
  customerDeleteAddress,
  customerSetDefaultAddress,
} from "./customer-addresses.js";
export {
  customerPrepareCheckout,
  customerPlaceOrder,
  customerReconcileOrderCreation,
} from "./customer-checkout.js";
export {
  adminVerifyRazorpayConfiguration,
  customerCreateRazorpayOrder,
  customerRetryRazorpayPayment,
  customerMarkRazorpaySdkOpened,
  customerVerifyRazorpayPayment,
  customerRecordRazorpayFailure,
  customerReconcilePendingRazorpayPayment,
} from "./razorpay-payments.js";
export {razorpayWebhook} from "./razorpay-webhook.js";
export {
  customerGetOrders,
  customerCancelOrder,
  customerReorder,
} from "./customer-orders.js";
export {
  deliveryGetAssignedCodOrders,
  deliveryStartAssignedOrder,
  deliveryCompleteAssignedOrder,
  deliveryReportCodMismatch,
  activateDeliveryTracking,
  stopDeliveryTracking,
} from "./delivery-tracking.js";
export {customerGetHome} from "./customer-home.js";
export {customerGetActiveSubcategories, adminBeginSubcategoryImageUpload, adminGetSubcategories, adminSaveSubcategory,
  adminDeleteSubcategory} from "./subcategory-management.js";
export {customerGetFaqs, adminGetFaqs, adminSaveFaq, adminDeleteFaq} from "./faq-management.js";
export {adminListStaff, adminCreateStaff, adminUpdateStaff} from "./staff-management.js";
export {
  adminBeginBannerImageUpload,
  adminGetBanners,
  adminCreateBanner,
  adminUpdateBanner,
  adminSetBannerActive,
  adminDeleteBanner,
} from "./banner-management.js";
export {customerGetTrackingRoute} from "./customer-tracking-route.js";
export {customerGetDeliveryOtp, verifyDeliveryOtp} from "./delivery-otp.js";
export {
  registerNotificationDevice,
  unregisterNotificationDevice,
  getNotificationHistory,
  markNotificationRead,
  markAllNotificationsRead,
  clearAllNotifications,
  notifyOrderCreated,
  notifyOrderUpdated,
  notifyLowStock,
  notifyFeatureConfigChanged,
  notifyOfferActivated,
  notifyCouponActivated,
} from "./notifications.js";
export {
  sendDelayedOrderNotifications,
  sendDeliveryStartReminders,
  sendWeeklySuperAdminReport,
  sendMonthlySuperAdminReport,
} from "./notification-schedules.js";
export {customerUpdateProfile} from "./customer-profile.js";
export {customerDeleteAccount} from "./customer-account-deletion.js";
