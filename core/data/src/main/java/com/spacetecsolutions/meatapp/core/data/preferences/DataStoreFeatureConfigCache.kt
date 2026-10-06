package com.spacetecsolutions.meatapp.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.spacetecsolutions.meatapp.core.model.FeatureConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class DataStoreFeatureConfigCache @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : FeatureConfigCache {
    override val config: Flow<FeatureConfig?> = dataStore.data.map { values ->
        if (values[CACHED] != true) null else FeatureConfig(
            deliveryStaffManagementAllowed = values[DELIVERY_STAFF] ?: false,
            realtimeTrackingAllowed = values[TRACKING] ?: false,
            codAllowed = values[COD] ?: false,
            razorpayAllowed = values[RAZORPAY] ?: false,
            upiAllowed = values[UPI] ?: false,
            offersAllowed = values[OFFERS] ?: false,
            couponsAllowed = values[COUPONS] ?: false,
            scheduledDeliveryAllowed = values[SCHEDULED] ?: false,
            inAppNotificationsEnabled = values[IN_APP_NOTIFICATIONS] ?: true,
            maxProducts = values[MAX_PRODUCTS] ?: FeatureConfig.DEFAULT_MAX_PRODUCTS,
            revision = values[REVISION] ?: 0L,
        )
    }

    override suspend fun save(config: FeatureConfig) {
        dataStore.edit {
            it[CACHED] = true
            it[DELIVERY_STAFF] = config.deliveryStaffManagementAllowed
            it[TRACKING] = config.realtimeTrackingAllowed
            it[COD] = config.codAllowed
            it[RAZORPAY] = config.razorpayAllowed
            it[UPI] = config.upiAllowed
            it[OFFERS] = config.offersAllowed
            it[COUPONS] = config.couponsAllowed
            it[SCHEDULED] = config.scheduledDeliveryAllowed
            it[IN_APP_NOTIFICATIONS] = config.inAppNotificationsEnabled
            it[MAX_PRODUCTS] = config.maxProducts
            it[REVISION] = config.revision
        }
    }

    private companion object {
        val CACHED = booleanPreferencesKey("feature_config_cached")
        val DELIVERY_STAFF = booleanPreferencesKey("feature_delivery_staff")
        val TRACKING = booleanPreferencesKey("feature_realtime_tracking")
        val COD = booleanPreferencesKey("feature_cod")
        val RAZORPAY = booleanPreferencesKey("feature_razorpay")
        val UPI = booleanPreferencesKey("feature_upi")
        val OFFERS = booleanPreferencesKey("feature_offers")
        val COUPONS = booleanPreferencesKey("feature_coupons")
        val SCHEDULED = booleanPreferencesKey("feature_scheduled_delivery")
        val IN_APP_NOTIFICATIONS = booleanPreferencesKey("feature_in_app_notifications")
        val MAX_PRODUCTS = intPreferencesKey("feature_max_products")
        val REVISION = longPreferencesKey("feature_config_revision")
    }
}
