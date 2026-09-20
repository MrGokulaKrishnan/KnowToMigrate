package com.knowtomigrate.app.service

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.*
import androidx.core.app.NotificationCompat
import com.knowtomigrate.app.MainActivity

private const val CHANNEL_ID = "ktm_transfer"
private const val NOTIF_ID = 1001

class TransferForegroundService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private var sessionId: String = ""
    private var bytesSent: Long = 0L
    private var totalBytes: Long = 1L
    private var speedMbs: Double = 0.0
    private var etaSecs: Double = 0.0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireLocks()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sessionId = intent?.getStringExtra("SESSION_ID") ?: "unknown"
        val direction = intent?.getStringExtra("DIRECTION") ?: "send"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(NOTIF_ID, buildNotification())
        }

        // Start periodic notification refresh
        scheduleNotificationUpdate()

        return START_NOT_STICKY
    }

    private fun scheduleNotificationUpdate() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIF_ID, buildNotification())
                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    /** Call this from the JNI progress callback to update the notification state */
    fun updateProgress(bytes: Long, total: Long, speed: Double, eta: Double) {
        bytesSent = bytes
        totalBytes = total
        speedMbs = speed / 1_000_000.0
        etaSecs = eta
    }

    /** Call this when the transfer completes to stop the service */
    fun onTransferComplete() {
        handler.removeCallbacksAndMessages(null)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val progress = if (totalBytes > 0) ((bytesSent.toDouble() / totalBytes) * 100).toInt() else 0
        val speed = "%.1f MB/s".format(speedMbs)
        val eta = formatEta(etaSecs)

        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val pauseIntent = PendingIntent.getBroadcast(
            this, 1,
            Intent("com.knowtomigrate.ACTION_PAUSE").setPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE
        )

        val cancelIntent = PendingIntent.getBroadcast(
            this, 2,
            Intent("com.knowtomigrate.ACTION_CANCEL").setPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("KnowToMigrate — Transferring")
            .setContentText("$progress% · $speed · ETA $eta")
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_media_pause, "Pause", pauseIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancelIntent)
            .setColor(0xFF5A00.toInt())
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "File Transfer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "KnowToMigrate active transfer progress"
            setShowBadge(false)
        }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    private fun acquireLocks() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "KnowToMigrate::TransferWakeLock"
        ).apply { acquire(10 * 60 * 60 * 1000L) } // max 10 hours

        val wm = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        wifiLock = wm.createWifiLock(
            WifiManager.WIFI_MODE_FULL_HIGH_PERF,
            "KnowToMigrate::TransferWifiLock"
        ).apply { acquire() }
    }

    private fun releaseLocks() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wifiLock?.takeIf { it.isHeld }?.release()
    }

    private fun formatEta(secs: Double): String {
        val s = secs.toLong()
        val m = s / 60
        return if (m > 0) "%02d:%02d".format(m, s % 60) else "00:%02d".format(s)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        releaseLocks()
        super.onDestroy()
    }
}
