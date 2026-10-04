$path = "c:\KnowToMigrate\apps\android\app\src\main\java\com\knowtomigrate\app\ui\screens\SettingsScreen.kt"
$content = Get-Content $path -Raw

# 1. Add imports if needed
if ($content -notmatch 'import com.knowtomigrate.app.updater.UpdateManager') {
    $content = $content.Replace(
        "import com.knowtomigrate.app.BuildConfig",
        "import com.knowtomigrate.app.BuildConfig`r`nimport com.knowtomigrate.app.updater.UpdateManager`r`nimport com.knowtomigrate.app.updater.UpdateState`r`nimport com.knowtomigrate.app.ui.components.KmProgressBar`r`nimport java.util.Locale"
    )
}

# 2. Update states
$oldStates = @"
    val coroutineScope = rememberCoroutineScope()
    var updateCheckState by remember { mutableStateOf("Idle") }
    var availableVersion by remember { mutableStateOf("") }
"@

$newStates = @"
    val coroutineScope = rememberCoroutineScope()
    val updateManager = remember { UpdateManager.getInstance(context) }
    val updateState by updateManager.updateState.collectAsState()
"@
$content = $content.Replace($oldStates, $newStates)

# 3. Replace Update Center UI section
$oldSectionRegex = '(?s)// 5\. Update Center.*?// 6\. About Section'
$newSection = @"
            // 5. Update Center (Liquid Glass APK Self-Updater)
            SettingsSectionHeader(title = "Update Center", icon = Icons.Default.SystemUpdate)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = KmGlassBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, KmOrange.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Header Row: Title, Current version, Check button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("KnowToMigrate APK Self-Updater", color = KmTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Installed: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
                                color = KmTextMuted,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = { updateManager.checkForUpdates() },
                            enabled = updateState !is UpdateState.Checking && updateState !is UpdateState.Downloading,
                            colors = ButtonDefaults.buttonColors(containerColor = KmOrange),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            if (updateState is UpdateState.Checking) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Checking...", color = Color.White, fontSize = 12.sp)
                            } else {
                                Text("Check for Updates", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    when (val state = updateState) {
                        is UpdateState.Idle -> {
                            Text("Independent APK distribution via direct HTTPS download and cryptographic verification.", color = KmTextMuted, fontSize = 12.sp)
                        }
                        is UpdateState.Checking -> {
                            Text("Querying official release channel for latest package...", color = KmOrangeLight, fontSize = 12.sp)
                        }
                        is UpdateState.UpToDate -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = KmSuccess.copy(alpha = 0.1f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, KmSuccess.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = KmSuccess, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("KnowToMigrate is up to date (v${BuildConfig.VERSION_NAME}).", color = KmTextPrimary, fontSize = 13.sp)
                                }
                            }
                        }
                        is UpdateState.UpdateAvailable -> {
                            val manifest = state.manifest
                            Column {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF140D00),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, KmOrange.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Update Available: v${manifest.versionName}", color = KmOrangeLight, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                            KmBadge(text = manifest.formattedSize, color = KmOrange)
                                        }

                                        if (manifest.releaseNotes.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("What's New in ${manifest.title}:", color = KmTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            manifest.releaseNotes.forEach { note ->
                                                Text("• $note", color = KmTextSecondary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 1.dp))
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                val isTransferring = updateManager.isTransferActive()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    if (isTransferring) {
                                        OutlinedButton(
                                            onClick = {
                                                updateManager.startDownload(manifest)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Text("Download in Background", color = KmTextPrimary, fontSize = 12.sp)
                                        }
                                    }
                                    Button(
                                        onClick = { updateManager.startDownload(manifest) },
                                        colors = ButtonDefaults.buttonColors(containerColor = KmOrange),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Update Now", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        is UpdateState.Downloading -> {
                            val manifest = state.manifest
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Downloading v${manifest.versionName}...", color = KmTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                    Text("${(state.progress * 100).toInt()}%", color = KmOrangeLight, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                KmProgressBar(progress = state.progress)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${formatFileSize(state.bytesDownloaded)} / ${formatFileSize(state.totalBytes)}",
                                        color = KmTextMuted,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "${String.format(Locale.US, "%.1f", state.speedMBps)} MB/s · ETA ${state.etaSecs}s",
                                        color = KmTextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = { updateManager.cancelDownload(manifest) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text("Cancel", color = KmTextMuted, fontSize = 11.sp)
                                }
                            }
                        }
                        is UpdateState.Verifying -> {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = KmOrange, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Verifying SHA-256 cryptographic signature...", color = KmOrangeLight, fontSize = 13.sp)
                            }
                        }
                        is UpdateState.ReadyToInstall -> {
                            val apkFile = state.apkFile
                            val manifest = state.manifest
                            val isTransferring = updateManager.isTransferActive()

                            Column {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF0D2614),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, KmSuccess.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Verified, contentDescription = null, tint = KmSuccess, modifier = Modifier.size(24.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("v${manifest.versionName} Verified & Ready", color = KmTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                            Text("SHA-256 integrity check passed. Safe to install.", color = KmSuccess, fontSize = 11.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    if (isTransferring) {
                                        OutlinedButton(
                                            onClick = {
                                                updateManager.queueInstallAfterTransfer(apkFile, manifest)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Text("Install After Transfer", color = KmOrangeLight, fontSize = 12.sp)
                                        }
                                    }
                                    Button(
                                        onClick = { updateManager.installApk(apkFile, manifest) },
                                        colors = ButtonDefaults.buttonColors(containerColor = KmSuccess),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.SystemUpdateAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Install Now", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        is UpdateState.QueuedAfterTransfer -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF140D00),
                                border = androidx.compose.foundation.BorderStroke(1.dp, KmOrange),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.HourglassTop, contentDescription = null, tint = KmOrange, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Update Queued for Installation", color = KmTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text("v${state.manifest.versionName} will automatically launch PackageInstaller once active transfers finish.", color = KmTextMuted, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                        is UpdateState.Installing -> {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(color = KmSuccess, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Launching Android PackageInstaller...", color = KmTextPrimary, fontSize = 13.sp)
                            }
                        }
                        is UpdateState.Error -> {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = KmError.copy(alpha = 0.1f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, KmError.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = KmError, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Update Check Failed", color = KmError, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                        Text(state.message, color = KmTextMuted, fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 6. About Section
"@

$content = [regex]::Replace($content, $oldSectionRegex, $newSection.Trim())

# 4. Add formatFileSize helper before the end of the file
$helperFunction = @"

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> String.format(Locale.US, "%.1f GB", bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> String.format(Locale.US, "%.1f MB", bytes / 1_000_000.0)
        bytes >= 1_000 -> String.format(Locale.US, "%.1f KB", bytes / 1_000.0)
        else -> "`$bytes B"
    }
}
"@

if ($content -notmatch 'fun formatFileSize') {
    $content = $content + "`r`n" + $helperFunction
}

Set-Content -Path $path -Value $content -Encoding utf8
Write-Host "Updated SettingsScreen.kt successfully."
