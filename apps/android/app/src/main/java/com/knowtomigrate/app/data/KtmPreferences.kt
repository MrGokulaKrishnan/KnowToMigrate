package com.knowtomigrate.app.data

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class KtmPreferences private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("ktm_settings", Context.MODE_PRIVATE)

    private val _deviceName = MutableStateFlow(loadDeviceName())
    val deviceName: StateFlow<String> = _deviceName.asStateFlow()

    private val _receiveFolderDisplay = MutableStateFlow(
        prefs.getString(KEY_RECEIVE_FOLDER_DISPLAY, "Downloads / KnowToMigrate") ?: "Downloads / KnowToMigrate"
    )
    val receiveFolderDisplay: StateFlow<String> = _receiveFolderDisplay.asStateFlow()

    private val _receiveFolderUri = MutableStateFlow<String?>(
        prefs.getString(KEY_RECEIVE_FOLDER_URI, null)
    )
    val receiveFolderUri: StateFlow<String?> = _receiveFolderUri.asStateFlow()

    private val _isDiscoverable = MutableStateFlow(
        prefs.getBoolean(KEY_IS_DISCOVERABLE, true)
    )
    val isDiscoverable: StateFlow<Boolean> = _isDiscoverable.asStateFlow()

    private val _autoAcceptTrusted = MutableStateFlow(
        prefs.getBoolean(KEY_AUTO_ACCEPT_TRUSTED, false)
    )
    val autoAcceptTrusted: StateFlow<Boolean> = _autoAcceptTrusted.asStateFlow()

    private val _requirePin = MutableStateFlow(
        prefs.getBoolean(KEY_REQUIRE_PIN, true)
    )
    val requirePin: StateFlow<Boolean> = _requirePin.asStateFlow()

    private val _verifySha256 = MutableStateFlow(
        prefs.getBoolean(KEY_VERIFY_SHA256, true)
    )
    val verifySha256: StateFlow<Boolean> = _verifySha256.asStateFlow()

    private fun loadDeviceName(): String {
        val saved = prefs.getString(KEY_DEVICE_NAME, null)
        if (!saved.isNullOrBlank()) return saved.trim()

        return getCleanDeviceModel()
    }

    fun setDeviceName(name: String) {
        val cleanName = name.trim().ifBlank { getCleanDeviceModel() }
        prefs.edit().putString(KEY_DEVICE_NAME, cleanName).apply()
        _deviceName.value = cleanName
    }

    fun setReceiveFolder(display: String, uriString: String?) {
        prefs.edit()
            .putString(KEY_RECEIVE_FOLDER_DISPLAY, display)
            .putString(KEY_RECEIVE_FOLDER_URI, uriString)
            .apply()
        _receiveFolderDisplay.value = display
        _receiveFolderUri.value = uriString
    }

    fun setDiscoverable(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_IS_DISCOVERABLE, enabled).apply()
        _isDiscoverable.value = enabled
    }

    fun setAutoAcceptTrusted(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_ACCEPT_TRUSTED, enabled).apply()
        _autoAcceptTrusted.value = enabled
    }

    fun setRequirePin(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_REQUIRE_PIN, enabled).apply()
        _requirePin.value = enabled
    }

    fun setVerifySha256(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VERIFY_SHA256, enabled).apply()
        _verifySha256.value = enabled
    }

    companion object {
        private const val KEY_DEVICE_NAME = "device_name"
        private const val KEY_RECEIVE_FOLDER_DISPLAY = "receive_folder_display"
        private const val KEY_RECEIVE_FOLDER_URI = "receive_folder_uri"
        private const val KEY_IS_DISCOVERABLE = "is_discoverable"
        private const val KEY_AUTO_ACCEPT_TRUSTED = "auto_accept_trusted"
        private const val KEY_REQUIRE_PIN = "require_pin"
        private const val KEY_VERIFY_SHA256 = "verify_sha256"

        fun getCleanDeviceModel(): String {
            val manufacturer = Build.MANUFACTURER.orEmpty().replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            var model = Build.MODEL.orEmpty().trim()
            if (model.startsWith(manufacturer, ignoreCase = true)) {
                model = model.substring(manufacturer.length).trim()
            }
            return if (manufacturer.isNotBlank() && model.isNotBlank()) {
                "$manufacturer $model"
            } else if (model.isNotBlank()) {
                model
            } else {
                "Android Device"
            }
        }

        @Volatile
        private var instance: KtmPreferences? = null

        fun getInstance(context: Context): KtmPreferences {
            return instance ?: synchronized(this) {
                instance ?: KtmPreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}
