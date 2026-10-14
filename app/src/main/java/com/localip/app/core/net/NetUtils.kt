package com.localip.app.core.net

import android.content.Context
import android.hardware.usb.UsbManager
import java.io.File
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.NetworkInterface

/** 一些小工具：掩码换算、/sys 读值、接口名判定 */
object NetUtils {

    /** 前缀长度 → 点分十进制掩码。例如 24 → 255.255.255.0 */
    fun prefixToMask(prefix: Int): String {
        if (prefix <= 0) return "0.0.0.0"
        if (prefix >= 32) return "255.255.255.255"
        val mask = (0xFFFFFFFFL shl (32 - prefix)) and 0xFFFFFFFFL
        return buildString {
            append((mask shr 24) and 0xFF).append('.')
            append((mask shr 16) and 0xFF).append('.')
            append((mask shr 8) and 0xFF).append('.')
            append(mask and 0xFF)
        }
    }

    /**
     * 接口名是否像"有线网卡"。
     *
     * 这是 [NetworkCapabilities] 标志位之外的第二层判断 —— 某些 ROM 会把 USB 网卡
     * 归成未知 transport，只看 hasTransport(TRANSPORT_ETHERNET) 会误判成"未连接转接器"。
     * Wi-Fi 固定是 wlan0 / p2p*，不会误命中。
     */
    fun isWiredIface(name: String?): Boolean {
        val n = name?.lowercase() ?: return false
        return n.startsWith("eth") ||
            n.startsWith("usb") ||
            n.startsWith("rndis") ||
            n.startsWith("enp")
    }

    /** 系统里是否存在一块处于 up 状态的有线网卡（不依赖 ConnectivityService） */
    fun findUpWiredInterface(): String? = runCatching {
        NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            .firstOrNull { itf ->
                runCatching { itf.isUp }.getOrDefault(false) && isWiredIface(itf.name)
            }?.name
    }.getOrNull()

    /** 该接口当前是否已经有 IPv4 地址 */
    fun ifaceHasIpv4(name: String?): Boolean = runCatching {
        val itf = NetworkInterface.getByName(name ?: return@runCatching false) ?: return@runCatching false
        itf.inetAddresses.toList().any { it is Inet4Address && !it.isLoopbackAddress }
    }.getOrDefault(false)

    /** 从若干地址里挑一个用于展示的 IPv6：优先全局地址，其次链路本地 */
    fun pickIpv6(addresses: List<String>): String? =
        addresses.firstOrNull { !it.lowercase().startsWith("fe80") } ?: addresses.firstOrNull()

    fun isIpv6(a: java.net.InetAddress): Boolean = a is Inet6Address && !a.isLoopbackAddress

    /** MAC 格式化：A4:5E:60:1B:2C:3D。Android 10+ 可能拿不到（隐私策略），返回 null 由 UI 兜底 */
    fun macOf(iface: String?): String? = runCatching {
        val bytes = NetworkInterface.getByName(iface ?: return@runCatching null)
            ?.hardwareAddress ?: return@runCatching null
        bytes.joinToString(":") { "%02X".format(it) }
    }.getOrNull()

    /** /sys/class/net/<iface>/speed 单位 Mbps */
    fun linkSpeedMbps(iface: String?): Int? {
        if (iface == null) return null
        return readSysInt("/sys/class/net/$iface/speed")
    }

    /** /sys/class/net/<iface>/mtu */
    fun mtu(iface: String?): Int? {
        if (iface == null) return null
        return readSysInt("/sys/class/net/$iface/mtu")
    }

    private fun readSysInt(path: String): Int? = runCatching {
        File(path).takeIf { it.exists() }?.readText()?.trim()?.toIntOrNull()
    }.getOrNull()

    /**
     * 从 /sys 读内核驱动名并映射成型号。
     *
     * ⚠️ Android 10+ 的 SELinux 会把 `/sys/class/net` 对普通 App 封掉
     * （实测小米 HyperOS 报 `avc: denied { search } for name="net"`），
     * 所以这只是**第一优先**手段，读不到要退到 [usbEthernetName]。
     */
    fun adapterNameFromSys(iface: String?): String? {
        if (iface == null) return null
        val driver = runCatching {
            File("/sys/class/net/$iface/device/uevent")
                .takeIf { it.exists() }
                ?.readLines()
                ?.firstOrNull { it.startsWith("DRIVER=") }
                ?.removePrefix("DRIVER=")
                ?.trim()
        }.getOrNull() ?: return null

        return when {
            driver.startsWith("r8153") -> "Realtek RTL8153"
            driver.startsWith("r8152") -> "Realtek RTL8152/8153"
            driver.startsWith("r8156") -> "Realtek RTL8156"
            driver.startsWith("ax88179") || driver.startsWith("ax8817x") -> "ASIX AX88179"
            driver.startsWith("asix") -> "ASIX USB 以太网"
            driver.startsWith("cdc_ether") || driver.startsWith("cdc_ncm") -> "USB 以太网 (CDC)"
            driver.startsWith("rndis") -> "USB 以太网 (RNDIS)"
            driver.startsWith("smsc95") -> "SMSC LAN95xx"
            driver.startsWith("lan78xx") -> "Microchip LAN78xx"
            else -> driver.replace('_', ' ')
        }
    }

    /**
     * 用已连接的 USB 设备的 VID/PID 反推网卡型号。
     *
     * 这条路**不碰 sysfs**，因此不受 SELinux 影响；而且 `UsbManager.deviceList`
     * 里的 VID/PID 不需要任何权限就能读到。是 /sys 被拦时最靠得住的手段。
     */
    fun usbEthernetName(context: Context): String? {
        return runCatching {
            val usb = context.getSystemService(Context.USB_SERVICE) as? UsbManager
                ?: return@runCatching null
            usb.deviceList.values.firstNotNullOfOrNull { device ->
                usbEthernetModel(device.vendorId, device.productId)
            }
        }.getOrNull()
    }

    /** VID/PID → 可读型号，覆盖常见 USB 以太网桥接芯片 */
    fun usbEthernetModel(vid: Int, pid: Int): String? = when {
        vid == 0x0BDA && pid == 0x8153 -> "Realtek RTL8153"
        vid == 0x0BDA && pid == 0x8152 -> "Realtek RTL8152"
        vid == 0x0BDA && pid == 0x8156 -> "Realtek RTL8156"
        vid == 0x0BDA && pid == 0x8150 -> "Realtek RTL8150"
        vid == 0x0BDA -> "Realtek USB 以太网"
        vid == 0x0B95 && pid == 0x1790 -> "ASIX AX88179"
        vid == 0x0B95 && pid == 0x7720 -> "ASIX AX88772"
        vid == 0x0B95 && pid == 0x772B -> "ASIX AX88772B"
        vid == 0x0B95 -> "ASIX USB 以太网"
        vid == 0x0424 && pid == 0x7500 -> "Microchip LAN7500"
        vid == 0x0424 && pid == 0x7800 -> "Microchip LAN7800"
        vid == 0x0424 -> "Microchip / SMSC USB 以太网"
        vid == 0x0525 && pid == 0xA4A2 -> "USB 以太网 (RNDIS)"
        else -> null
    }
}
