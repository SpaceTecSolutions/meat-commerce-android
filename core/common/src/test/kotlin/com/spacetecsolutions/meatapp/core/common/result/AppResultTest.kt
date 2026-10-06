package com.spacetecsolutions.meatapp.core.common.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppResultTest {
    @Test
    fun `map transforms success without changing failures`() {
        assertEquals(AppResult.Success(4), AppResult.Success(2).map { it * 2 })
        assertEquals(
            AppResult.Failure(AppError.Offline),
            AppResult.Failure(AppError.Offline).map { "unused" },
        )
    }

    @Test
    fun `runAppCatching maps thrown exceptions to unknown error`() {
        val result = runAppCatching { error("broken") }

        assertTrue(result is AppResult.Failure && result.error is AppError.Unknown)
    }
}
