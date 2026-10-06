package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.AdminCustomerDetails
import com.spacetecsolutions.meatapp.core.model.AdminCustomerSummary

interface AdminCustomerRepository {
    suspend fun getCustomers(query: String = ""): AppResult<List<AdminCustomerSummary>>
    suspend fun getCustomerDetails(customerId: String): AppResult<AdminCustomerDetails>
    suspend fun setCustomerActive(
        customerId: String, active: Boolean, expectedRevision: Long,
    ): AppResult<AdminCustomerSummary>
}
