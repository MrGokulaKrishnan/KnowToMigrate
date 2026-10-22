package com.knowtomigrate.app.ui.screens

/*
 * KnowToMigrate - Transfer Progress Screen
 * Updated: 22 October 2026
 * Pure AMOLED #000000 base with KM Electric Orange accents and Dark Liquid Glass
 * Strict requirement: Displays only real computed transfer metrics (no simulated/fake progress).
 */

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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.knowtomigrate.app.R
import com.knowtomigrate.app.ui.components.*
import com.knowtomigrate.app.ui.navigation.Screen
import com.knowtomigrate.app.ui.theme.*

@Composable
fun TransferScreen(navController: NavController, sessionId: String) {
    val context = LocalContext.current
    val manager = remember { com.knowtomigrate.app.network.KtmAndroidManager.getInstance(context) }

    val clientProg by manager.clientProgress.collectAsState()
    val serverProg by manager.serverProgress.collectAsState()

    // Determine whether this session is sending or receiving
    val isSender = clientProg.totalBytes > 0 || sessionId.contains("outgoing", ignoreCase = true)
    val activeProg = if (isSender) clientProg else serverProg

    // Real computed metrics only — no simulation
    val realFraction = if (activeProg.totalBytes > 0) {
        (activeProg.bytesTransferred.toFloat() / activeProg.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else if (activeProg.isCompleted) {
        1.0f
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = realFraction,
        animationSpec = tween(200),
        label = "real_prog"
    )

    val pctText = if (activeProg.totalBytes > 0) {
        "${activeProg.percentage.toInt()}%"
    } else if (activeProg.isCompleted) {
        "100%"
    } else "0%"

    val speedText = when {
        activeProg.isCompleted -> "Complete"
        activeProg.speedMBps > 0.05 -> "${"%.1f".format(activeProg.speedMBps)} MB/s"
        activeProg.status == com.knowtomigrate.app.network.TransferStatus.TRANSFERRING -> "Transferring…"
        else -> "Connecting…"
    }

    val fileNameText = when {
        activeProg.isCompleted -> "All files transferred"
        activeProg.currentFileName.isNotBlank() -> activeProg.currentFileName
        else -> "Preparing files…"
    }

    val targetPeer = if (activeProg.peerName.isNotBlank()) activeProg.peerName else "Nearby device"

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
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = KmTextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isSender) "Sending" else "Receiving",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = KmTextPrimary
                )
            }

            Text(
                text = "${if (isSender) "To" else "From"} $targetPeer",
                fontSize = 13.sp,
                color = KmTextMuted,
                modifier = Modifier.padding(start = 54.dp, bottom = 20.dp)
            )

            // Progress Glass Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(KmRadii.Card))
                    .background(KmGlassBackground)
                    .border(1.dp, KmGlassBorder, RoundedCornerShape(KmRadii.Card))
                    .drawBehind {
                        drawLine(
                            color = KmGlassTopHighlight,
                            start = Offset(18.dp.toPx(), 1.dp.toPx()),
                            end = Offset(size.width - 18.dp.toPx(), 1.dp.toPx()),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                    .padding(vertical = 32.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                KmCircularProgressRing(
                    progress = animatedProgress,
                    percentageText = pctText,
                    speedText = speedText,
                    filenameText = fileNameText
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Honest Security Notice Row
            KmSecurityNoticeRow()

            Spacer(modifier = Modifier.weight(1f))

            // Action Buttons Row: Cancel (Secondary) and Done (Primary, enabled only upon completion)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KmSecondaryButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = {
                        navController.popBackStack()
                    },
                    modifier = Modifier.weight(1f)
                )

                KmPrimaryButton(
                    text = stringResource(R.string.action_done),
                    enabled = activeProg.isCompleted,
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
