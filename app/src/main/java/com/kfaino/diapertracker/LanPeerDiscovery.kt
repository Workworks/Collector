package com.kfaino.diapertracker

import android.os.Build
import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

data class LanPeer(
    val ip: String,
    val port: Int = 8848,
    val name: String = "Collecter 终端",
    val isDesktop: Boolean = false
)

/**
 * 局域网设备自发现引擎 (UDP Port 8849)
 *
 * 在同一家庭/办公 Wi-Fi 下免输入 IP 自动发现附近在线的 Collecter 移动端与桌面端。
 */
object LanPeerDiscovery {

    private const val TAG = "LanPeerDiscovery"
    private const val DISCOVERY_PORT = 8849
    private const val PING_MSG = "COLLECTER_DISCOVERY_PING"
    private const val PONG_PREFIX = "COLLECTER_DISCOVERY_PONG:"

    private var announcerSocket: DatagramSocket? = null
    private var isAnnouncing = false
    private val executor = Executors.newCachedThreadPool()

    /** 开启局域网在线广播应答服务 */
    @Synchronized
    fun startAnnouncer(deviceName: String = "${Build.MANUFACTURER} ${Build.MODEL}", httpPort: Int = 8848) {
        if (isAnnouncing) return
        try {
            announcerSocket = DatagramSocket(DISCOVERY_PORT).apply {
                broadcast = true
            }
            isAnnouncing = true

            executor.execute {
                val buf = ByteArray(512)
                val packet = DatagramPacket(buf, buf.size)
                while (isAnnouncing && announcerSocket != null) {
                    try {
                        announcerSocket!!.receive(packet)
                        val msg = String(packet.data, 0, packet.length, StandardCharsets.UTF_8).trim()
                        if (msg == PING_MSG) {
                            val pong = "$PONG_PREFIX$deviceName:$httpPort".toByteArray(StandardCharsets.UTF_8)
                            val respPacket = DatagramPacket(pong, pong.size, packet.address, packet.port)
                            announcerSocket!!.send(respPacket)
                        }
                    } catch (e: Exception) {
                        if (!isAnnouncing) break
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "启动局域网发现应答失败: ${e.message}")
        }
    }

    @Synchronized
    fun stopAnnouncer() {
        isAnnouncing = false
        try {
            announcerSocket?.close()
            announcerSocket = null
        } catch (e: Exception) {
            Log.w(TAG, "停止局域网应答异常: ${e.message}")
        }
    }

    /** 主动发起广播寻找附近设备 */
    fun findPeers(timeoutMs: Int = 2000, onPeerFound: (LanPeer) -> Unit) {
        executor.execute {
            var clientSocket: DatagramSocket? = null
            try {
                clientSocket = DatagramSocket().apply {
                    broadcast = true
                    soTimeout = timeoutMs
                }

                val ping = PING_MSG.toByteArray(StandardCharsets.UTF_8)
                val packet = DatagramPacket(ping, ping.size, InetAddress.getByName("255.255.255.255"), DISCOVERY_PORT)
                clientSocket.send(packet)

                val buf = ByteArray(512)
                val respPacket = DatagramPacket(buf, buf.size)
                val discoveredIps = mutableSetOf<String>()

                val startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < timeoutMs) {
                    try {
                        clientSocket.receive(respPacket)
                        val text = String(respPacket.data, 0, respPacket.length, StandardCharsets.UTF_8).trim()
                        if (text.startsWith(PONG_PREFIX)) {
                            val parts = text.substring(PONG_PREFIX.length).split(":")
                            val name = parts.getOrNull(0) ?: "未知设备"
                            val port = parts.getOrNull(1)?.toIntOrNull() ?: 8848
                            val ip = respPacket.address.hostAddress ?: continue

                            if (discoveredIps.add(ip)) {
                                val isDesktop = name.contains("Desktop", ignoreCase = true) || name.contains("PC", ignoreCase = true)
                                onPeerFound(LanPeer(ip = ip, port = port, name = name, isDesktop = isDesktop))
                            }
                        }
                    } catch (_: java.net.SocketTimeoutException) {
                        break
                    } catch (e: Exception) {
                        Log.w(TAG, "接收发现应答异常: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "发送局域网发现广播失败: ${e.message}")
            } finally {
                clientSocket?.close()
            }
        }
    }
}
