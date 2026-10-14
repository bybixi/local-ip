package com.localip.app.domain

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.localip.app.R
import com.localip.app.core.net.GatewayPing
import com.localip.app.core.net.NetSource
import com.localip.app.core.net.NetState
import com.localip.app.core.net.NetworkMonitor
import com.localip.app.core.util.runSuspendCatching
import com.localip.app.data.db.AppDatabase
import com.localip.app.data.db.ConnRecord
import com.localip.app.data.prefs.AppSettings
import com.localip.app.data.prefs.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 网关探测的 UI 状态 */
sealed interface PingUi {
    data object Running : PingUi
    data class Done(val result: GatewayPing.Result) : PingUi
    data class Failed(val host: String) : PingUi
    data object NoGateway : PingUi
}

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val monitor = NetworkMonitor(app)
    private val settingsRepo = SettingsRepository(app)
    private val dao = AppDatabase.get(app).records()

    private val _detectStartedAt = MutableStateFlow<Long?>(null)
    private val _now = MutableStateFlow(System.currentTimeMillis())
    private val _showClearDialog = MutableStateFlow(false)
    private val _ping = MutableStateFlow<PingUi?>(null)
    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 4)
    private var pingJob: Job? = null

    /** 一次性提示（Toast） */
    val messages: SharedFlow<String> = _message
    val showClearDialog: StateFlow<Boolean> = _showClearDialog.asStateFlow()
    val ping: StateFlow<PingUi?> = _ping.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepo.flow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    /**
     * 网络状态。
     * 侧效应（写历史 / 自动复制 / 通知）挂在共享上游的 onEach 上 ——
     * 桌面进入后台后 UI 不再订阅，WhileSubscribed 到点会自动停掉 2 秒轮询，不耗电。
     */
    val net: StateFlow<NetState> = monitor.observe()
        .distinctUntilChanged()
        .onEach { state -> maybeRecord(state) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NetState())

    val records: StateFlow<List<ConnRecord>> = settings
        .map { it.recordLimit }
        .distinctUntilChanged()
        .flatMapLatest { limit -> dao.observe(limit) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val overview: StateFlow<OverviewState> =
        combine(net, _detectStartedAt, _now) { n, startedAt, now ->
            OverviewState.decide(n, startedAt, now)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            OverviewState.Disconnected(NetState()),
        )

    init {
        // 记录「进入检测中」的时刻，用于 20 秒超时提示
        viewModelScope.launch {
            net.map { it.wiredPresent && !it.wiredHasIpv4 }
                .distinctUntilChanged()
                .collect { detecting ->
                    _detectStartedAt.value = if (detecting) System.currentTimeMillis() else null
                }
        }
        // 检测中时每秒推动一次 now
        viewModelScope.launch {
            while (true) {
                if (_detectStartedAt.value != null) _now.value = System.currentTimeMillis()
                delay(1_000)
            }
        }
    }

    // ------------------------------------------------------------ 历史记录

    private suspend fun maybeRecord(state: NetState) {
        if (!settings.value.autoDetect) return
        val ip = state.ipv4 ?: return

        // 只有"连接标识"发生变化才写记录。
        // 这个标识被持久化在 DataStore 里 —— 否则每次冷启动 App 都会把当前连接重复记一遍。
        val key = "$ip|${state.source}|${state.ifaceName.orEmpty()}"
        if (key == settingsRepo.lastRecordKey.first()) return
        settingsRepo.setLastRecordKey(key)

        val cfg = settings.value
        runSuspendCatching {
            dao.insert(
                ConnRecord(
                    ip = ip,
                    sourceKey = state.source.name,
                    detail = detailOf(state),
                    ifaceName = state.ifaceName,
                    timestamp = System.currentTimeMillis(),
                )
            )
            dao.trim(cfg.recordLimit)
        }
        if (cfg.autoCopyIp) {
            copyToClipboard(ip)
        }
        if (cfg.notifyOnConnect) {
            notifyIp(state)
        }
    }

    private fun detailOf(state: NetState): String? = when (state.source) {
        NetSource.ETHERNET -> state.adapterName
        NetSource.WIFI -> state.ssid
        else -> state.ifaceName
    }

    // ------------------------------------------------------------ 用户操作

    fun refresh() = monitor.refresh()

    fun copyIp(ip: String? = net.value.ipv4) {
        if (ip.isNullOrBlank()) {
            _message.tryEmit(str(R.string.toast_no_ip))
            return
        }
        copyToClipboard(ip)
        _message.tryEmit(str(R.string.toast_copied))
    }

    fun shareText(): String {
        val s = net.value
        val source = sourceLabel(s.source)
        return buildString {
            append(str(R.string.ip_label)).append("：").append(s.ipv4 ?: "—").append('\n')
            append(str(R.string.source_prefix)).append(source)
            if (s.source == NetSource.ETHERNET && !s.adapterName.isNullOrBlank()) {
                append(" · ").append(s.adapterName)
            }
            if (!s.gateway.isNullOrBlank()) {
                append('\n').append(str(R.string.row_gateway)).append("：").append(s.gateway)
            }
            if (!s.subnetMask.isNullOrBlank()) {
                append('\n').append(str(R.string.row_mask)).append("：").append(s.subnetMask)
            }
            if (!s.dnsPrimary.isNullOrBlank()) {
                append('\n').append(str(R.string.row_dns)).append("：").append(s.dns.joinToString(", "))
            }
        }
    }

    fun openClearDialog() { _showClearDialog.value = true }
    fun closeClearDialog() { _showClearDialog.value = false }

    fun confirmClear() {
        viewModelScope.launch {
            runSuspendCatching { dao.clear() }
            // 把去重游标设成"当前连接"，这样刚清完不会立刻又插回一条
            val s = net.value
            val current = s.ipv4?.let { "$it|${s.source}|${s.ifaceName.orEmpty()}" }.orEmpty()
            settingsRepo.setLastRecordKey(current)
            _showClearDialog.value = false
            _message.tryEmit(str(R.string.toast_cleared))
        }
    }

    fun runGatewayPing() {
        val gw = net.value.gateway
        if (gw.isNullOrBlank()) {
            _ping.value = PingUi.NoGateway
            return
        }
        _ping.value = PingUi.Running
        pingJob?.cancel()
        pingJob = viewModelScope.launch {
            val result = withTimeoutOrNull(15_000L) { GatewayPing.ping(gw, 3, 2) }
            _ping.value = if (result == null) PingUi.Failed(gw) else PingUi.Done(result)
        }
    }

    fun dismissPing() {
        pingJob?.cancel()
        _ping.value = null
    }

    fun setAutoDetect(v: Boolean) = viewModelScope.launch { settingsRepo.setAutoDetect(v) }
    fun setNotifyOnConnect(v: Boolean) = viewModelScope.launch { settingsRepo.setNotifyOnConnect(v) }
    fun setAutoCopyIp(v: Boolean) = viewModelScope.launch { settingsRepo.setAutoCopyIp(v) }
    fun setIpFormat(v: String) = viewModelScope.launch { settingsRepo.setIpFormat(v) }
    fun setRecordLimit(v: Int) = viewModelScope.launch { settingsRepo.setRecordLimit(v) }

    /** 导出 CSV，回调返回可分享的 Uri（失败为 null） */
    fun exportCsv(onReady: (Uri?) -> Unit) {
        viewModelScope.launch {
            val uri = withContext(Dispatchers.IO) { writeCsv() }
            onReady(uri)
        }
    }

    fun toast(text: String) { _message.tryEmit(text) }

    // ------------------------------------------------------------ 工具

    private fun str(id: Int, vararg args: Any): String =
        getApplication<Application>().getString(id, *args)

    fun sourceLabel(source: NetSource): String = str(
        when (source) {
            NetSource.ETHERNET -> R.string.source_ethernet
            NetSource.WIFI -> R.string.source_wlan
            NetSource.CELLULAR -> R.string.source_cellular
            NetSource.USB_TETHER -> R.string.source_usb_tether
            NetSource.NONE -> R.string.source_none
        }
    )

    private fun copyToClipboard(text: String) {
        runCatching {
            val ctx = getApplication<Application>()
            val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("IP", text))
        }
    }

    private fun notifyIp(state: NetState) {
        val ctx = getApplication<Application>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        runCatching {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        ctx.getString(R.string.notif_channel_name),
                        NotificationManager.IMPORTANCE_DEFAULT,
                    ).apply { description = ctx.getString(R.string.notif_channel_desc) }
                )
            }
            val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_ip)
                .setContentTitle(ctx.getString(R.string.notif_title))
                .setContentText(
                    ctx.getString(
                        R.string.notif_text,
                        sourceLabel(state.source),
                        state.ipv4.orEmpty(),
                    )
                )
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            nm.notify(NOTIF_ID, notification)
        }
    }

    private suspend fun writeCsv(): Uri? = runCatching {
        val ctx = getApplication<Application>()
        val dir = File(ctx.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "localip-records.csv")
        val rows = dao.all()
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        file.bufferedWriter(Charsets.UTF_8).use { w ->
            w.write("\uFEFF") // BOM：Excel 打开中文不乱码
            w.write("IP,来源,详情,接口,时间\n")
            rows.forEach { r ->
                w.write(
                    listOf(
                        r.ip,
                        sourceLabel(runCatching { NetSource.valueOf(r.sourceKey) }.getOrDefault(NetSource.NONE)),
                        r.detail.orEmpty(),
                        r.ifaceName.orEmpty(),
                        time.format(Date(r.timestamp)),
                    ).joinToString(",") { csvEscape(it) }
                )
                w.write("\n")
            }
        }
        FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    }.getOrNull()

    private fun csvEscape(value: String): String =
        if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    private companion object {
        const val CHANNEL_ID = "localip_network"
        const val NOTIF_ID = 1001
    }
}
