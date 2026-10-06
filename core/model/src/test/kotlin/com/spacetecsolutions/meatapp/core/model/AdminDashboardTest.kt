package com.spacetecsolutions.meatapp.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AdminDashboardTest {
    @Test
    fun `dashboard defaults preserve requested period`() {
        val dashboard = AdminDashboard(
        adminName = "Akash",
        currencyCode = "INR",
        todayRevenueMinor = 0,
        todayOrders = 0,
        pendingOrders = 0,
        deliveredOrders = 0,
        unreadNotificationCount = 0,
        period = AdminDashboardPeriod.THIS_WEEK,
        periodLabel = "This Week",
        )
        assertEquals(AdminDashboardPeriod.THIS_WEEK, dashboard.period)
        assertEquals("Akash", dashboard.adminName)
    }
}
