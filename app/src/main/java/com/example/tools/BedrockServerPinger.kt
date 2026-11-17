package com.example.tools

import com.example.model.SavedServer
import com.example.model.ServerPingResult
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.random.Random

object BedrockServerPinger {

    private const val ID_UNCONNECTED_PING: Byte = 0x01
    private const val ID_UNCONNECTED_PONG: Byte = 0x1C

    // Standard RakNet offline message magic (16 bytes)
    private val RAKNET_MAGIC = byteArrayOf(
        0x00, 0xFF.toByte(), 0xFF.toByte(), 0x00,
        0xFE.toByte(), 0xFE.toByte(), 0xFE.toByte(), 0xFE.toByte(),
        0xFD.toByte(), 0xFD.toByte(), 0xFD.toByte(), 0xFD.toByte(),
        0x12, 0x34, 0x56, 0x78
    )

    fun ping(server: SavedServer, timeoutMs: Int = 3500): ServerPingResult {
        return try {
            val address = InetAddress.getByName(server.host)
            val sendTime = System.currentTimeMillis()
            val packetBytes = ByteBuffer.allocate(33).order(ByteOrder.BIG_ENDIAN).apply {
                put(ID_UNCONNECTED_PING)
                putLong(sendTime)
                put(RAKNET_MAGIC)
                putLong(Random.nextLong())
            }.array()

            DatagramSocket().use { socket ->
                socket.soTimeout = timeoutMs
                val outPacket = DatagramPacket(packetBytes, packetBytes.size, address, server.port)
                val startNs = System.nanoTime()
                socket.send(outPacket)

                val recvBuf = ByteArray(4096)
                val inPacket = DatagramPacket(recvBuf, recvBuf.size)
                socket.receive(inPacket)
                val latencyMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)

                parseUnconnectedPong(
                    serverId = server.id,
                    data = inPacket.data,
                    length = inPacket.length,
                    latencyMs = latencyMs
                )
            }
        } catch (t: Throwable) {
            ServerPingResult(
                serverId = server.id,
                isOnline = false,
                motd = "",
                subMotd = "",
                versionName = "",
                onlinePlayers = 0,
                maxPlayers = 0,
                gameMode = "",
                latencyMs = 0L,
                errorMessage = "${t.javaClass.simpleName}: ${t.message}"
            )
        }
    }

    internal fun parseUnconnectedPong(
        serverId: String,
        data: ByteArray,
        length: Int,
        latencyMs: Long
    ): ServerPingResult {
        if (length < 35 || data[0] != ID_UNCONNECTED_PONG) {
            return ServerPingResult(
                serverId = serverId,
                isOnline = false,
                motd = "",
                subMotd = "",
                versionName = "",
                onlinePlayers = 0,
                maxPlayers = 0,
                gameMode = "",
                latencyMs = latencyMs,
                errorMessage = "Unexpected RakNet packet (len=$length, id=0x${(data.firstOrNull()?.toInt() ?: 0) and 0xFF})"
            )
        }

        // Layout: 1 (id) + 8 (time) + 8 (serverGuid) + 16 (magic) + 2 (stringLen) + string
        val buf = ByteBuffer.wrap(data, 33, length - 33).order(ByteOrder.BIG_ENDIAN)
        val strLen = buf.short.toInt() and 0xFFFF
        val actualLen = strLen.coerceAtMost(length - 35)
        val payload = String(data, 35, actualLen, Charsets.UTF_8)

        // Example: MCPE;Dedicated Server;594;1.20.10;0;10;13253860892328930865;Bedrock level;Survival;1;19132;19133;
        val parts = payload.split(';')
        val motd = parts.getOrNull(1).orEmpty().ifBlank { payload }
        val versionName = parts.getOrNull(3).orEmpty()
        val onlinePlayers = parts.getOrNull(4)?.toIntOrNull() ?: 0
        val maxPlayers = parts.getOrNull(5)?.toIntOrNull() ?: 0
        val subMotd = parts.getOrNull(7).orEmpty()
        val gameMode = parts.getOrNull(8).orEmpty()

        return ServerPingResult(
            serverId = serverId,
            isOnline = true,
            motd = cleanMinecraftFormatting(motd),
            subMotd = cleanMinecraftFormatting(subMotd),
            versionName = versionName,
            onlinePlayers = onlinePlayers,
            maxPlayers = maxPlayers,
            gameMode = gameMode,
            latencyMs = latencyMs,
            errorMessage = null
        )
    }

    private fun cleanMinecraftFormatting(raw: String): String {
        return raw.replace(Regex("§."), "").trim()
    }
}
