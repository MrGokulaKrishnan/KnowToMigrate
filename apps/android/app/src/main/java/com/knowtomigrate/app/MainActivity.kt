package com.knowtomigrate.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.knowtomigrate.app.ui.theme.KnowToMigrateTheme
import com.knowtomigrate.app.ui.navigation.KtmNavGraph

class MainActivity : ComponentActivity() {

    private val sharedUrisState = mutableStateOf<List<Uri>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
        } catch (_: Throwable) { }
        try {
            handleIntent(intent)
        } catch (_: Throwable) { }
        try {
            setContent {
                KnowToMigrateTheme {
                    KtmNavGraph(sharedUris = sharedUrisState.value)
                }
            }
        } catch (t: Throwable) {
            android.util.Log.e("KnowToMigrate", "Failed to set Compose content", t)
        }
    }

    override fun onStart() {
        super.onStart()
        lifecycleScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(1000L)
            try {
                com.knowtomigrate.app.network.KtmAndroidManager.getInstance(applicationContext).start()
            } catch (_: Throwable) { }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                com.knowtomigrate.app.network.KtmAndroidManager.getInstance(applicationContext).stop()
            } catch (_: Throwable) { }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        try {
            val extracted = extractUris(intent)
            if (extracted.isNotEmpty()) {
                sharedUrisState.value = extracted
            }
        } catch (_: Throwable) { }
    }

    private fun extractUris(intent: Intent): List<Uri> {
        val result = mutableListOf<Uri>()

        // 1. Handle EXTRA_STREAM
        when (intent.action) {
            Intent.ACTION_SEND -> {
                @Suppress("DEPRECATION")
                val streamUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                if (streamUri != null) result.add(streamUri)
                else if (intent.data != null) result.add(intent.data!!)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                @Suppress("DEPRECATION")
                val list = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                if (list != null) result.addAll(list)
            }
        }

        // 2. Handle ClipData (Modern Android Gallery / Files apps set ClipData)
        val clipData = intent.clipData
        if (clipData != null) {
            for (i in 0 until clipData.itemCount) {
                val itemUri = clipData.getItemAt(i).uri
                if (itemUri != null && !result.contains(itemUri)) {
                    result.add(itemUri)
                }
            }
        }

        // 3. Grant & retain read permissions for all extracted URIs
        for (uri in result) {
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Throwable) {
                // If not persistable, transient grant still allows reading stream
            }
        }

        return result.distinct()
    }
}
