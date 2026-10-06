package com.spacetecsolutions.meatapp.core.navigation

import com.spacetecsolutions.meatapp.core.model.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoleNavigationRegistryTest {
    @Test
    fun `each role exposes the required tabs in order`() {
        assertEquals(listOf("Dashboard", "Admins", "Reports", "More"), labels(UserRole.SUPER_ADMIN))
        assertEquals(listOf("Dashboard", "Orders", "Products", "More"), labels(UserRole.ADMIN))
        assertEquals(listOf("Home", "Orders", "History", "Profile"), labels(UserRole.DELIVERY))
        assertEquals(listOf("Home", "Categories", "Cart", "Orders", "Profile"), labels(UserRole.CUSTOMER))
    }

    @Test
    fun `every cross-role destination is denied`() {
        UserRole.entries.forEach { authenticatedRole ->
            UserRole.entries.filterNot { it == authenticatedRole }.forEach { otherRole ->
                RoleNavigationRegistry.graphDestinationsFor(otherRole).forEach { destination ->
                    assertTrue(
                        RoleNavigationRegistry.validateRoute(authenticatedRole, destination.route)
                            is NavigationDecision.Denied,
                    )
                }
            }
        }
    }

    @Test
    fun `trusted same-role deep link is allowed`() {
        val result = RoleNavigationRegistry.validateDeepLink(
            UserRole.CUSTOMER,
            "meatbush://app/customer/orders",
            "meatbush",
        )

        assertTrue(result is NavigationDecision.Allowed)
        assertEquals("Orders", (result as NavigationDecision.Allowed).destination.label)
    }

    @Test
    fun `cross-role unknown and untrusted deep links are denied`() {
        val links = listOf(
            "meatbush://app/admin/orders",
            "meatbush://app/customer/not-real",
            "other://app/customer/orders",
            "meatbush://evil/customer/orders",
            "not a uri",
        )

        links.forEach { link ->
            assertTrue(
                RoleNavigationRegistry.validateDeepLink(UserRole.CUSTOMER, link, "meatbush")
                    is NavigationDecision.Denied,
            )
        }
    }

    @Test
    fun `admin cannot access Super Admin feature management`() {
        assertTrue(
            RoleNavigationRegistry.validateRoute(
                UserRole.ADMIN,
                SuperAdminRoutes.FEATURE_MANAGEMENT,
            ) is NavigationDecision.Denied,
        )
    }

    @Test
    fun `admin can access home banner management`() {
        val result = RoleNavigationRegistry.validateRoute(UserRole.ADMIN, AdminRoutes.BANNERS)

        assertTrue(result is NavigationDecision.Allowed)
        assertEquals("Home Banners", (result as NavigationDecision.Allowed).destination.label)
    }

    @Test
    fun `delivery cannot access business management destinations`() {
        listOf(
            AdminRoutes.DASHBOARD,
            AdminRoutes.PRODUCTS,
            AdminRoutes.DELIVERY_SETTINGS,
            AdminRoutes.PAYMENT_SETTINGS,
            SuperAdminRoutes.REPORTS,
            SuperAdminRoutes.FEATURE_MANAGEMENT,
        ).forEach { route ->
            assertTrue(RoleNavigationRegistry.validateRoute(UserRole.DELIVERY, route)
                is NavigationDecision.Denied)
        }
    }

    @Test
    fun `customer cannot access Admin category management`() {
        assertTrue(
            RoleNavigationRegistry.validateRoute(
                UserRole.CUSTOMER,
                AdminRoutes.CATEGORIES,
            ) is NavigationDecision.Denied,
        )
    }

    @Test
    fun `non customer cannot access customer addresses`() {
        assertTrue(RoleNavigationRegistry.validateRoute(
            UserRole.ADMIN, CustomerRoutes.ADDRESSES,
        ) is NavigationDecision.Denied)
    }

    private fun labels(role: UserRole) =
        RoleNavigationRegistry.destinationsFor(role).map(RoleDestination::label)
}
