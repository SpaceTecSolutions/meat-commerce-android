package com.spacetecsolutions.meatapp.core.domain.repository

import com.spacetecsolutions.meatapp.core.common.result.AppResult

interface MediaRepository {
    suspend fun upload(path: String, bytes: ByteArray, contentType: String): AppResult<String>
    suspend fun delete(path: String): AppResult<Unit>
}
