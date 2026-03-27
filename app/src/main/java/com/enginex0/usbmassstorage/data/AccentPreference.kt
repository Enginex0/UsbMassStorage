package com.enginex0.usbmassstorage.data

import android.content.Context

enum class AccentColor {
    SYSTEM_DEFAULT,
    ALMOST_BLACK,
    WHITE
}

object AccentPreference {
    private const val PREFS = "accent_prefs"
    private const val KEY = "accent_color"

    fun load(context: Context): AccentColor {
        val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return AccentColor.SYSTEM_DEFAULT
        return runCatching { AccentColor.valueOf(name) }.getOrDefault(AccentColor.SYSTEM_DEFAULT)
    }

    fun save(context: Context, accent: AccentColor) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, accent.name).apply()
    }
}
