package com.spacetecsolutions.meatapp.core.designsystem.component

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSnackbarManagerTest {
    @Test
    fun `messages are delivered in queue order`() = runTest {
        val manager = AppSnackbarManager()
        val first = AppSnackbarMessage("Saved", AppSnackbarType.SUCCESS)
        val second = AppSnackbarMessage("Offline", AppSnackbarType.WARNING)

        manager.show(first)
        manager.show(second)

        assertEquals(listOf(first, second), manager.messages.take(2).toList())
    }
}
