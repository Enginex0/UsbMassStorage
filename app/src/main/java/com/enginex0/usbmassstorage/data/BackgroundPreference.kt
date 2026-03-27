package com.enginex0.usbmassstorage.data

import android.content.Context

object BackgroundPreference {
    private const val PREFS = "bg_prefs"
    private const val KEY = "bg_opacity"
    private const val DEFAULT = 0.10f

    fun load(context: Context): Float =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getFloat(KEY, DEFAULT)

    fun save(context: Context, opacity: Float) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putFloat(KEY, opacity).apply()
    }
}
