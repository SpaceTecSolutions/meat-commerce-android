package com.spacetecsolutions.meatapp.feature.superadmin.productlimit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.FeatureManagementRepository
import com.spacetecsolutions.meatapp.core.domain.repository.ProductLimitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductLimitViewModel @Inject constructor(
    private val repository: ProductLimitRepository,
    private val featureRepository: FeatureManagementRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ProductLimitUiState())
    val state = mutableState.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        mutableState.update { it.copy(loading = it.status == null, error = null) }
        when (val result = repository.getStatus()) {
            is AppResult.Success -> mutableState.update {
                it.copy(
                    loading = false,
                    status = result.value,
                    limitText = result.value.maxProducts.toString(),
                    limitError = null,
                )
            }
            is AppResult.Failure -> mutableState.update {
                it.copy(loading = false, error = result.error.message())
            }
        }
    }

    fun updateLimit(value: String) {
        if (value.all(Char::isDigit) && value.length <= 6) {
            mutableState.update { state ->
                state.copy(
                    limitText = value,
                    limitError = ProductLimitValidator.validate(
                        value.toIntOrNull(),
                        state.status?.countedProducts ?: 0,
                    ),
                )
            }
        }
    }

    fun save() {
        val current = state.value
        val status = current.status ?: return
        val value = current.limitText.toIntOrNull()
        val validation = ProductLimitValidator.validate(value, status.countedProducts)
        if (validation != null) {
            mutableState.update { it.copy(limitError = validation) }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(saving = true) }
            when (val result = repository.updateLimit(value!!, status.revision)) {
                is AppResult.Success -> {
                    mutableState.update {
                        it.copy(
                            saving = false,
                            status = result.value,
                            limitText = result.value.maxProducts.toString(),
                            limitError = null,
                            message = ProductLimitMessage("Product limit updated", true),
                        )
                    }
                    featureRepository.refresh()
                }
                is AppResult.Failure -> mutableState.update {
                    it.copy(
                        saving = false,
                        limitError = (result.error as? AppError.Validation)
                            ?.takeIf { error -> error.field == "maxProducts" }?.message,
                        message = ProductLimitMessage(result.error.message(), false),
                    )
                }
            }
        }
    }

    fun consumeMessage() = mutableState.update { it.copy(message = null) }
}

private fun AppError.message(): String = when (this) {
    AppError.Forbidden, AppError.Unauthorized -> "Only an authorized Super Admin can change the limit"
    AppError.Network, AppError.Offline, AppError.Timeout -> "Check your connection and try again"
    is AppError.Validation -> message
    else -> "Unable to update the product limit. Try again"
}
