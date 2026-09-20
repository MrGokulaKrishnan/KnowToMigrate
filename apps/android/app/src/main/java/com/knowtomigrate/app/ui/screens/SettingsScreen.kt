package com.knowtomigrate.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.knowtomigrate.app.ui.theme.*

@Composable
fun SettingsScreen(navController: NavController) {
    var deviceName by remember { mutableStateOf("My Android") }
    var receiveDir by remember { mutableStateOf("/sdcard/KnowToMigrate/") }
    var isVisible by remember { mutableStateOf(true) }
    var autoAcceptTrusted by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KmBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Text(
                text = "Settings",
                color = KmTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )

            // Device section
            SettingsSection(title = "Device") {
                SettingsTextField(
                    label = "Device Name",
                    value = deviceName,
                    onValueChange = { deviceName = it }
                )
            }

            // Transfer section
            SettingsSection(title = "Transfers") {
                SettingsTextField(
                    label = "Receive Folder",
                    value = receiveDir,
                    onValueChange = { receiveDir = it }
                )
                SettingsToggle(
                    label = "Visible to Nearby Devices",
                    description = "Allow KnowToMigrate to broadcast your device on the local network",
                    checked = isVisible,
                    onCheckedChange = { isVisible = it }
                )
                SettingsToggle(
                    label = "Auto-Accept Trusted Devices",
                    description = "Automatically accept transfers from devices you trust",
                    checked = autoAcceptTrusted,
                    onCheckedChange = { autoAcceptTrusted = it }
                )
            }

            // About section
            SettingsSection(title = "About") {
                SettingsInfoRow(label = "Version", value = "1.0.0")
                SettingsInfoRow(label = "Build", value = "2026.09.20")
                SettingsInfoRow(label = "Protocol", value = "KTM v2")
                SettingsInfoRow(label = "Encryption", value = "AES-256-GCM")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            text = title.uppercase(),
            color = KmOrange,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
        )
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = KmGlassBackground,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(label, color = KmTextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = KmOrange,
                unfocusedBorderColor = KmGlassBorder,
                focusedTextColor = KmTextPrimary,
                unfocusedTextColor = KmTextPrimary,
                cursorColor = KmOrange,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
            ),
            shape = RoundedCornerShape(12.dp),
        )
    }
}

@Composable
private fun SettingsToggle(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(label, color = KmTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, color = KmTextMuted, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = KmOrange,
                uncheckedThumbColor = KmTextMuted,
                uncheckedTrackColor = KmBlackElevated,
            )
        )
    }
}

@Composable
private fun SettingsInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = KmTextSecondary, fontSize = 14.sp)
        Text(value, color = KmTextMuted, fontSize = 14.sp)
    }
}
