package com.spacetecsolutions.meatapp.feature.customerhome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.ShopSupportRepository
import com.spacetecsolutions.meatapp.core.model.ShopSupport
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CustomerSupportState(
    val loading: Boolean = true,
    val support: ShopSupport? = null,
    val error: String? = null,
)

@HiltViewModel
class CustomerSupportViewModel @Inject constructor(
    private val repository: ShopSupportRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(CustomerSupportState())
    val state = mutableState.asStateFlow()

    init { refresh() }

    fun refresh() = viewModelScope.launch {
        mutableState.update { it.copy(loading = true, error = null) }
        when (val result = repository.getSupport()) {
            is AppResult.Success -> mutableState.update { it.copy(loading = false, support = result.value) }
            is AppResult.Failure -> mutableState.update {
                it.copy(loading = false, error = result.error.supportText())
            }
        }
    }
}

private fun AppError.supportText() = when (this) {
    AppError.Network, AppError.Offline, AppError.Timeout -> "Check your connection and try again"
    AppError.Unauthorized, AppError.Forbidden -> "Support details are unavailable for this account"
    is AppError.Validation -> message
    else -> "Unable to load support details"
}
