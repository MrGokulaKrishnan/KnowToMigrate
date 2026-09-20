package com.knowtomigrate.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KnowToMigrateApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize KTM JNI library
        try {
            System.loadLibrary("ktm")
        } catch (e: UnsatisfiedLinkError) {
            // ktm.so not yet built — app runs in UI demo mode
        }
    }
}
