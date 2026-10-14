package com.localip.app.core.net

/** 当前 IP 的来源接口类型 */
enum class NetSource { ETHERNET, WIFI, CELLULAR, USB_TETHER, NONE }

/**
 * 一次网络状态快照。
 *
 * [wiredPresent] 与 [wiredHasIpv4] 是把 7 屏串起来的两个正交信号：
 * - `wiredPresent`   系统里存在一块可用的有线网卡（插上了，**不管有没有拿到 IP**）
 * - `wiredHasIpv4`   该网卡已经拿到 IPv4
 *
 * 只看"默认网络"是不够的：插上网卡但 DHCP 还没完成时，系统默认网络**仍然是 Wi-Fi**，
 * 那样就永远进不了「检测中」这一态。
 */
data class NetState(
    val source: NetSource = NetSource.NONE,
    val ifaceName: String? = null,
    val ipv4: String? = null,
    val prefixLength: Int? = null,
    val ipv6: String? = null,
    val gateway: String? = null,
    val dns: List<String> = emptyList(),
    val mac: String? = null,
    val mtu: Int? = null,
    val linkSpeedMbps: Int? = null,
    val fullDuplex: Boolean = true,
    val adapterName: String? = null,
    val ssid: String? = null,
    val wiredPresent: Boolean = false,
    val wiredHasIpv4: Boolean = false,
) {
    val hasIp: Boolean get() = !ipv4.isNullOrBlank()

    /** /24 → 255.255.255.0 */
    val subnetMask: String? get() = prefixLength?.let { NetUtils.prefixToMask(it) }

    val dnsPrimary: String? get() = dns.getOrNull(0)
    val dnsSecondary: String? get() = dns.getOrNull(1)

    /** 来源是否为有线网卡 */
    val fromEthernet: Boolean get() = source == NetSource.ETHERNET
}
