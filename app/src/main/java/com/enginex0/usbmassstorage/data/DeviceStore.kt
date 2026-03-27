package com.enginex0.usbmassstorage.data

import android.content.Context
import android.net.Uri
import android.util.Log
import com.enginex0.usbmassstorage.daemon.DeviceInfo
import com.enginex0.usbmassstorage.daemon.DeviceType
import com.topjohnwu.superuser.Shell

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
            val fs = prefs.getString("device_${i}_fs", null)
            devices.add(DeviceInfo(Uri.parse(uri), type, fs))
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
                device.fsType?.let { putString("device_${i}_fs", it) }
            }
            apply()
        }
        Log.d(TAG, "save: ${devices.size} devices persisted")
        writeAutomountConfig(devices)
    }

    fun clear() {
        prefs.edit().clear().apply()
        Log.d(TAG, "clear: all devices removed")
        Shell.cmd("rm -f $AUTOMOUNT_PATH").exec()
    }

    private fun writeAutomountConfig(devices: List<DeviceInfo>) {
        val lines = devices.mapNotNull { device ->
            val path = resolveToLowerFs(device.uri) ?: return@mapNotNull null
            val type = when (device.type) {
                DeviceType.CDROM -> "cdrom"
                DeviceType.DISK_RO -> "disk-ro"
                DeviceType.DISK_RW -> "disk-rw"
            }
            "$path|$type"
        }

        if (lines.isEmpty()) {
            Shell.cmd("rm -f $AUTOMOUNT_PATH").exec()
            return
        }

        val cmds = mutableListOf(": > $AUTOMOUNT_PATH")
        lines.forEach { cmds.add("echo '$it' >> $AUTOMOUNT_PATH") }
        Shell.cmd(*cmds.toTypedArray()).exec()
        Log.d(TAG, "writeAutomountConfig: ${lines.size} entries")
    }

    private fun resolveToLowerFs(uri: Uri): String? {
        if (uri.scheme != "file") return null
        val path = uri.path ?: return null
        if (path.startsWith("/storage/emulated/")) {
            return "/data/media/${path.removePrefix("/storage/emulated/")}"
        }
        return path
    }

    companion object {
        private const val AUTOMOUNT_PATH = "/data/adb/usbmassstorage/automount.conf"
    }
}
