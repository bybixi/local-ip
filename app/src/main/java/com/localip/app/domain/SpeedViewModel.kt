package com.localip.app.domain

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.localip.app.core.iperf.IperfClientEvent
import com.localip.app.core.iperf.IperfConfig
import com.localip.app.core.iperf.IperfDirection
import com.localip.app.core.iperf.IperfProgress
import com.localip.app.core.iperf.IperfProtocol
import com.localip.app.core.iperf.IperfResult
import com.localip.app.core.iperf.IperfRunner
import com.localip.app.core.iperf.IperfServerEvent
import com.localip.app.core.net.NetState
import com.localip.app.core.net.NetworkMonitor
import com.localip.app.core.util.runSuspendCatching
import com.localip.app.data.db.AppDatabase
import com.localip.app.data.db.SpeedRecord
import com.localip.app.data.prefs.AppSettings
import com.localip.app.data.prefs.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 页面角色：客户端（去连别人）/ 服务端（等别人来连） */
enum class SpeedRole { CLIENT, SERVER }

enum class SpeedFailure { NO_BINARY, NEED_HOST, RUNNER_ERROR }

sealed interface SpeedUiState {
    data object Idle : SpeedUiState
    data class Running(val progress: IperfProgress, val config: IperfConfig) : SpeedUiState
    data class Done(val result: IperfResult) : SpeedUiState
    data class Failed(val reason: SpeedFailure, val detail: String? = null) : SpeedUiState
}

sealed interface ServerUiState {
    data object Stopped : ServerUiState
    data class Listening(val port: Int, val clients: Int, val lastMbps: Double?) : ServerUiState
    data class Failed(val reason: SpeedFailure, val detail: String? = null) : ServerUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class SpeedViewModel(app: Application) : AndroidViewModel(app) {

    private val runner = IperfRunner(app)
    private val settingsRepo = SettingsRepository(app)
    private val speedDao = AppDatabase.get(app).speeds()
    private val monitor = NetworkMonitor(app)

    private val _role = MutableStateFlow(SpeedRole.CLIENT)
    private val _client = MutableStateFlow<SpeedUiState>(SpeedUiState.Idle)
    private val _server = MutableStateFlow<ServerUiState>(ServerUiState.Stopped)
    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 4)

    private var clientJob: Job? = null
    private var serverJob: Job? = null

    val role: StateFlow<SpeedRole> = _role.asStateFlow()
    val client: StateFlow<SpeedUiState> = _client.asStateFlow()
    val server: StateFlow<ServerUiState> = _server.asStateFlow()
    val messages: SharedFlow<String> = _message

    val settings: StateFlow<AppSettings> = settingsRepo.flow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    /** 服务端模式要展示本机 IP，复用同一套网络监听 */
    val net: StateFlow<NetState> = monitor.observe()
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NetState())

    val history: StateFlow<List<SpeedRecord>> = speedDao.observe(SPEED_HISTORY_LIMIT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val binaryAvailable: Boolean get() = runner.isAvailable

    // ------------------------------------------------------------------ 操作

    fun setRole(value: SpeedRole) {
        if (value == _role.value) return
        _role.value = value
        // 切走时把另一侧运行的进程停掉，避免后台残留
        if (value == SpeedRole.CLIENT) stopServer() else stopTest()
    }

    fun startTest() {
        val cfg = settings.value
        val host = cfg.speedHost.trim()
        if (host.isBlank()) {
            _client.value = SpeedUiState.Failed(SpeedFailure.NEED_HOST)
            return
        }
        if (!runner.isAvailable) {
            _client.value = SpeedUiState.Failed(SpeedFailure.NO_BINARY)
            return
        }

        clientJob?.cancel()
        val config = cfg.toIperfConfig(host)
        _client.value = SpeedUiState.Running(
            progress = IperfProgress(totalSec = config.durationSec),
            config = config,
        )

        clientJob = viewModelScope.launch {
            runner.runClient(config)
                // 兜底：runner 里已吞掉 IOException，这里再保一层，
                // 保证任何异常都变成 Failed 状态而不是把 App 搞崩。
                // Flow.catch 不会捕获 CancellationException，取消语义不受影响。
                .catch { t ->
                    _client.value = SpeedUiState.Failed(SpeedFailure.RUNNER_ERROR, t.message)
                }
                .collect { event ->
                when (event) {
                    is IperfClientEvent.Progress ->
                        _client.value = SpeedUiState.Running(event.progress, config)

                    is IperfClientEvent.Finished -> {
                        _client.value = SpeedUiState.Done(event.result)
                        persist(event.result)
                    }

                    is IperfClientEvent.Failed ->
                        _client.value = SpeedUiState.Failed(
                            SpeedFailure.RUNNER_ERROR,
                            event.message,
                        )
                }
            }
        }
    }

    fun stopTest() {
        runner.stop()
        clientJob?.cancel()
        clientJob = null
        if (_client.value is SpeedUiState.Running) _client.value = SpeedUiState.Idle
    }

    fun resetTest() {
        stopTest()
        _client.value = SpeedUiState.Idle
    }

    fun startServer() {
        if (!runner.isAvailable) {
            _server.value = ServerUiState.Failed(SpeedFailure.NO_BINARY)
            return
        }
        val port = settings.value.speedPort
        serverJob?.cancel()

        serverJob = viewModelScope.launch {
            var clients = 0
            var lastMbps: Double? = null
            runner.runServer(port)
                .catch { t ->
                    _server.value = ServerUiState.Failed(SpeedFailure.RUNNER_ERROR, t.message)
                }
                .collect { event ->
                when (event) {
                    is IperfServerEvent.Started ->
                        _server.value = ServerUiState.Listening(port, clients, lastMbps)

                    is IperfServerEvent.ClientConnected -> {
                        clients += 1
                        _server.value = ServerUiState.Listening(port, clients, lastMbps)
                    }

                    is IperfServerEvent.TestFinished -> {
                        lastMbps = event.mbps
                        _server.value = ServerUiState.Listening(port, clients, lastMbps)
                        persistServerResult(port, event.mbps)
                    }

                    is IperfServerEvent.Failed ->
                        _server.value = ServerUiState.Failed(
                            SpeedFailure.RUNNER_ERROR,
                            event.message,
                        )
                }
            }
            // 进程退出：如果不是失败态，就回到"已停止"
            if (_server.value !is ServerUiState.Failed) _server.value = ServerUiState.Stopped
        }
    }

    fun stopServer() {
        runner.stop()
        serverJob?.cancel()
        serverJob = null
        if (_server.value is ServerUiState.Listening) _server.value = ServerUiState.Stopped
    }

    fun toast(text: String) = _message.tryEmit(text)

    // ------------------------------------------------------------------ 偏好写入

    fun setSpeedHost(v: String) = viewModelScope.launch { settingsRepo.setSpeedHost(v) }
    fun setSpeedPort(v: Int) = viewModelScope.launch { settingsRepo.setSpeedPort(v) }
    fun setSpeedProtocol(v: String) = viewModelScope.launch { settingsRepo.setSpeedProtocol(v) }
    fun setSpeedDirection(v: String) = viewModelScope.launch { settingsRepo.setSpeedDirection(v) }
    fun setSpeedDuration(v: Int) = viewModelScope.launch { settingsRepo.setSpeedDuration(v) }
    fun setSpeedStreams(v: Int) = viewModelScope.launch { settingsRepo.setSpeedStreams(v) }

    // ------------------------------------------------------------------ 内部

    private fun persist(result: IperfResult) {
        viewModelScope.launch {
            runSuspendCatching {
                speedDao.insert(
                    SpeedRecord(
                        host = result.host,
                        port = result.port,
                        protocol = result.protocol.name,
                        direction = result.direction.name,
                        streams = result.streams,
                        mbps = result.mbps,
                        retransmits = result.retransmits,
                        jitterMs = result.jitterMs,
                        lostPackets = result.lostPackets,
                        timestamp = result.timestamp,
                    )
                )
                speedDao.trim(SPEED_HISTORY_LIMIT)
            }
        }
    }

    private fun persistServerResult(port: Int, mbps: Double?) {
        val value = mbps ?: return
        viewModelScope.launch {
            runSuspendCatching {
                speedDao.insert(
                    SpeedRecord(
                        // 服务端模式下没有"目标服务器"，用空串占位；UI 按 direction=SERVER 区分展示
                        host = "",
                        port = port,
                        protocol = IperfProtocol.TCP.name,
                        direction = "SERVER",
                        streams = 1,
                        mbps = value,
                        retransmits = 0,
                        jitterMs = null,
                        lostPackets = 0,
                        timestamp = System.currentTimeMillis(),
                    )
                )
                speedDao.trim(SPEED_HISTORY_LIMIT)
            }
        }
    }

    override fun onCleared() {
        runner.stop()
        clientJob?.cancel()
        serverJob?.cancel()
    }

    private companion object {
        const val SPEED_HISTORY_LIMIT = 20
    }
}

private fun AppSettings.toIperfConfig(host: String): IperfConfig = IperfConfig(
    host = host,
    port = speedPort,
    protocol = runCatching { IperfProtocol.valueOf(speedProtocol) }
        .getOrDefault(IperfProtocol.TCP),
    direction = runCatching { IperfDirection.valueOf(speedDirection) }
        .getOrDefault(IperfDirection.DOWNLOAD),
    durationSec = speedDuration,
    streams = speedStreams,
)
