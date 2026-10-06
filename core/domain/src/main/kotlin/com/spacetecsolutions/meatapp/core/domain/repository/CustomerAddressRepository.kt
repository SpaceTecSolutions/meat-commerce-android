package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult
import com.spacetecsolutions.meatapp.core.model.CustomerAddress
import com.spacetecsolutions.meatapp.core.model.CustomerAddressInput

interface CustomerAddressRepository {
    suspend fun getAddresses(): AppResult<List<CustomerAddress>>
    suspend fun create(input: CustomerAddressInput): AppResult<List<CustomerAddress>>
    suspend fun update(input: CustomerAddressInput): AppResult<List<CustomerAddress>>
    suspend fun delete(addressId: String, expectedRevision: Long): AppResult<List<CustomerAddress>>
    suspend fun setDefault(addressId: String, expectedRevision: Long): AppResult<List<CustomerAddress>>
}
