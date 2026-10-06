package com.spacetecsolutions.meatapp.core.data.firebase.realtime

import com.google.firebase.database.FirebaseDatabase
import org.junit.Assert.assertEquals
import org.junit.Test
import javax.inject.Provider

class RealtimeTrackingDatabaseAccessorTest {
    @Test
    fun `constructing accessor does not initialize realtime database`() {
        var providerCalls = 0
        val provider = Provider<FirebaseDatabase> {
            providerCalls += 1
            error("Database should not be requested during construction")
        }

        RealtimeTrackingDatabaseAccessor(provider)

        assertEquals(0, providerCalls)
    }
}
