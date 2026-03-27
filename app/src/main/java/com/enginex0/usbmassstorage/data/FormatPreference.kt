package com.enginex0.usbmassstorage.data

import android.content.Context

enum class FileSystemType {
    FAT32,
    EXFAT,
    NONE
}

object FormatPreference {
    private const val PREFS = "format_prefs"
    private const val KEY = "fs_type"

    fun load(context: Context): FileSystemType {
        val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return FileSystemType.FAT32
        return runCatching { FileSystemType.valueOf(name) }.getOrDefault(FileSystemType.FAT32)
    }

    fun save(context: Context, type: FileSystemType) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, type.name).apply()
    }
}
