package com.localip.app.core.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * 网关探测 —— 替代设计稿里超出核心范围的「测速」。
 *
 * 主路径用系统的 `/system/bin/ping`（ICMP 最可靠）；
 * 失败再退回 [InetAddress.isReachable]。
 */
object GatewayPing {

    data class Result(
        val host: String,
        val avgMs: Double,
        val minMs: Double,
        val sent: Int,
        val received: Int,
    ) {
        val lost: Int get() = sent - received
        fun avgText(): String = String.format(java.util.Locale.US, "%.1f", avgMs)
        fun minText(): String = String.format(java.util.Locale.US, "%.1f", minMs)
    }

    private const val PING_BIN = "/system/bin/ping"

    suspend fun ping(host: String, count: Int = 3, waitSec: Int = 2): Result? =
        withContext(Dispatchers.IO) {
            runPing(host, count, waitSec) ?: runReachable(host, count, waitSec)
        }

    private fun runPing(host: String, count: Int, waitSec: Int): Result? = runCatching {
        if (!File(PING_BIN).exists()) return null
        val process = ProcessBuilder(
            PING_BIN, "-c", count.toString(), "-W", waitSec.toString(), host,
        ).redirectErrorStream(true).start()

        val output = process.inputStream.bufferedReader().use { it.readText() }
        if (!process.waitFor(15, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            return null
        }

        // 每个成功回包都会打印 "time=1.23 ms"，用它与语言无关地统计收包数
        val times = Regex("time[=<]([0-9.]+)\\s*ms")
            .findAll(output)
            .mapNotNull { it.groupValues[1].toDoubleOrNull() }
            .toList()

        if (times.isEmpty()) null
        else Result(
            host = host,
            avgMs = times.average(),
            minMs = times.min(),
            sent = count,
            received = times.size,
        )
    }.getOrNull()

    private fun runReachable(host: String, count: Int, waitSec: Int): Result? = runCatching {
        val addr = InetAddress.getByName(host)
        val timeout = waitSec * 1000 + 500
        val samples = ArrayList<Double>(count)
        repeat(count) {
            val start = System.nanoTime()
            val ok = addr.isReachable(timeout)
            if (ok) samples += (System.nanoTime() - start) / 1_000_000.0
        }
        if (samples.isEmpty()) null
        else Result(
            host = host,
            avgMs = samples.average(),
            minMs = samples.min(),
            sent = count,
            received = samples.size,
        )
    }.getOrNull()
}
