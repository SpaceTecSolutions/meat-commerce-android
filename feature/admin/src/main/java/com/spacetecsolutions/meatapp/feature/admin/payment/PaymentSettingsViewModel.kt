package com.spacetecsolutions.meatapp.feature.admin.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.FeatureManagementRepository
import com.spacetecsolutions.meatapp.core.domain.repository.PaymentConfigurationRepository
import com.spacetecsolutions.meatapp.core.model.PaymentConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PaymentSettingsViewModel @Inject constructor(
    private val paymentRepository: PaymentConfigurationRepository,
    featureRepository: FeatureManagementRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(PaymentSettingsUiState())
    val state = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            featureRepository.featureConfig.collect { result ->
                val features = (result as? AppResult.Success)?.value
                mutableState.update { it.copy(permissions = PaymentPermissions(
                    cod = true,
                    razorpay = features?.razorpayAllowed == true,
                    upi = features?.upiAllowed == true,
                )) }
            }
        }
        refresh()
    }

    fun refresh() = viewModelScope.launch {
        mutableState.update { it.copy(loading = true, error = null) }
        when (val result = paymentRepository.get()) {
            is AppResult.Success -> mutableState.update {
                it.copy(loading = false, config = result.value, error = null)
            }
            is AppResult.Failure -> mutableState.update {
                it.copy(loading = false, error = result.error.text())
            }
        }
    }

    fun setUpi(enabled: Boolean) = updateConfig { copy(upiEnabled = enabled && state.value.permissions.upi) }
    fun setRazorpay(enabled: Boolean) = updateConfig {
        copy(razorpayEnabled = enabled && razorpayConfigured && state.value.permissions.razorpay)
    }

    fun verifyRazorpay() = viewModelScope.launch {
        mutableState.update { it.copy(saving = true) }
        when (val result = paymentRepository.verifyRazorpay()) {
            is AppResult.Success -> {
                mutableState.update { it.copy(saving = false,
                    message = PaymentSettingsMessage("Razorpay backend verified. You can enable it now.", true)) }
                refresh()
            }
            is AppResult.Failure -> mutableState.update { it.copy(saving = false,
                message = PaymentSettingsMessage(result.error.text(), false)) }
        }
    }

    fun save() = viewModelScope.launch {
        val current = state.value
        val config = current.config ?: return@launch
        val request = config.copy(
            codEnabled = true,
            razorpayEnabled = config.razorpayEnabled && config.razorpayConfigured && current.permissions.razorpay,
            upiEnabled = config.upiEnabled && current.permissions.upi,
        )
        mutableState.update { it.copy(saving = true) }
        when (val result = paymentRepository.update(request)) {
            is AppResult.Success -> mutableState.update {
                it.copy(saving = false, config = result.value,
                    message = PaymentSettingsMessage("Payment settings saved", true))
            }
            is AppResult.Failure -> mutableState.update {
                it.copy(saving = false, message = PaymentSettingsMessage(result.error.text(), false))
            }
        }
    }

    fun consumeMessage() = mutableState.update { it.copy(message = null) }
    private fun updateConfig(block: PaymentConfig.() -> PaymentConfig) =
        mutableState.update { it.copy(config = it.config?.block()) }
}

private fun AppError.text() = when (this) {
    AppError.Forbidden, AppError.Unauthorized -> "Only an authorized Admin can configure payments"
    AppError.Network, AppError.Offline, AppError.Timeout -> "Check your connection and try again"
    is AppError.Validation -> message
    else -> "Unable to update payment settings"
}
