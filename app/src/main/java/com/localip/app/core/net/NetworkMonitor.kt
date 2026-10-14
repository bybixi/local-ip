package com.localip.app.core.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.Inet6Address

/**
 * 网络状态监听 —— 整个 App 的心脏。
 *
 * 三个信号源合并成一条 Flow：
 *  ① 默认网络回调：拿"当前生效"的 IP / 网关 / DNS
 *  ② 以太网专项请求：网卡一插上就回调，**不等 DHCP** —— 这是进入「检测中」态的信号
 *  ③ 2 秒轮询兜底：部分 ROM 的 ConnectivityService 不上报未配好的以太网
 *
 * 另外提供 [refresh] 供 UI 上的「刷新」按钮立即触发一次读取。
 */
class NetworkMonitor(context: Context) {

    private val appContext = context.applicationContext
    private val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val manualRefresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** UI 上的「刷新」按钮调用它 */
    fun refresh() {
        manualRefresh.tryEmit(Unit)
    }

    fun observe(): Flow<NetState> = channelFlow {
        // 信号通道：系统回调 / 轮询 / 手动刷新只往这里发"该读一次了"的信号，
        // 实际的 read() 由下方消费协程统一执行 —— 保证它永远不在主线程跑
        //（系统回调是主线程打过来的，绝不能在里面读 /sys 文件和枚举网卡）。
        val signal = Channel<Unit>(capacity = Channel.CONFLATED)
        val push: () -> Unit = { signal.trySend(Unit) }

        val defaultCb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { push() }
            override fun onLost(network: Network) { push() }
            override fun onUnavailable() { push() }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { push() }
            override fun onLinkPropertiesChanged(network: Network, lp: LinkProperties) { push() }
        }

        val ethernetCb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { push() }
            override fun onLost(network: Network) { push() }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { push() }
            override fun onLinkPropertiesChanged(network: Network, lp: LinkProperties) { push() }
        }

        runCatching { cm.registerDefaultNetworkCallback(defaultCb) }
        runCatching {
            // 刻意不加 NET_CAPABILITY_INTERNET：否则"网卡在但还没配好"时不会上报
            cm.registerNetworkCallback(
                NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
                    .build(),
                ethernetCb,
            )
        }

        // 轮询兜底（协程在 flowOn(IO) 的上游作用域里运行）
        launch {
            while (true) {
                push()
                delay(POLL_INTERVAL_MS)
            }
        }

        // 唯一执行 read() 的地方
        launch {
            for (ignored in signal) {
                trySend(read())
            }
        }

        launch { manualRefresh.collect { push() } }

        awaitClose {
            signal.close()
            runCatching { cm.unregisterNetworkCallback(defaultCb) }
            runCatching { cm.unregisterNetworkCallback(ethernetCb) }
        }
    }.flowOn(Dispatchers.IO)

    // ---------------------------------------------------------------- 内部

    private class WiredInfo(
        val iface: String?,
        val lp: LinkProperties?,
        val caps: NetworkCapabilities?,
    )

    @Suppress("DEPRECATION")
    private fun findWired(): WiredInfo {
        var found: WiredInfo? = null
        runCatching {
            cm.allNetworks.forEach { n ->
                if (found != null) return@forEach
                val caps = cm.getNetworkCapabilities(n)
                val lp = cm.getLinkProperties(n)
                val name = lp?.interfaceName
                if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true ||
                    NetUtils.isWiredIface(name)
                ) {
                    found = WiredInfo(name, lp, caps)
                }
            }
        }
        if (found == null) {
            // 兜底：直接问系统网卡列表（有些 ROM 不上报未配好的以太网）
            val name = NetUtils.findUpWiredInterface()
            if (name != null) found = WiredInfo(name, null, null)
        }
        return found ?: WiredInfo(null, null, null)
    }

    private fun read(): NetState {
        val active = cm.activeNetwork
        val activeCaps = active?.let { cm.getNetworkCapabilities(it) }
        val activeLp = active?.let { cm.getLinkProperties(it) }

        val wired = findWired()
        val wiredPresent = wired.iface != null
        val wiredHasIpv4 = wired.lp?.linkAddresses?.any { it.address is Inet4Address } == true ||
            (wired.lp == null && NetUtils.ifaceHasIpv4(wired.iface))

        // 有线优先：插着网卡且拿到 IP 时以它为准（与系统默认评分行为一致）
        val usingWired = wiredPresent && wiredHasIpv4
        val lp = if (usingWired) (wired.lp ?: activeLp) else activeLp
        val caps = if (usingWired) (wired.caps ?: activeCaps) else activeCaps
        val iface = if (wiredPresent) wired.iface else lp?.interfaceName

        val v4 = lp?.linkAddresses?.firstOrNull { it.address is Inet4Address }
        val v6 = NetUtils.pickIpv6(
            lp?.linkAddresses
                ?.mapNotNull { it.address as? Inet6Address }
                ?.filter { !it.isLoopbackAddress }
                ?.mapNotNull { it.hostAddress }
                .orEmpty()
        )

        val source = if (usingWired) NetSource.ETHERNET else detectSource(caps, lp)

        // 型号：/sys 驱动名 → USB VID/PID → 接口名。Android 10+ 起 /sys/class/net 常被 SELinux 拦，
        // 所以 VID/PID 那条路才是主力兜底。非有线接口不给型号（避免 Wi-Fi 显示成 "wlan0"）。
        val isWiredIface = NetUtils.isWiredIface(iface)
        val adapter = if (isWiredIface) {
            NetUtils.adapterNameFromSys(iface)
                ?: NetUtils.usbEthernetName(appContext)
                ?: iface
        } else {
            NetUtils.adapterNameFromSys(iface)
        }

        // ---- 连接速率 ----
        // 三种来源的语义完全不同，不能混用：
        //  · 以太网 → /sys/class/net/<iface>/speed 是协商速率（但常被 SELinux 拦），
        //             退而用 NetworkCapabilities 的 LinkDnBandwidth（以太网这个值就是协商速率）
        //  · Wi-Fi  → 必须走 WifiInfo。NetworkCapabilities 对 Wi-Fi 给的是**估算可用带宽**，
        //             会明显偏低（实测 1200Mbps 的链路只报 194Mbps），和系统设置里显示的对不上
        val speed = when {
            source == NetSource.ETHERNET ->
                NetUtils.linkSpeedMbps(iface) ?: caps.linkSpeedMbps()
            source == NetSource.WIFI ->
                wifiLinkSpeedMbps() ?: caps.linkSpeedMbps()
            else -> caps.linkSpeedMbps()
        }

        // LinkProperties.getMtu() 在部分机型上返回 0，那不是有效值
        val mtu = lp?.mtu?.takeIf { it > 0 } ?: NetUtils.mtu(iface)

        return NetState(
            source = source,
            ifaceName = iface,
            ipv4 = v4?.address?.hostAddress,
            prefixLength = v4?.prefixLength,
            ipv6 = v6,
            gateway = lp?.routes?.firstOrNull { it.isDefaultRoute }?.gateway?.hostAddress,
            dns = lp?.dnsServers?.mapNotNull { it.hostAddress }.orEmpty(),
            mac = NetUtils.macOf(iface),
            mtu = mtu,
            linkSpeedMbps = speed,
            fullDuplex = source == NetSource.ETHERNET,
            adapterName = adapter,
            ssid = currentSsid(),
            wiredPresent = wiredPresent,
            wiredHasIpv4 = wiredHasIpv4,
        )
    }

    /** 两层判断：先看 transport 标志位，再用接口名兜底 */
    private fun detectSource(caps: NetworkCapabilities?, lp: LinkProperties?): NetSource {
        if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true) {
            return NetSource.ETHERNET
        }
        val iface = lp?.interfaceName.orEmpty()
        if (NetUtils.isWiredIface(iface)) return NetSource.ETHERNET

        return when {
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> NetSource.WIFI
            caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> NetSource.CELLULAR
            else -> NetSource.NONE
        }
    }

    /**
     * Wi-Fi 的射频协商速率（与系统设置里显示的同一个数据源）。
     *
     * Android 10+ 优先用 tx/rx 分开的字段；老版本退回已废弃的 linkSpeed。
     * 注意：这几个字段**不需要定位权限**（被抹掉的是 SSID/BSSID）。
     */
    @Suppress("DEPRECATION")
    private fun wifiLinkSpeedMbps(): Int? = runCatching {
        val wm = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            ?: return@runCatching null
        val info = wm.connectionInfo ?: return@runCatching null
        val legacy = info.linkSpeed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listOf(info.txLinkSpeedMbps, info.rxLinkSpeedMbps, legacy).firstOrNull { it > 0 }
        } else {
            legacy.takeIf { it > 0 }
        }
    }.getOrNull()

    /**
     * Wi-Fi 名称。Android 10+ 需要定位权限，13+ 还要 NEARBY_WIFI_DEVICES。
     * 本 App 不主动弹权限框（不为一个小工具索要定位），拿不到就返回 null，
     * UI 退化成只显示 "WLAN" —— 设计稿本来就是这个约定。
     */
    @Suppress("DEPRECATION")
    private fun currentSsid(): String? = runCatching {
        val wm = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
        val raw = wm.connectionInfo?.ssid ?: return null
        if (raw.isBlank() || raw == "<unknown ssid>") null else raw.trim('"')
    }.getOrNull()

    private companion object {
        const val POLL_INTERVAL_MS = 2_000L
    }
}

/**
 * NetworkCapabilities 上报的链路带宽（Kbps）→ Mbps。
 *
 * ⚠️ 语义要分清：对**以太网**这个值就是协商速率；
 * 对 **Wi-Fi** 它只是"估算可用带宽"（按信号质量估的吞吐），会比射频协商速率低很多，
 * 所以 Wi-Fi 分支只在拿不到 WifiInfo 时才用它兜底。
 */
private fun NetworkCapabilities?.linkSpeedMbps(): Int? =
    this?.linkDownstreamBandwidthKbps?.takeIf { it in 1..1_000_000 }?.div(1000)
