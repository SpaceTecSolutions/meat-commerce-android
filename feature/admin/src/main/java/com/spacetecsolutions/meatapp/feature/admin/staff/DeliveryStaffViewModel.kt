package com.spacetecsolutions.meatapp.feature.admin.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.*
import com.spacetecsolutions.meatapp.core.domain.repository.DeliveryStaffRepository
import com.spacetecsolutions.meatapp.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DeliveryStaffViewModel @Inject constructor(
    private val repository: DeliveryStaffRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(DeliveryStaffUiState())
    val state = mutableState.asStateFlow()

    init { load() }

    fun load() = viewModelScope.launch {
        mutableState.update { it.copy(loading = it.staff.isEmpty(), refreshing = it.staff.isNotEmpty(), error = null) }
        when (val result = repository.getStaff()) {
            is AppResult.Success -> mutableState.update {
                it.copy(staff = result.value.sortedBy(User::displayName), loading = false, refreshing = false)
            }
            is AppResult.Failure -> mutableState.update {
                it.copy(loading = false, refreshing = false, error = result.error.message())
            }
        }
    }

    fun create() = mutableState.update { it.copy(form = StaffForm()) }
    fun edit(user: User) = mutableState.update {
        it.copy(form = StaffForm(user.id, user.displayName, user.mobileNumber))
    }
    fun closeForm() = mutableState.update { if (it.submitting) it else it.copy(form = null) }
    fun name(value: String) = updateForm { copy(name = value, error = null) }
    fun mobile(value: String) = updateForm { copy(mobile = value, error = null) }
    fun password(value: String) = updateForm { copy(password = value, error = null) }

    fun save() {
        val form = state.value.form ?: return
        val mobile = form.mobile.filter(Char::isDigit)
        val error = when {
            form.name.trim().length < 2 -> "Enter a valid name"
            mobile.length != 10 || mobile.firstOrNull() !in '6'..'9' -> "Enter a valid Indian mobile number"
            !form.editing && form.password.length < 8 -> "Password must be at least 8 characters"
            else -> null
        }
        if (error != null) { mutableState.update { it.copy(form = form.copy(error = error)) }; return }
        viewModelScope.launch {
            mutableState.update { it.copy(submitting = true) }
            val result = if (form.editing) repository.update(UpdateDeliveryStaffRequest(
                form.userId!!, form.name.trim(), "+91$mobile",
            )) else repository.create(CreateDeliveryStaffRequest(
                form.name.trim(), "+91$mobile", form.password,
            ))
            mutation(result, if (form.editing) "Delivery user updated" else "Delivery user created")
        }
    }

    fun requestStatus(user: User?) = mutableState.update { it.copy(statusChange = user) }
    fun confirmStatus() {
        val user = state.value.statusChange ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(submitting = true) }
            mutation(repository.setActive(user.id, !user.active),
                if (user.active) "Delivery user deactivated" else "Delivery user activated")
        }
    }
    fun consumeMessage() = mutableState.update { it.copy(message = null) }

    private fun updateForm(change: StaffForm.() -> StaffForm) = mutableState.update { state ->
        state.form?.let { state.copy(form = it.change()) } ?: state
    }
    private fun mutation(result: AppResult<User>, message: String) = when (result) {
        is AppResult.Success -> mutableState.update { state -> state.copy(
            staff = (state.staff.filterNot { it.id == result.value.id } + result.value).sortedBy(User::displayName),
            form = null, statusChange = null, submitting = false, message = StaffMessage(message, true),
        ) }
        is AppResult.Failure -> mutableState.update {
            it.copy(submitting = false, message = StaffMessage(result.error.message(), false))
        }
    }
}

private fun AppError.message() = when (this) {
    AppError.DuplicateAccount -> "An account already exists for this mobile number"
    AppError.Forbidden, AppError.Unauthorized -> "Delivery staff management is unavailable"
    AppError.Network, AppError.Offline, AppError.Timeout -> "Check your connection and try again"
    is AppError.Validation -> message
    else -> "Unable to update delivery staff"
}
