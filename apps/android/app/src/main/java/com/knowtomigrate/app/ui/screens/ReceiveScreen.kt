package com.knowtomigrate.app.ui.screens

/*
 * KnowToMigrate - ReceiveScreen
 * Updated: 22 October 2026
 * AMOLED black, KM Orange accents, Liquid Glass surfaces, 3-tab floating bottom bar
 */

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
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

    var isAcceptingFiles by remember { mutableStateOf(true) }
    var isDetailsExpanded by remember { mutableStateOf(false) }
    var saveFolder by remember { mutableStateOf("Downloads / KnowToMigrate") }

    // If active incoming transfer starts, navigate to TransferScreen
    LaunchedEffect(serverProg.status) {
        if (serverProg.status == com.knowtomigrate.app.network.TransferStatus.TRANSFERRING) {
            navController.navigate(Screen.Transfer.withSession("incoming"))
        }
    }

    Scaffold(
        containerColor = KmBlack,
        bottomBar = {
            KmFloatingBottomBar(
                selectedIndex = 2,
                onSelect = { idx ->
                    when (idx) {
                        0 -> navController.navigate(Screen.Home.route)
                        1 -> navController.navigate(Screen.Send.route)
                        2 -> { /* already on Receive */ }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(KmBlack)
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Top row: Header & State text with toggle switch
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Receive",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF5F5F5)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isAcceptingFiles) "Accepting files" else "Not accepting files",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isAcceptingFiles) KmOnline else KmTextMuted
                        )
                    }

                    Switch(
                        checked = isAcceptingFiles,
                        onCheckedChange = { isAcceptingFiles = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = KmTextOnBrand,
                            checkedTrackColor = KmOrange,
                            uncheckedThumbColor = KmTextMuted,
                            uncheckedTrackColor = KmBlackElevated,
                            uncheckedBorderColor = KmGlassBorder
                        )
                    )
                }
            }

            // 2. Pairing PIN Card (dimmed when toggle is off)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isAcceptingFiles) 1f else 0.42f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(KmGlassBackground)
                        .border(1.dp, KmGlassBorder, RoundedCornerShape(16.dp))
                        .drawBehind {
                            // 1px top highlight at 28% white
                            drawLine(
                                color = Color(0x47FFFFFF),
                                start = Offset(0f, 0.5f),
                                end = Offset(size.width, 0.5f),
                                strokeWidth = 1f
                            )
                        }
                        .padding(vertical = 24.dp, horizontal = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (isAcceptingFiles) {
                            KmRadarWidget(size = 64.dp)
                            Spacer(modifier = Modifier.height(14.dp))
                        }

                        Text(
                            text = "Pairing PIN",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = KmTextMuted
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "482 915",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp,
                            color = Color(0xFFF5F5F5)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Check this matches the PIN on the sending device before you accept.",
                            fontSize = 12.sp,
                            color = KmTextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                }
            }

            // 3. "This phone" row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(KmGlassBackground)
                        .border(1.dp, KmGlassBorder, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x14FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = KmOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = Build.MODEL ?: "Android Device",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF5F5F5)
                        )
                        Text(
                            text = if (isAcceptingFiles) "Visible to nearby devices" else "Hidden from nearby devices",
                            fontSize = 12.sp,
                            color = KmTextMuted
                        )
                    }
                    KmStatusDot(status = if (isAcceptingFiles) "online" else "offline")
                }
            }

            // 4. "Save to" row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(KmGlassBackground)
                        .border(1.dp, KmGlassBorder, RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0x14FFFFFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = Color(0xFFF5F5F5),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = saveFolder,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF5F5F5)
                        )
                        Text(
                            text = "Shows in Gallery and Files",
                            fontSize = 12.sp,
                            color = KmTextMuted
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x1AFFFFFF))
                            .border(1.dp, KmGlassBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                // Folder location
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Change",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFF5F5F5)
                        )
                    }
                }
            }

            // 5. "Not encrypted yet" warning row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x0FFFC24D))
                        .border(1.dp, Color(0x33FFC24D), RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = KmWarning,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Not encrypted yet",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KmWarning
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Transfers stay on your local network but are not encrypted. Do not use on public Wi-Fi.",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = Color(0xFFCCCCCC)
                        )
                    }
                }
            }

            // 6. Details disclosure
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(KmGlassBackground)
                        .border(1.dp, KmGlassBorder, RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDetailsExpanded = !isDetailsExpanded }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Details",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFF5F5F5)
                        )
                        Icon(
                            imageVector = if (isDetailsExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isDetailsExpanded) "Collapse" else "Expand",
                            tint = KmTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    AnimatedVisibility(visible = isDetailsExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HorizontalDivider(color = KmGlassBorder, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(2.dp))
                            ReceiveDetailItem(
                                label = "Unique ID",
                                value = "android-${Build.MODEL.lowercase().replace(' ', '-')}"
                            )
                            ReceiveDetailItem(
                                label = "Transfer Port",
                                value = "54124"
                            )
                            ReceiveDetailItem(
                                label = "Discovery Port",
                                value = "54123"
                            )
                            ReceiveDetailItem(
                                label = "Transport",
                                value = "Local Wi-Fi / Direct LAN"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiveDetailItem(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = KmTextMuted
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = Color(0xFFF5F5F5)
        )
    }
}
