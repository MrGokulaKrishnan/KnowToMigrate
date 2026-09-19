package com.knowtomigrate.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat

class TransferForegroundService : Service() {

    private val CHANNEL_ID = "knowtomigrate_transfers"
    private val NOTIFICATION_ID = 1001

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Acquire partial wake lock & Wi-Fi high-perf lock
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "KnowToMigrate::TransferLock").apply {
            acquire(4 * 60 * 60 * 1000L) // 4 hours max safety timeout
        }

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "KnowToMigrate::WifiLock").apply {
            acquire()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val fileName = intent?.getStringExtra("EXTRA_FILE_NAME") ?: "Batch Migration"
        val progress = intent?.getIntExtra("EXTRA_PROGRESS", 0) ?: 0
        val speedMbps = intent?.getDoubleExtra("EXTRA_SPEED_MBPS", 85.0) ?: 85.0

        val notification = buildNotification(fileName, progress, speedMbps)
        startForeground(NOTIFICATION_ID, notification)

        return START_NOT_STICKY
    }

    private fun buildNotification(fileName: String, progress: Int, speedMbps: Double): Notification {
        val pauseIntent = PendingIntent.getService(this, 1, Intent(this, TransferForegroundService::class.java).apply { action = "ACTION_PAUSE" }, PendingIntent.FLAG_IMMUTABLE)
        val cancelIntent = PendingIntent.getService(this, 2, Intent(this, TransferForegroundService::class.java).apply { action = "ACTION_CANCEL" }, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("KnowToMigrate Streaming")
            .setContentText("$fileName · $speedMbps MB/s")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pauseIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Active File Transfers",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows real-time speed and progress for KnowToMigrate transfers"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        wakeLock?.let { if (it.isHeld) it.release() }
        wifiLock?.let { if (it.isHeld) it.release() }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
