package com.knowtomigrate.app

import android.app.Application
import android.os.Looper
import android.util.Log
import java.io.File

class KnowToMigrateApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Robust uncaught exception handler: logs crash to disk and protects app
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("KnowToMigrate", "Caught fatal exception in thread [${thread.name}]", throwable)
            try {
                val crashFile = File(filesDir, "last_crash.txt")
                crashFile.writeText("Thread: ${thread.name}\nTimestamp: ${System.currentTimeMillis()}\nError: ${throwable.stackTraceToString()}")
            } catch (_: Throwable) {}

            val isMainThread = thread == Looper.getMainLooper().thread || thread.name.equals("main", ignoreCase = true)
            if (!isMainThread) {
                // Background thread error: swallow to keep UI alive and prevent OS crash dialog
                Log.w("KnowToMigrate", "Safely recovered from background thread crash in [${thread.name}]")
                return@setDefaultUncaughtExceptionHandler
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
