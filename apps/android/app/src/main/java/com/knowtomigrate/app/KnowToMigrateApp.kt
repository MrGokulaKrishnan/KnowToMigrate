package com.knowtomigrate.app

import android.app.Application

class KnowToMigrateApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            System.loadLibrary("ktm_jni")
        } catch (_: Throwable) {
            // Native library optional; pure Kotlin network engine runs seamlessly
        }
    }
}
