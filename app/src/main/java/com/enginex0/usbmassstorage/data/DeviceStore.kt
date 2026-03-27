package com.enginex0.usbmassstorage.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.enginex0.usbmassstorage.daemon.DeviceInfo
import com.enginex0.usbmassstorage.daemon.DeviceType

private const val TAG = "UsbMsStore"
private const val PREFS_NAME = "device_store"
private const val KEY_COUNT = "device_count"

class DeviceStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): List<DeviceInfo> {
        val count = prefs.getInt(KEY_COUNT, 0)
        if (count == 0) return emptyList()

        val devices = mutableListOf<DeviceInfo>()
        for (i in 0 until count) {
            val uri = prefs.getString("device_${i}_uri", null) ?: break
            val typeName = prefs.getString("device_${i}_type", null) ?: break
            val type = try {
                DeviceType.valueOf(typeName)
            } catch (_: IllegalArgumentException) {
                Log.w(TAG, "load: unknown type $typeName at index $i, skipping")
                continue
            }
            devices.add(DeviceInfo(Uri.parse(uri), type))
        }
        Log.d(TAG, "load: ${devices.size} saved devices")
        return devices
    }

    fun save(devices: List<DeviceInfo>) {
        prefs.edit().apply {
            clear()
            putInt(KEY_COUNT, devices.size)
            devices.forEachIndexed { i, device ->
                putString("device_${i}_uri", device.uri.toString())
                putString("device_${i}_type", device.type.name)
            }
            apply()
        }
        Log.d(TAG, "save: ${devices.size} devices persisted")
    }

    fun clear() {
        prefs.edit().clear().apply()
        Log.d(TAG, "clear: all devices removed")
    }
}
