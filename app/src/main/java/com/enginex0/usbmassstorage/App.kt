package com.enginex0.usbmassstorage

import android.app.Application
import android.util.Log
import com.google.android.material.color.DynamicColors
import com.topjohnwu.superuser.Shell
import java.io.File

class App : Application() {

    companion object {
        private const val TAG = "UsbMS"

        init {
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_REDIRECT_STDERR)
                    .setTimeout(10)
            )
            Log.d(TAG, "Shell builder initialized")
        }
    }

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        installCrashHandler()
    }

    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val logFile = File(getExternalFilesDir(null), "crash.log")
                Runtime.getRuntime()
                    .exec(arrayOf("logcat", "-d", "*:V"))
                    .inputStream.use { input ->
                        logFile.outputStream().use { output -> input.copyTo(output) }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to dump crash log", e)
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
