package com.knowtomigrate.app.network

import android.content.Context
import android.net.wifi.WifiManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap

class KtmDiscoveryService(
    private val context: Context,
    private val localDeviceId: String,
    private val localDeviceName: String
) {
    private var scope: CoroutineScope? = null
    private var listenerSocket: DatagramSocket? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private val deviceMap = ConcurrentHashMap<String, DiscoveredDevice>()

    private val _devices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val devices: StateFlow<List<DiscoveredDevice>> = _devices.asStateFlow()

    fun start() {
        if (scope != null) return
        val handler = CoroutineExceptionHandler { _, t ->
            android.util.Log.w("KtmDiscovery", "Handled discovery error", t)
        }
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + handler)

        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            multicastLock = wifi?.createMulticastLock("KtmMulticastLock")?.apply {
                setReferenceCounted(true)
                acquire()
            }
        } catch (_: Throwable) {
            // Ignore if lock fails on cellular or unsupported devices
        }

        startListener()
        startBroadcaster()
        startPruner()
    }

    fun stop() {
        scope?.cancel()
        scope = null
        try { listenerSocket?.close() } catch (_: Throwable) {}
        listenerSocket = null
        try {
            if (multicastLock?.isHeld == true) multicastLock?.release()
        } catch (_: Throwable) {}
        multicastLock = null
    }

    private fun startListener() {
        scope?.launch {
            try {
                listenerSocket = DatagramSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                    bind(java.net.InetSocketAddress(KtmConstants.DISCOVERY_PORT))
                }
                val buffer = ByteArray(4096)

                while (isActive) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        listenerSocket?.receive(packet)
                    } catch (_: Throwable) {
                        break
                    }

                    try {
                        val json = String(packet.data, 0, packet.length, Charsets.UTF_8)
                        if (json.contains(KtmConstants.DISCOVERY_MAGIC)) {
                            val senderIp = packet.address?.hostAddress ?: ""
                            val device = DiscoveredDevice.fromJson(json, senderIp)
                            if (device != null && device.deviceId != localDeviceId) {
                                deviceMap[device.deviceId] = device
                                updateDeviceList()
                            }
                        }
                    } catch (_: Throwable) {}
                }
            } catch (_: Throwable) {
                // Socket unavailable or port bound
            }
        }
    }

    private fun startBroadcaster() {
        scope?.launch {
            while (isActive) {
                try {
                    val localDev = DiscoveredDevice(
                        deviceId = localDeviceId,
                        deviceName = localDeviceName,
                        platform = "Android",
                        transferPort = KtmConstants.TRANSFER_PORT,
                        version = "1.0.0"
                    )
                    val jsonBytes = localDev.toJson().toByteArray(Charsets.UTF_8)
                    val broadcastAddr = InetAddress.getByName("255.255.255.255")

                    DatagramSocket().use { socket ->
                        socket.broadcast = true
                        val packet = DatagramPacket(jsonBytes, jsonBytes.size, broadcastAddr, KtmConstants.DISCOVERY_PORT)
                        socket.send(packet)
                    }
                } catch (_: Throwable) {
                    // Ignore transient network errors on cellular/offline
                }
                delay(KtmConstants.DISCOVERY_INTERVAL_MS)
            }
        }
    }

    private fun startPruner() {
        scope?.launch {
            while (isActive) {
                try {
                    delay(2000L)
                    val now = System.currentTimeMillis()
                    var changed = false
                    val iterator = deviceMap.entries.iterator()
                    while (iterator.hasNext()) {
                        val entry = iterator.next()
                        if (now - entry.value.lastSeen > KtmConstants.DEVICE_TIMEOUT_MS) {
                            iterator.remove()
                            changed = true
                        }
                    }
                    if (changed) {
                        updateDeviceList()
                    }
                } catch (_: Throwable) {}
            }
        }
    }

    fun addManualDevice(ipAddress: String, port: Int = KtmConstants.TRANSFER_PORT, name: String = "Manual Device") {
        val key = "manual_${ipAddress}_$port"
        val dev = DiscoveredDevice(
            deviceId = key,
            deviceName = name,
            platform = "Remote IP",
            transferPort = port,
            ipAddress = ipAddress,
            lastSeen = System.currentTimeMillis()
        )
        deviceMap[key] = dev
        updateDeviceList()
    }

    private fun updateDeviceList() {
        _devices.value = deviceMap.values.filter { it.isOnline }
    }
}
