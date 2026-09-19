package com.knowtomigrate

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.knowtomigrate.ui.theme.*

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle Android Sharesheet incoming files
        val sharedUris = handleIncomingShareIntent(intent)

        setContent {
            KnowToMigrateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = PureBlack
                ) {
                    AndroidHomeScreen(sharedUris = sharedUris)
                }
            }
        }
    }

    private fun handleIncomingShareIntent(intent: Intent?): List<Uri> {
        val uris = mutableListOf<Uri>()
        if (intent == null) return uris

        when (intent.action) {
            Intent.ACTION_SEND -> {
                (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let { uris.add(it) }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris.addAll(it) }
            }
        }
        return uris
    }
}

@Composable
fun AndroidHomeScreen(sharedUris: List<Uri>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Text(
                text = "KNOW TO MIGRATE",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp
            )
            Text(
                text = "Move Anything. Anywhere. Seamlessly.",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (sharedUris.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .background(KmOrange.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .border(1.dp, KmOrange.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Received ${sharedUris.size} item(s) from Share Sheet",
                        color = KmAmber,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Central Migration & Radar Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(GlassSurface, RoundedCornerShape(24.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "⚡ Nearby P2P Radar Active",
                    color = KmOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Searching for nearby Android and Windows devices...",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        // Bottom Action Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { /* Launch file picker */ },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KmOrange)
            ) {
                Text(text = "Send Files", fontWeight = FontWeight.Bold, color = PureBlack)
            }

            OutlinedButton(
                onClick = { /* Launch receive mode */ },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
            ) {
                Text(text = "Receive Mode", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
