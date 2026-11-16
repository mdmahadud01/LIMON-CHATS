package com.example.data

import android.content.Context
import java.util.UUID

object DeviceIdentity {
    private const val PREFS_NAME = "limon_device_prefs"
    private const val KEY_DEVICE_ID = "device_id"

    fun getDeviceId(context: Context): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val generated = "android_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        prefs.edit().putString(KEY_DEVICE_ID, generated).apply()
        return generated
    }
}
