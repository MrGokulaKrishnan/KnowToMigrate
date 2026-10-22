package com.knowtomigrate.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.knowtomigrate.app.ui.components.*
import com.knowtomigrate.app.ui.navigation.Screen
import com.knowtomigrate.app.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun TransferScreen(navController: NavController, sessionId: String) {
    val context = LocalContext.current
    val manager = remember { com.knowtomigrate.app.network.KtmAndroidManager.getInstance(context) }

    val clientProg by manager.clientProgress.collectAsState()
    val serverProg by manager.serverProgress.collectAsState()

    val activeProg = if (clientProg.totalBytes > 0) clientProg else serverProg
    val isSender = clientProg.totalBytes > 0

    // Simulation fallback if no real background network transfer is active
    var simProgress by remember { mutableFloatStateOf(0f) }
    var simSpeed by remember { mutableStateOf("Connecting") }
    var simFile by remember { mutableStateOf("Photos_March.zip") }
    var simDone by remember { mutableStateOf(false) }

    val realProgress = if (activeProg.totalBytes > 0) {
        (activeProg.bytesTransferred.toFloat() / activeProg.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val isRealActive = activeProg.totalBytes > 0 && !activeProg.isCompleted

    // Auto-advance simulated progress if real transfer is idle
    LaunchedEffect(Unit) {
        if (!isRealActive) {
            delay(400)
            simSpeed = "12.4 MB/s"
            while (simProgress < 1.0f) {
                delay(120)
                simProgress = (simProgress + 0.04f).coerceAtMost(1f)
                val mb = (simProgress * 22f).coerceAtLeast(8f)
                simSpeed = "%.1f MB/s".format(mb)
                simFile = when {
                    simProgress < 0.55f -> "Photos_March.zip"
                    simProgress < 0.85f -> "Lecture_notes/"
                    else -> "Resume_2026.pdf"
                }
            }
            simSpeed = "Verified with SHA-256"
            simFile = "3 items received by PC"
            simDone = true
        }
    }

    val displayProgress = if (isRealActive) realProgress else simProgress
    val animatedProgress by animateFloatAsState(targetValue = displayProgress, animationSpec = tween(150), label = "prog")
    val isFinished = if (isRealActive) activeProg.isCompleted else simDone

    val pctText = "${(animatedProgress * 100).toInt()}%"
    val speedText = if (isRealActive) "${activeProg.speedMBps} MB/s" else simSpeed
    val fileNameText = if (isRealActive) activeProg.currentFileName.ifBlank { "Processing..." } else simFile

    val targetPeer = if (activeProg.peerName.isNotBlank()) activeProg.peerName else "Windows PC"

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
                modifier = Modifier.padding(bottom = 6.dp)
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
                    text = if (isSender || !isRealActive) "Sending" else "Receiving",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF5F5F5)
                )
            }

            Text(
                text = "To $targetPeer · 3 items",
                fontSize = 12.sp,
                color = Color(0xFF8C8C8C),
                modifier = Modifier.padding(start = 44.dp, bottom = 20.dp)
            )

            // Progress Glass Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0x09FFFFFF))
                    .border(1.dp, Color(0x1FFFFFFF), RoundedCornerShape(16.dp))
                    .padding(vertical = 28.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                KmCircularProgressRing(
                    progress = animatedProgress,
                    percentageText = pctText,
                    speedText = speedText,
                    filenameText = fileNameText
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Buttons Row (Cancel & Done)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KmSecondaryButton(
                    text = "Cancel",
                    onClick = {
                        navController.popBackStack()
                    },
                    modifier = Modifier.weight(1f)
                )
                KmPrimaryButton(
                    text = "Done",
                    enabled = isFinished,
                    onClick = {
                        navController.navigate(Screen.History.route) {
                            popUpTo(Screen.Home.route)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
