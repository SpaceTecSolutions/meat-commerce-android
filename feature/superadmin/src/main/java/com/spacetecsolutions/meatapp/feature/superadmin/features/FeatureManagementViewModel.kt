package com.spacetecsolutions.meatapp.feature.superadmin.features

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.FeatureManagementRepository
import com.spacetecsolutions.meatapp.core.model.FeatureConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeatureManagementViewModel @Inject constructor(
    private val repository: FeatureManagementRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(FeatureManagementUiState())
    val state = mutableState.asStateFlow()
    private var refreshing = true

    init {
        viewModelScope.launch {
            repository.featureConfig.collect { result ->
                when (result) {
                    null -> mutableState.update { it.copy(loading = true) }
                    is AppResult.Success -> if (!refreshing) showConfig(result.value)
                    is AppResult.Failure -> mutableState.update {
                        it.copy(loading = false, error = result.error.message())
                    }
                }
            }
        }
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        refreshing = true
        mutableState.update { it.copy(loading = true, error = null) }
        when (val result = repository.refresh()) {
            is AppResult.Success -> {
                refreshing = false
                showConfig(result.value)
            }
            is AppResult.Failure -> mutableState.update {
                refreshing = false
                it.copy(loading = false, error = if (it.config == null) result.error.message() else null,
                    message = FeatureMessage(result.error.message(), false))
            }
        }
    }

    fun toggle(feature: ManagedFeature, enabled: Boolean) = mutableState.update { state ->
        if (feature == ManagedFeature.COD || feature == ManagedFeature.SCHEDULED_DELIVERY) return@update state
        val current = state.draft ?: return@update state
        val changed = when (feature) {
            ManagedFeature.DELIVERY_STAFF -> current.copy(deliveryStaffManagementAllowed = enabled)
            ManagedFeature.STAFF_MANAGEMENT -> current.copy(staffManagementAllowed = enabled)
            ManagedFeature.SUBCATEGORIES -> current.copy(subcategoriesAllowed = enabled)
            ManagedFeature.REALTIME_TRACKING -> current.copy(realtimeTrackingAllowed = enabled)
            ManagedFeature.COD -> current
            ManagedFeature.RAZORPAY -> current.copy(razorpayAllowed = enabled)
            ManagedFeature.UPI -> current.copy(upiAllowed = enabled)
            ManagedFeature.OFFERS -> current.copy(offersAllowed = enabled)
            ManagedFeature.COUPONS -> current.copy(couponsAllowed = enabled)
            ManagedFeature.SCHEDULED_DELIVERY -> current
            ManagedFeature.IN_APP_NOTIFICATIONS -> current.copy(inAppNotificationsEnabled = enabled)
        }
        state.copy(draft = changed)
    }

    fun updateMaxProducts(value: String) {
        if (value.all(Char::isDigit) && value.length <= 6) {
            mutableState.update { state ->
                val parsed = value.toIntOrNull()
                state.copy(
                    maxProductsText = value,
                    maxProductsError = validateMaximum(parsed),
                    draft = if (parsed != null && parsed <= MAX_PRODUCTS_LIMIT) {
                        state.draft?.copy(maxProducts = parsed)
                    } else state.draft,
                )
            }
        }
    }

    fun save() {
        val current = state.value
        val draft = current.draft ?: return
        val maximum = current.maxProductsText.toIntOrNull()
        val error = validateMaximum(maximum)
        if (error != null) {
            mutableState.update { it.copy(maxProductsError = error) }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(saving = true) }
            when (val result = repository.update(draft.copy(maxProducts = maximum!!,
                codAllowed = true, scheduledDeliveryAllowed = true))) {
                is AppResult.Success -> {
                    mutableState.update { it.copy(saving = false) }
                    showConfig(result.value)
                    mutableState.update { it.copy(message = FeatureMessage("Feature settings saved", true)) }
                }
                is AppResult.Failure -> if (result.error is AppError.Validation &&
                    (result.error as AppError.Validation).message.startsWith("Settings changed elsewhere")) {
                    when (val latest = repository.refresh()) {
                        is AppResult.Success -> {
                            mutableState.update { it.copy(saving = false) }
                            showConfig(latest.value)
                            mutableState.update { it.copy(message = FeatureMessage(
                                "Settings changed elsewhere. Latest settings loaded; review and save again", false)) }
                        }
                        is AppResult.Failure -> mutableState.update { it.copy(saving = false,
                            message = FeatureMessage("Settings changed elsewhere. Refresh and retry", false)) }
                    }
                } else mutableState.update {
                    it.copy(saving = false, message = FeatureMessage(result.error.message(), false))
                }
            }
        }
    }

    fun discardChanges() = state.value.config?.let(::showConfig)
    fun consumeMessage() = mutableState.update { it.copy(message = null) }

    private fun showConfig(config: FeatureConfig) = mutableState.update { current ->
        val previous = current.config
        when {
            current.saving || (previous != null && config.revision < previous.revision) -> current
            current.dirty && previous != null && config.revision == previous.revision ->
                current.copy(config = config, loading = false)
            else -> current.copy(
            loading = false,
            config = config,
            draft = config,
            maxProductsText = config.maxProducts.toString(),
            maxProductsError = null,
            error = null,
            message = if (current.dirty && previous != null && config.revision > previous.revision)
                FeatureMessage("Settings changed elsewhere. Latest settings loaded; review your changes", false)
                else current.message,
        )
        }
    }

    private fun validateMaximum(value: Int?): String? = when {
        value == null -> "Maximum products is required"
        value > MAX_PRODUCTS_LIMIT -> "Maximum supported limit is $MAX_PRODUCTS_LIMIT"
        else -> null
    }

    private companion object { const val MAX_PRODUCTS_LIMIT = 100_000 }
}

private fun AppError.message(): String = when (this) {
    AppError.Forbidden, AppError.Unauthorized -> "Only an authorized Super Admin can change features"
    AppError.Network, AppError.Offline, AppError.Timeout -> "Check your connection and try again"
    is AppError.Validation -> message
    else -> "Unable to load feature settings. Try again"
}
