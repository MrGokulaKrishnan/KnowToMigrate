# Keep JNI native methods
-keep class com.knowtomigrate.app.jni.KtmJni { *; }
-keepclassmembers class com.knowtomigrate.app.jni.KtmJni {
    native <methods>;
    static <fields>;
}

# Keep data classes used for JSON parsing
-keep class com.knowtomigrate.app.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Hilt
-keep @dagger.hilt.android.AndroidEntryPoint class * { *; }

# Room
-keep @androidx.room.Entity class * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }

# Keep Serializable classes
-keepnames class * implements java.io.Serializable

-dontwarn com.knowtomigrate.**
