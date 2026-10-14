package com.localip.app.core.iperf

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * iperf3 执行器。
 *
 * 可执行文件以「原生库」的形式随 APK 分发（`jniLibs/arm64-v8a/libiperf3.so`），
 * 安装时被解压到 `nativeLibraryDir` —— 那里是安卓上少数允许 App 执行外部二进制的目录之一
 * （App 私有目录是 noexec 的，所以不能把二进制解到 filesDir 再执行）。
 */
class IperfRunner(context: Context) {

    private val appContext = context.applicationContext

    /** 当前运行中的 iperf3 进程，供 [stop] 强杀 */
    @Volatile
    private var currentProcess: Process? = null

    /**
     * 强制停止当前进程。destroy 会让阻塞在 readLine() 上的读线程立刻返回，
     * 从而触发 flow 的 finally 清理 —— 协程 cancel 本身叫不醒线程阻塞。
     */
    fun stop() {
        runCatching { currentProcess?.destroyForcibly() }
    }

    val binaryPath: String? by lazy {
        runCatching {
            val file = File(appContext.applicationInfo.nativeLibraryDir, BINARY_NAME)
            if (file.exists() && file.canExecute()) file.absolutePath else null
        }.getOrNull()
    }

    val isAvailable: Boolean get() = binaryPath != null

    // ------------------------------------------------------------------ 客户端

    fun runClient(config: IperfConfig): Flow<IperfClientEvent> = flow {
        val bin = binaryPath
        if (bin == null) {
            emit(IperfClientEvent.Failed("iperf3 binary not found"))
            return@flow
        }

        val process = runCatching {
            ProcessBuilder(buildClientArgs(bin, config))
                .redirectErrorStream(true)
                .start()
        }.getOrElse { throwable ->
            emit(IperfClientEvent.Failed(throwable.message ?: "exec failed"))
            return@flow
        }
        currentProcess = process

        var elapsed = 0.0
        var transferred = 0.0
        var retransmits = 0
        var jitter: Double? = null
        var lost = 0
        var totalPackets = 0
        var lastMbps = 0.0
        var previousSummaryMbps = 0.0
        val errors = StringBuilder()

        try {
            val reader = process.inputStream.bufferedReader()
            while (true) {
                val line = readLineOrNull(reader) ?: break
                val parsed = IperfOutput.parseInterval(line)

                if (parsed == null) {
                    val lower = line.lowercase()
                    if (lower.contains("error") || lower.contains("failed") ||
                        lower.contains("unable to")
                    ) {
                        errors.append(line.trim()).append('\n')
                    }
                    continue
                }

                // iperf3 对同一区间会先打 sender 行、后打 receiver 行；
                // 后面出现的覆盖前面，因此"最后一行"就是实际到达量。汇总行（0.00-total）才是累计值。
                val isSummary = parsed.startSec == 0.0 &&
                    parsed.endSec >= config.durationSec - 0.5

                if (parsed.endSec >= elapsed) {
                    elapsed = parsed.endSec
                    lastMbps = parsed.mbps
                }

                if (isSummary) {
                    if (lastMbps > 0) previousSummaryMbps = lastMbps
                    transferred = parsed.bytes
                    parsed.retransmits?.let { retransmits = it }
                    parsed.lostPackets?.let { lost = it }
                    parsed.totalPackets?.let { totalPackets = it }
                } else {
                    transferred += parsed.bytes
                    parsed.retransmits?.let { retransmits += it }
                    parsed.lostPackets?.let { lost += it }
                    parsed.totalPackets?.let { totalPackets += it }
                }
                parsed.jitterMs?.let { jitter = it }

                emit(
                    IperfClientEvent.Progress(
                        IperfProgress(
                            elapsedSec = elapsed,
                            totalSec = config.durationSec,
                            mbps = lastMbps,
                            transferBytes = transferred,
                            retransmits = retransmits,
                            jitterMs = jitter,
                            lostPackets = lost,
                            totalPackets = totalPackets,
                        )
                    )
                )
            }
            if (!process.waitFor(30, TimeUnit.SECONDS)) process.destroyForcibly()
        } finally {
            currentProcess = null
            runCatching { process.destroy() }
        }

        if (lastMbps <= 0.0) {
            val reason = errors.toString().trim().lineSequence().firstOrNull().orEmpty()
            emit(IperfClientEvent.Failed(reason.ifBlank { "iperf3 returned no result" }))
            return@flow
        }

        emit(
            IperfClientEvent.Finished(
                IperfResult(
                    host = config.host,
                    port = config.port,
                    protocol = config.protocol,
                    direction = config.direction,
                    streams = config.streams,
                    mbps = lastMbps,
                    mbpsReverse = if (config.direction == IperfDirection.BOTH) {
                        previousSummaryMbps.takeIf { it > 0.0 }
                    } else {
                        null
                    },
                    retransmits = retransmits,
                    jitterMs = jitter,
                    lostPackets = lost,
                    totalPackets = totalPackets,
                    durationSec = elapsed,
                )
            )
        )
    }.flowOn(Dispatchers.IO)

    private fun buildClientArgs(bin: String, config: IperfConfig): List<String> = buildList {
        add(bin)
        add("-c"); add(config.host)
        add("-p"); add(config.port.toString())
        add("-t"); add(config.durationSec.toString())
        add("-P"); add(config.streams.toString())
        if (config.protocol == IperfProtocol.UDP) add("-u")
        when (config.direction) {
            IperfDirection.UPLOAD -> Unit
            IperfDirection.DOWNLOAD -> add("-R")
            IperfDirection.BOTH -> add("--bidir")
        }
        // 让每一秒的输出立刻刷出来，否则 UI 拿不到实时进度
        add("--forceflush")
    }

    // ------------------------------------------------------------------ 服务端

    fun runServer(port: Int): Flow<IperfServerEvent> = flow {
        val bin = binaryPath
        if (bin == null) {
            emit(IperfServerEvent.Failed("iperf3 binary not found"))
            return@flow
        }

        val process = runCatching {
            ProcessBuilder(bin, "-s", "-p", port.toString(), "--forceflush")
                .redirectErrorStream(true)
                .start()
        }.getOrElse { throwable ->
            emit(IperfServerEvent.Failed(throwable.message ?: "exec failed"))
            return@flow
        }
        currentProcess = process

        var announced = false
        try {
            val reader = process.inputStream.bufferedReader()
            while (true) {
                val line = readLineOrNull(reader) ?: break
                when {
                    line.contains("Server listening") -> if (!announced) {
                        announced = true
                        emit(IperfServerEvent.Started)
                    }

                    line.startsWith("Accepted connection from") -> {
                        val client = line.removePrefix("Accepted connection from")
                            .trim()
                            .trimEnd(',')
                        emit(IperfServerEvent.ClientConnected(client))
                    }

                    else -> IperfOutput.parseInterval(line)?.let { interval ->
                        if (interval.role != "sender") {
                            emit(IperfServerEvent.TestFinished(interval.mbps))
                        }
                    }
                }
            }
            if (!process.waitFor(30, TimeUnit.SECONDS)) process.destroyForcibly()
            if (!announced) emit(IperfServerEvent.Failed("server did not start"))
        } finally {
            currentProcess = null
            runCatching { process.destroy() }
        }
    }.flowOn(Dispatchers.IO)

    private companion object {
        const val BINARY_NAME = "libiperf3.so"
    }
}

/**
 * 安全读一行。
 *
 * ⚠️ 关键：进程被 Process.destroyForcibly() 强杀时，**阻塞中的 read 抛的是
 * `InterruptedIOException`（"read interrupted"）而不是返回 null**。
 * 这是"用户点了停止"的正常路径，必须在最内层吞掉 —— 否则异常会穿透 flow 冒到
 * `viewModelScope.launch`，未捕获直接把整个 App 搞崩。
 *
 * 真机实测踩过：停止服务 → `FATAL EXCEPTION: main / java.io.InterruptedIOException: read interrupted`
 * → 堆栈指向 IperfRunner 的 readLine()。
 */
private fun readLineOrNull(reader: BufferedReader): String? = try {
    reader.readLine()
} catch (io: IOException) {
    null
}
