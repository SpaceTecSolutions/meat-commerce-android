package com.spacetecsolutions.meatapp.feature.admin.promotion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.domain.repository.PromotionRepository
import com.spacetecsolutions.meatapp.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*
import javax.inject.Inject

data class PromotionForm(
    val id: String = "new", val revision: Long = 0, val name: String = "", val code: String = "",
    val kind: PromotionKind = PromotionKind.OFFER, val type: DiscountType = DiscountType.PERCENTAGE,
    val value: String = "", val minimum: String = "", val maximum: String = "",
    val validFrom: String = "", val validUntil: String = "", val active: Boolean = true,
)
data class PromotionManagementUiState(
    val loading: Boolean = true, val promotions: List<Promotion> = emptyList(),
    val offersAllowed: Boolean = false, val couponsAllowed: Boolean = false,
    val form: PromotionForm? = null, val busyId: String? = null,
    val error: String? = null, val message: PromotionMessage? = null,
)
data class PromotionMessage(val text: String, val success: Boolean)

@HiltViewModel
class PromotionManagementViewModel @Inject constructor(
    private val repository: PromotionRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(PromotionManagementUiState())
    val state = mutableState.asStateFlow()

    fun load(offersAllowed: Boolean, couponsAllowed: Boolean) {
        if (!offersAllowed && !couponsAllowed) {
            mutableState.value = PromotionManagementUiState(loading = false)
            return
        }
        val changed = state.value.offersAllowed != offersAllowed || state.value.couponsAllowed != couponsAllowed
        mutableState.update { it.copy(offersAllowed = offersAllowed, couponsAllowed = couponsAllowed) }
        if (changed || state.value.loading) refresh()
    }

    fun refresh() = viewModelScope.launch {
        if (!state.value.offersAllowed && !state.value.couponsAllowed) return@launch
        mutableState.update { it.copy(loading = true, error = null) }
        when (val result = repository.getAdminPromotions()) {
            is AppResult.Success -> mutableState.update { it.copy(loading = false, promotions = result.value) }
            is AppResult.Failure -> mutableState.update { it.copy(loading = false, error = result.error.text()) }
        }
    }

    fun edit(promotion: Promotion?) = mutableState.update { state -> state.copy(form = promotion?.toForm() ?: PromotionForm(
        kind = if (state.offersAllowed) PromotionKind.OFFER else PromotionKind.COUPON,
        validFrom = LocalDate.now().toString(), validUntil = LocalDate.now().plusDays(7).toString(),
    )) }
    fun update(form: PromotionForm) = mutableState.update { it.copy(form = form) }
    fun dismissForm() = mutableState.update { it.copy(form = null) }

    fun save() {
        val promotion = state.value.form?.toPromotionOrNull() ?: run {
            mutableState.update { it.copy(message = PromotionMessage("Check all promotion fields", false)) }; return
        }
        if ((promotion.kind == PromotionKind.OFFER && !state.value.offersAllowed) ||
            (promotion.kind == PromotionKind.COUPON && !state.value.couponsAllowed)) return
        mutableState.update { it.copy(form = null, busyId = promotion.id) }
        viewModelScope.launch {
            when (val result = repository.savePromotion(promotion)) {
                is AppResult.Success -> mutableState.update { current -> current.copy(
                    busyId = null,
                    promotions = current.promotions.filterNot { it.id == result.value.id } + result.value,
                    message = PromotionMessage("Promotion saved", true),
                ) }
                is AppResult.Failure -> fail(result.error.text())
            }
        }
    }

    fun toggle(promotion: Promotion) {
        if (state.value.busyId != null) return
        mutableState.update { it.copy(busyId = promotion.id) }
        viewModelScope.launch {
            when (val result = repository.setPromotionActive(promotion.id, !promotion.active, promotion.revision)) {
                is AppResult.Success -> mutableState.update { current -> current.copy(
                    busyId = null, promotions = current.promotions.map { if (it.id == promotion.id) result.value else it },
                    message = PromotionMessage(if (result.value.active) "Promotion activated" else "Promotion deactivated", true),
                ) }
                is AppResult.Failure -> fail(result.error.text())
            }
        }
    }

    fun consumeMessage() = mutableState.update { it.copy(message = null) }
    private fun fail(text: String) = mutableState.update { it.copy(busyId = null, message = PromotionMessage(text, false)) }
}

private fun Promotion.toForm() = PromotionForm(id, revision, name, code.orEmpty(), kind, discountType,
    discountValue.toString(), (minimumOrderMinor / 100.0).clean(), maximumDiscountMinor?.let { (it / 100.0).clean() }.orEmpty(),
    Instant.ofEpochMilli(validFromEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate().toString(),
    Instant.ofEpochMilli(validUntilEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate().toString(), active)
private fun PromotionForm.toPromotionOrNull(): Promotion? = runCatching {
    val discountValue = if (type == DiscountType.FIXED) {
        value.toBigDecimal().movePointRight(2).longValueExact()
    } else {
        value.toLong()
    }
    val minimumMinor = minimum.ifBlank { "0" }.toBigDecimal().movePointRight(2).longValueExact()
    val maximumMinor = maximum.takeIf(String::isNotBlank)?.toBigDecimal()?.movePointRight(2)?.longValueExact()
    val zone = ZoneId.systemDefault()
    Promotion(id, name.trim(), code.trim().uppercase().takeIf { kind == PromotionKind.COUPON }, kind, type,
        discountValue,
        minimumMinor, maximumMinor, LocalDate.parse(validFrom).atStartOfDay(zone).toInstant().toEpochMilli(),
        LocalDate.parse(validUntil).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1, active, revision)
}.getOrNull()
private fun Double.clean() = if (this % 1.0 == 0.0) toLong().toString() else toString()
private fun AppError.text() = when (this) {
    AppError.Forbidden, AppError.Unauthorized -> "This promotion is not authorized"
    AppError.Network, AppError.Offline, AppError.Timeout -> "Check your connection and try again"
    is AppError.Validation -> message
    else -> "Unable to update promotions"
}
