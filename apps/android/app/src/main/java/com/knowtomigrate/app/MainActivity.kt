package com.knowtomigrate.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.knowtomigrate.app.ui.theme.KnowToMigrateTheme
import com.knowtomigrate.app.ui.navigation.KtmNavGraph

class MainActivity : ComponentActivity() {

    private var sharedUris: List<Uri> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)
        setContent {
            KnowToMigrateTheme {
                KtmNavGraph(sharedUris = sharedUris)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        try {
            com.knowtomigrate.app.network.KtmAndroidManager.getInstance(this).start()
        } catch (_: Throwable) { }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            com.knowtomigrate.app.network.KtmAndroidManager.getInstance(this).stop()
        } catch (_: Throwable) { }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        try {
            when (intent.action) {
                Intent.ACTION_SEND -> {
                    @Suppress("DEPRECATION")
                    val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    sharedUris = listOfNotNull(uri)
                }
                Intent.ACTION_SEND_MULTIPLE -> {
                    @Suppress("DEPRECATION")
                    sharedUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM) ?: emptyList()
                }
            }
        } catch (_: Throwable) { }
    }
}
