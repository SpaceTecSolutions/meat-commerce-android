package com.spacetecsolutions.meatapp.feature.admin.delivery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.DeliveryConfigurationRepository
import com.spacetecsolutions.meatapp.core.domain.repository.FeatureManagementRepository
import com.spacetecsolutions.meatapp.core.model.DeliveryConfig
import com.spacetecsolutions.meatapp.core.model.DeliverySlot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeliverySettingsViewModel @Inject constructor(
    private val deliveryRepository: DeliveryConfigurationRepository,
    featureRepository: FeatureManagementRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(DeliverySettingsUiState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            featureRepository.featureConfig.collect { result ->
                val features = (result as? AppResult.Success)?.value
                val allowed = true
                val trackingAllowed = features?.realtimeTrackingAllowed ?: false
                mutableState.update { current ->
                    current.copy(
                        scheduledDeliveryAllowed = allowed,
                        realtimeTrackingAllowed = trackingAllowed,
                        config = current.config?.copy(
                            scheduledDeliveryEnabled = true,
                            realtimeTrackingEnabled = current.config.realtimeTrackingEnabled && trackingAllowed,
                        ),
                    )
                }
            }
        }
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        mutableState.update { it.copy(loading = true, error = null) }
        when (val result = deliveryRepository.get()) {
            is AppResult.Success -> show(result.value)
            is AppResult.Failure -> mutableState.update {
                it.copy(loading = false, error = result.error.text())
            }
        }
    }

    fun setRealtimeTracking(enabled: Boolean) = updateConfig {
        copy(realtimeTrackingEnabled = enabled && state.value.realtimeTrackingAllowed)
    }
    fun setCharge(value: String) = setMoney(value) { copy(deliveryCharge = it) }
    fun setThreshold(value: String) = setMoney(value) { copy(freeThreshold = it) }
    fun setMinimum(value: String) = setMoney(value) { copy(minimumOrder = it) }
    fun addSlot(start: Int, end: Int, active: Boolean) = updateConfig {
        copy(slots = slots + DeliverySlot(
            "slot-${System.currentTimeMillis()}", slotLabel(start, end), start, end, active,
        ))
    }
    fun updateSlot(id: String, start: Int, end: Int, active: Boolean) = updateConfig {
        copy(slots = slots.map { slot ->
            if (slot.id == id) slot.copy(
                label = slotLabel(start, end), startMinutes = start, endMinutes = end, active = active,
            ) else slot
        })
    }
    fun removeSlot(id: String) = updateConfig { copy(slots = slots.filterNot { it.id == id }) }
    fun toggleSlot(id: String, active: Boolean) = updateConfig {
        copy(slots = slots.map { if (it.id == id) it.copy(active = active) else it })
    }

    fun save() = viewModelScope.launch {
        val current = state.value
        val config = current.config ?: return@launch
        val charge = current.deliveryCharge.moneyMinorOrNull()
        val minimum = current.minimumOrder.moneyMinorOrNull()
        val threshold = current.freeThreshold.moneyMinorOrNull()
        if (charge == null || minimum == null || (current.freeThreshold.isNotBlank() && threshold == null)) {
            mutableState.update { it.copy(message = DeliverySettingsMessage("Enter valid non-negative amounts", false)) }
            return@launch
        }
        mutableState.update { it.copy(saving = true) }
        val request = config.copy(deliveryChargeMinor = charge, minimumOrderMinor = minimum,
            freeDeliveryThresholdMinor = threshold,
            normalDeliveryEnabled = false, scheduledDeliveryEnabled = true,
            realtimeTrackingEnabled = config.realtimeTrackingEnabled && current.realtimeTrackingAllowed)
        when (val result = deliveryRepository.update(request)) {
            is AppResult.Success -> {
                show(result.value)
                mutableState.update { it.copy(message = DeliverySettingsMessage("Delivery settings saved", true)) }
            }
            is AppResult.Failure -> mutableState.update {
                it.copy(saving = false, message = DeliverySettingsMessage(result.error.text(), false))
            }
        }
    }

    fun consumeMessage() = mutableState.update { it.copy(message = null) }
    private fun updateConfig(block: DeliveryConfig.() -> DeliveryConfig) =
        mutableState.update { it.copy(config = it.config?.block()) }
    private fun setMoney(value: String, block: DeliverySettingsUiState.(String) -> DeliverySettingsUiState) {
        if (value.length <= 10 && value.matches(Regex("\\d*(\\.\\d{0,2})?"))) mutableState.update { it.block(value) }
    }
    private fun show(config: DeliveryConfig) = mutableState.update {
        it.copy(loading = false, saving = false,
            config = config.copy(normalDeliveryEnabled = false, scheduledDeliveryEnabled = true),
            deliveryCharge = config.deliveryChargeMinor.asMoney(),
            freeThreshold = config.freeDeliveryThresholdMinor?.asMoney().orEmpty(),
            minimumOrder = config.minimumOrderMinor.asMoney(), error = null)
    }
}

private fun slotLabel(start: Int, end: Int) = "${start.clockLabel()} - ${end.clockLabel()}"
private fun Int.clockLabel(): String {
    val hour = (this / 60) % 24; val minute = this % 60
    val displayHour = if (hour % 12 == 0) 12 else hour % 12
    return "%d:%02d %s".format(displayHour, minute, if (hour < 12) "AM" else "PM")
}

private fun Long.asMoney() = if (this % 100 == 0L) (this / 100).toString() else "%.2f".format(this / 100.0)
private fun AppError.text() = when (this) {
    AppError.Forbidden, AppError.Unauthorized -> "Only an authorized Admin can configure delivery"
    AppError.Network, AppError.Offline, AppError.Timeout -> "Check your connection and try again"
    is AppError.Validation -> message
    else -> "Unable to update delivery settings"
}
