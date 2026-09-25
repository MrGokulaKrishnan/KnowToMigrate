package com.knowtomigrate.app.network

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

class KtmTransportManager(private val context: Context) {

    /**
     * Checks device hardware / system capabilities for transports
     */
    val isWifiDirectSupported: Boolean by lazy {
        context.packageManager.hasSystemFeature("android.hardware.wifi.direct")
    }

    val isBluetoothSupported: Boolean by lazy {
        context.packageManager.hasSystemFeature("android.hardware.bluetooth")
    }

    val localSupportedTransports: List<String>
        get() {
            val list = mutableListOf("WIFI_LAN")
            if (isWifiDirectSupported) list.add("WIFI_DIRECT")
            if (isBluetoothSupported) list.add("BLUETOOTH")
            return list
        }

    /**
     * Probes Wi-Fi LAN reachability by performing a fast TCP handshake to targetIp:targetPort.
     * Timeout: 800ms to keep discovery & selection instantaneous.
     */
    suspend fun probeWifiLanReachability(ipAddress: String, port: Int = KtmConstants.TRANSFER_PORT, timeoutMs: Int = 800): Boolean {
        if (ipAddress.isBlank()) return false
        return withContext(Dispatchers.IO) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ipAddress, port), timeoutMs)
                    true
                }
            } catch (_: Throwable) {
                false
            }
        }
    }

    /**
     * Executes the "Select Best" transport negotiation hierarchy:
     * Priority 1: Wi-Fi LAN (fastest: 50-120+ MB/s, 0 negotiation overhead)
     * Priority 2: Wi-Fi Direct (high speed: 30-80 MB/s, direct peer-to-peer)
     * Priority 3: Bluetooth (universal fallback: 1-2 MB/s, works offline/air-gapped)
     */
    suspend fun evaluateAndSelectBest(device: DiscoveredDevice): KtmTransportType {
        // Step 1: Wi-Fi LAN Probe
        val lanReachable = probeWifiLanReachability(device.ipAddress, device.transferPort)
        device.isWifiLanReachable = lanReachable

        if (lanReachable && device.supportedTransports.contains("WIFI_LAN")) {
            device.bestTransport = "WIFI_LAN"
            device.activeTransport = "WIFI_LAN"
            return KtmTransportType.WIFI_LAN
        }

        // Step 2: Wi-Fi Direct
        if (isWifiDirectSupported && device.supportedTransports.contains("WIFI_DIRECT")) {
            device.bestTransport = "WIFI_DIRECT"
            device.activeTransport = "WIFI_DIRECT"
            return KtmTransportType.WIFI_DIRECT
        }

        // Step 3: Bluetooth Fallback
        if (isBluetoothSupported && device.supportedTransports.contains("BLUETOOTH")) {
            device.bestTransport = "BLUETOOTH"
            device.activeTransport = "BLUETOOTH"
            return KtmTransportType.BLUETOOTH
        }

        // Default fallback to Wi-Fi LAN
        device.bestTransport = "WIFI_LAN"
        device.activeTransport = "WIFI_LAN"
        return KtmTransportType.WIFI_LAN
    }

    companion object {
        @Volatile
        private var instance: KtmTransportManager? = null

        fun getInstance(context: Context): KtmTransportManager {
            return instance ?: synchronized(this) {
                instance ?: KtmTransportManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
