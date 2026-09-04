package com.vexa.vpn

import android.content.Context
import java.util.UUID

/** Stable anonymous device identifier. No account or personal identity is required. */
class DeviceIdentity(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val id: String
        get() = preferences.getString(KEY_DEVICE_ID, null) ?: createId()

    private fun createId(): String {
        val value = UUID.randomUUID().toString()
        preferences.edit().putString(KEY_DEVICE_ID, value).apply()
        return value
    }

    private companion object {
        const val PREFS = "vexa_device"
        const val KEY_DEVICE_ID = "device_id"
    }
}
