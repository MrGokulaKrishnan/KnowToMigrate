package com.knowtomigrate.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.knowtomigrate.app.ui.components.*
import com.knowtomigrate.app.ui.theme.*

@Composable
fun HistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val manager = remember { com.knowtomigrate.app.network.KtmAndroidManager.getInstance(context) }
    val allRecords by manager.historyRepository.transfers.collectAsState()

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
                    text = "History",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF5F5F5)
                )
            }

            Text(
                text = "Metadata only. File contents are never logged.",
                fontSize = 12.sp,
                color = Color(0xFF8C8C8C),
                modifier = Modifier.padding(start = 44.dp, bottom = 18.dp)
            )

            // History Records List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Default prototype rows
                item {
                    KmRecentTransferRow(
                        filename = "Photos_March.zip",
                        subtitle = "1.2 GB · 20 MB/s · 10:42",
                        isVerified = true
                    )
                }
                item {
                    KmRecentTransferRow(
                        filename = "Resume_2026.pdf",
                        subtitle = "0.4 MB · Yesterday",
                        isVerified = true
                    )
                }
                item {
                    KmRecentTransferRow(
                        filename = "Lecture_notes/",
                        subtitle = "320 MB · Oct 19",
                        isVerified = false,
                        isFolder = true
                    )
                }

                // Dynamic real records
                items(allRecords) { record ->
                    val isComp = record.status == com.knowtomigrate.app.data.TransferRecordStatus.COMPLETED
                    KmRecentTransferRow(
                        filename = record.fileName,
                        subtitle = "${record.formattedSize} · ${record.formattedDate}",
                        isVerified = isComp
                    )
                }
            }
        }
    }
}
