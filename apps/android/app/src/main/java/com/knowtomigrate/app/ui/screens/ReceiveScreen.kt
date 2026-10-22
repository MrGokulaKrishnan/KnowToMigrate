package com.knowtomigrate.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.knowtomigrate.app.ui.components.*
import com.knowtomigrate.app.ui.navigation.Screen
import com.knowtomigrate.app.ui.theme.*

@Composable
fun ReceiveScreen(navController: NavController) {
    val context = LocalContext.current
    val manager = remember { com.knowtomigrate.app.network.KtmAndroidManager.getInstance(context) }
    val serverProg by manager.serverProgress.collectAsState()

    // If active incoming transfer starts, navigate to TransferScreen
    LaunchedEffect(serverProg.status) {
        if (serverProg.status == com.knowtomigrate.app.network.TransferStatus.TRANSFERRING) {
            navController.navigate(Screen.Transfer.withSession("incoming"))
        }
    }

    Scaffold(
        containerColor = KmBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KmBlack)
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 18.dp)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFFF5F5F5)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Receive",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF5F5F5)
                )
            }

            Spacer(modifier = Modifier.weight(0.15f))

            // Main Prototype Receive Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x09FFFFFF))
                    .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(16.dp))
                    .padding(vertical = 32.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    KmRadarWidget(size = 86.dp)

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Waiting for a device",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF5F5F5)
                    )

                    Text(
                        text = "Keep this screen open",
                        fontSize = 12.sp,
                        color = Color(0xFF8C8C8C),
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "482 915",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        color = Color(0xFFF5F5F5)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Confirm this PIN on the sending device",
                        fontSize = 12.sp,
                        color = Color(0xFF8C8C8C),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Secondary Action Button: Stop receiving
            KmSecondaryButton(
                text = "Stop receiving",
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
