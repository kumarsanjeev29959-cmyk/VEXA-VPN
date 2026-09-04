package com.vexa.vpn

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class DeviceIdentityTest {
    @Test
    fun deviceIdIsStableForSameStorage() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val first = DeviceIdentity(context).id
        val second = DeviceIdentity(context).id
        assertEquals(first, second)
    }

    @Test
    fun deviceIdLooksLikeUuid() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val id = DeviceIdentity(context).id
        assertNotEquals("", id)
        assertEquals(36, id.length)
    }
}
