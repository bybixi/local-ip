package com.localip.app.core.iperf

import java.util.Locale
import kotlin.math.pow

enum class IperfProtocol { TCP, UDP }

/** 相对客户端的流向。iperf3 里"下载"= 客户端接收（-R），"上传"= 客户端发送 */
enum class IperfDirection { DOWNLOAD, UPLOAD, BOTH }

data class IperfConfig(
    val host: String = "",
    val port: Int = 5201,
    val protocol: IperfProtocol = IperfProtocol.TCP,
    val direction: IperfDirection = IperfDirection.DOWNLOAD,
    val durationSec: Int = 10,
    val streams: Int = 4,
)

/** 每秒一条的实时进度 */
data class IperfProgress(
    val elapsedSec: Double = 0.0,
    val totalSec: Int = 10,
    val mbps: Double = 0.0,
    val transferBytes: Double = 0.0,
    val retransmits: Int = 0,
    val jitterMs: Double? = null,
    val lostPackets: Int = 0,
    val totalPackets: Int = 0,
) {
    val fraction: Float
        get() = if (totalSec <= 0) 0f else (elapsedSec / totalSec).toFloat().coerceIn(0f, 1f)

    val remainingSec: Int get() = (totalSec - elapsedSec).toInt().coerceAtLeast(0)
}

/** 一次测速的最终结果（也用于落库） */
data class IperfResult(
    val host: String,
    val port: Int,
    val protocol: IperfProtocol,
    val direction: IperfDirection,
    val streams: Int,
    /** 本次方向上的实际带宽（Mbps） */
    val mbps: Double,
    /** 双向测试时另一个方向 */
    val mbpsReverse: Double? = null,
    val retransmits: Int = 0,
    val jitterMs: Double? = null,
    val lostPackets: Int = 0,
    val totalPackets: Int = 0,
    val durationSec: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
)

sealed interface IperfClientEvent {
    data class Progress(val progress: IperfProgress) : IperfClientEvent
    data class Finished(val result: IperfResult) : IperfClientEvent
    data class Failed(val message: String) : IperfClientEvent
}

sealed interface IperfServerEvent {
    data object Started : IperfServerEvent
    data class ClientConnected(val client: String) : IperfServerEvent
    data class TestFinished(val mbps: Double?) : IperfServerEvent
    data class Failed(val message: String) : IperfServerEvent
}

// ---------------------------------------------------------------------------
// 文本解析：iperf3 的每行输出
// ---------------------------------------------------------------------------

/**
 * 匹配 iperf3 的区间行，例如：
 * ```
 * [SUM]  0.00-1.00   sec   115 MBytes   964 Mbits/sec    0             sender
 * [  4]   0.00-1.00   sec  1.09 MBytes  9.13 Mbits/sec  0.017 ms  0/1000 (0%)
 * ```
 */
private val INTERVAL_REGEX = Regex(
    "^\\[\\s*(SUM|\\d+)\\]\\s+" +          // [SUM] / [  4]
        "([\\d.]+)-(\\d+(?:\\.\\d+)?)\\s+sec\\s+" +
        "([\\d.]+)\\s+([KMGT]?)Bytes\\s+" +
        "([\\d.]+)\\s+([KMGT]?)bits/sec" +
        "(.*)$"
)

private val JITTER_LOSS_REGEX = Regex("([\\d.]+)\\s*ms\\s+(\\d+)/(\\d+)")

private val RETR_REGEX = Regex("^\\s+(\\d+)\\b")

internal object IperfOutput {

    fun unitFactor(prefix: String): Double = when (prefix) {
        "K" -> 1e3
        "M" -> 1e6
        "G" -> 1e9
        "T" -> 1e12
        else -> 1.0
    }

    /** 解析一行；不是区间行就返回 null */
    fun parseInterval(line: String): ParsedInterval? {
        val m = INTERVAL_REGEX.find(line.trim()) ?: return null
        val isSum = m.groupValues[1] == "SUM"
        val start = m.groupValues[2].toDoubleOrNull() ?: return null
        val end = m.groupValues[3].toDoubleOrNull() ?: return null
        val bytes = (m.groupValues[4].toDoubleOrNull() ?: return null) *
            unitFactor(m.groupValues[5])
        val bitrate = (m.groupValues[6].toDoubleOrNull() ?: return null) *
            unitFactor(m.groupValues[7])
        val tail = m.groupValues[8]

        val jitterLoss = JITTER_LOSS_REGEX.find(tail)
        val retr = if (jitterLoss == null) {
            RETR_REGEX.find(tail)?.groupValues?.get(1)?.toIntOrNull()
        } else {
            null
        }

        return ParsedInterval(
            isSum = isSum,
            startSec = start,
            endSec = end,
            bytes = bytes,
            mbps = bitrate / 1e6,
            retransmits = retr,
            jitterMs = jitterLoss?.groupValues?.get(1)?.toDoubleOrNull(),
            lostPackets = jitterLoss?.groupValues?.get(2)?.toIntOrNull(),
            totalPackets = jitterLoss?.groupValues?.get(3)?.toIntOrNull(),
            role = when {
                tail.contains("sender") -> "sender"
                tail.contains("receiver") -> "receiver"
                else -> null
            },
        )
    }

    /** 人类可读的字节数：3.42 GB */
    fun formatBytes(bytes: Double): String {
        if (bytes <= 0) return "0 B"
        val units = listOf("B", "KB", "MB", "GB", "TB")
        val i = (kotlin.math.ln(bytes) / kotlin.math.ln(1024.0)).toInt().coerceIn(0, units.lastIndex)
        val value = bytes / 1024.0.pow(i.toDouble())
        return if (i == 0) {
            "${value.toInt()} ${units[i]}"
        } else {
            String.format(Locale.US, "%.2f %s", value, units[i])
        }
    }
}

internal data class ParsedInterval(
    val isSum: Boolean,
    val startSec: Double,
    val endSec: Double,
    val bytes: Double,
    val mbps: Double,
    val retransmits: Int?,
    val jitterMs: Double?,
    val lostPackets: Int?,
    val totalPackets: Int?,
    val role: String?,
)
