package com.spacetecsolutions.meatapp.feature.superadmin.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetecsolutions.meatapp.core.common.result.AppError
import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.domain.repository.AdminManagementRepository
import com.spacetecsolutions.meatapp.core.model.CreateAdminRequest
import com.spacetecsolutions.meatapp.core.model.UpdateAdminRequest
import com.spacetecsolutions.meatapp.core.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminManagementViewModel @Inject constructor(
    private val repository: AdminManagementRepository,
    private val validator: AdminValidator,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AdminManagementUiState())
    val state = mutableState.asStateFlow()

    init { loadAdmins() }

    fun updateQuery(value: String) = mutableState.update { it.copy(query = value) }

    fun loadAdmins() = viewModelScope.launch {
        mutableState.update {
            it.copy(loading = it.admins.isEmpty(), refreshing = it.admins.isNotEmpty(), error = null)
        }
        when (val result = repository.getAdmins()) {
            is AppResult.Success -> mutableState.update {
                it.copy(
                    admins = result.value.sortedBy(User::displayName),
                    loading = false,
                    refreshing = false,
                )
            }
            is AppResult.Failure -> mutableState.update {
                it.copy(loading = false, refreshing = false, error = result.error.message())
            }
        }
    }

    fun openCreate() = mutableState.update { it.copy(form = AdminFormState()) }
    fun openEdit(admin: User) = mutableState.update {
        it.copy(form = AdminFormState(admin.id, admin.displayName, admin.mobileNumber))
    }
    fun closeForm() = mutableState.update { if (it.submitting) it else it.copy(form = null) }
    fun updateName(value: String) = updateForm { copy(name = value, nameError = null) }
    fun updateMobile(value: String) = updateForm { copy(mobile = value, mobileError = null) }
    fun updatePassword(value: String) = updateForm { copy(password = value, passwordError = null) }

    fun saveAdmin() {
        val form = state.value.form ?: return
        val name = validator.name(form.name)
        val mobile = validator.mobile(form.mobile)
        val password = if (form.editing) null else validator.password(form.password)
        if (name is AdminValidation.Invalid || mobile is AdminValidation.Invalid ||
            password is AdminValidation.Invalid
        ) {
            mutableState.update {
                it.copy(form = form.copy(
                    nameError = (name as? AdminValidation.Invalid)?.message,
                    mobileError = (mobile as? AdminValidation.Invalid)?.message,
                    passwordError = (password as? AdminValidation.Invalid)?.message,
                ))
            }
            return
        }
        viewModelScope.launch {
            mutableState.update { it.copy(submitting = true) }
            val result = if (form.editing) {
                repository.updateAdmin(
                    UpdateAdminRequest(
                        form.userId!!,
                        (name as AdminValidation.Valid).value,
                        (mobile as AdminValidation.Valid).value,
                    ),
                )
            } else {
                repository.createAdmin(
                    CreateAdminRequest(
                        (name as AdminValidation.Valid).value,
                        (mobile as AdminValidation.Valid).value,
                        (password as AdminValidation.Valid).value,
                    ),
                )
            }
            handleMutation(result, if (form.editing) "Admin updated" else "Admin created")
        }
    }

    fun requestStatusChange(admin: User) = mutableState.update {
        it.copy(confirmation = AdminStatusConfirmation(admin, activate = !admin.active))
    }
    fun dismissConfirmation() = mutableState.update { if (it.submitting) it else it.copy(confirmation = null) }
    fun confirmStatusChange() {
        val confirmation = state.value.confirmation ?: return
        viewModelScope.launch {
            mutableState.update { it.copy(submitting = true) }
            handleMutation(
                repository.setAdminActive(confirmation.admin.id, confirmation.activate),
                if (confirmation.activate) "Admin activated" else "Admin deactivated",
            )
        }
    }

    fun consumeMessage() = mutableState.update { it.copy(message = null) }

    private fun updateForm(transform: AdminFormState.() -> AdminFormState) =
        mutableState.update { state -> state.form?.let { state.copy(form = it.transform()) } ?: state }

    private fun handleMutation(result: AppResult<User>, successMessage: String) {
        when (result) {
            is AppResult.Success -> mutableState.update { state ->
                val updated = (state.admins.filterNot { it.id == result.value.id } + result.value)
                    .sortedBy(User::displayName)
                state.copy(
                    admins = updated,
                    form = null,
                    confirmation = null,
                    submitting = false,
                    message = AdminMessage(successMessage, true),
                )
            }
            is AppResult.Failure -> mutableState.update {
                it.copy(submitting = false, message = AdminMessage(result.error.message(), false))
            }
        }
    }
}

private fun AppError.message(): String = when (this) {
    AppError.DuplicateAccount -> "An account already exists for this mobile number"
    AppError.Forbidden, AppError.Unauthorized -> "Only an authorized Super Admin can manage Admins"
    AppError.Network, AppError.Offline, AppError.Timeout -> "Check your connection and try again"
    is AppError.Validation -> message
    else -> "Unable to complete the request. Try again"
}
