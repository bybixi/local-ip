package com.localip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.localip.app.core.net.NetSource
import com.localip.app.core.net.NetState
import com.localip.app.data.db.ConnRecord
import com.localip.app.data.prefs.AppSettings
import com.localip.app.domain.OverviewState
import com.localip.app.domain.PingUi
import com.localip.app.core.net.GatewayPing
import com.localip.app.ui.components.BottomTabBar
import com.localip.app.ui.components.ClearDialogOverlay
import com.localip.app.ui.components.PingDialogOverlay
import com.localip.app.ui.nav.Dest
import com.localip.app.ui.screens.DetailContent
import com.localip.app.ui.screens.OverviewContent
import com.localip.app.ui.screens.RecordsContent
import com.localip.app.ui.screens.SettingsContent
import com.localip.app.ui.theme.AppColors
import com.localip.app.ui.theme.LocalIpTheme
import java.util.Calendar

// ---------------------------------------------------------------------------
// 预览用示例数据（数值与设计稿一致）
// ---------------------------------------------------------------------------

private val wiredNet = NetState(
    source = NetSource.ETHERNET,
    ifaceName = "eth0",
    ipv4 = "192.168.1.108",
    prefixLength = 24,
    ipv6 = "fe80::a45e:60ff:fe1b",
    gateway = "192.168.1.1",
    dns = listOf("223.5.5.5", "119.29.29.29"),
    mac = "A4:5E:60:1B:2C:3D",
    mtu = 1500,
    linkSpeedMbps = 1000,
    adapterName = "Realtek RTL8153",
    wiredPresent = true,
    wiredHasIpv4 = true,
)

private val wiredNoIp = wiredNet.copy(
    ipv4 = null,
    prefixLength = null,
    ipv6 = null,
    gateway = null,
    dns = emptyList(),
    mac = null,
    wiredHasIpv4 = false,
)

private val wifiNet = NetState(
    source = NetSource.WIFI,
    ifaceName = "wlan0",
    ipv4 = "172.20.10.4",
    prefixLength = 24,
    gateway = "172.20.10.1",
    dns = listOf("172.20.10.1"),
    ssid = "FY-15-416-5G",
    mtu = 1500,
    wiredPresent = false,
    wiredHasIpv4 = false,
)

private val sampleRecords = listOf(
    ConnRecord(1, "192.168.1.108", "ETHERNET", "Realtek RTL8153", "eth0", now()),
    ConnRecord(2, "192.168.31.56", "ETHERNET", null, "eth0", daysAgo(1)),
    ConnRecord(3, "172.20.10.4", "WIFI", "FY-15-416-5G", "wlan0", daysAgo(2)),
    ConnRecord(4, "192.168.0.14", "ETHERNET", null, "eth0", daysAgo(4)),
    ConnRecord(5, "10.0.0.23", "WIFI", "会议室访客 Wi-Fi", "wlan0", daysAgo(7)),
    ConnRecord(6, "192.168.43.77", "USB_TETHER", null, "rndis0", daysAgo(10)),
)

private fun now(): Long = System.currentTimeMillis()

private fun daysAgo(days: Int): Long =
    Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -days) }.timeInMillis

internal fun srcLabel(source: NetSource): String = when (source) {
    NetSource.ETHERNET -> "有线网卡"
    NetSource.WIFI -> "WLAN"
    NetSource.CELLULAR -> "移动数据"
    NetSource.USB_TETHER -> "USB 网络共享"
    NetSource.NONE -> "无网络"
}

/** 把内容套进 390×844 的手机框，并带上底部导航 —— 与设计稿的画板一致。
 *  internal 是为了让 SpeedScreen.kt 的预览也能复用。 */
@Composable
internal fun PhoneFrame(
    dest: Dest = Dest.Overview,
    overlay: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .size(390.dp, 844.dp)
            .background(AppColors.Bg)
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth()) { content() }
            BottomTabBar(current = dest, onSelect = {})
        }
        overlay?.invoke()
    }
}

// ---------------------------------------------------------------------------
// 01 / 05 / 06 / 兜底
// ---------------------------------------------------------------------------

@Preview(name = "01 概览 · 有线", widthDp = 390, heightDp = 844)
@Composable
private fun P01OverviewWired() {
    LocalIpTheme {
        PhoneFrame(Dest.Overview) {
            OverviewContent(
                state = OverviewState.Wired(wiredNet),
                sourceLabel = ::srcLabel,
                shareText = { "" },
                onCopy = {}, onRefresh = {}, onGatewayPing = {},
                onOpenDetail = {}, onShare = {},
            )
        }
    }
}

@Preview(name = "06 检测中 · DHCP", widthDp = 390, heightDp = 844)
@Composable
private fun P06OverviewDetecting() {
    LocalIpTheme {
        PhoneFrame(Dest.Overview) {
            OverviewContent(
                state = OverviewState.Detecting(wiredNoIp, timedOut = false),
                sourceLabel = ::srcLabel,
                shareText = { "" },
                onCopy = {}, onRefresh = {}, onGatewayPing = {},
                onOpenDetail = {}, onShare = {},
            )
        }
    }
}

@Preview(name = "06b 检测中 · 超时", widthDp = 390, heightDp = 844)
@Composable
private fun P06bOverviewTimeout() {
    LocalIpTheme {
        PhoneFrame(Dest.Overview) {
            OverviewContent(
                state = OverviewState.Detecting(wiredNoIp, timedOut = true),
                sourceLabel = ::srcLabel,
                shareText = { "" },
                onCopy = {}, onRefresh = {}, onGatewayPing = {},
                onOpenDetail = {}, onShare = {},
            )
        }
    }
}

@Preview(name = "05 概览 · 未连接转接器", widthDp = 390, heightDp = 844)
@Composable
private fun P05OverviewFallback() {
    LocalIpTheme {
        PhoneFrame(Dest.Overview) {
            OverviewContent(
                state = OverviewState.Fallback(wifiNet),
                sourceLabel = ::srcLabel,
                shareText = { "" },
                onCopy = {}, onRefresh = {}, onGatewayPing = {},
                onOpenDetail = {}, onShare = {},
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 02 / 03 / 04
// ---------------------------------------------------------------------------

@Preview(name = "02 网络详情", widthDp = 390, heightDp = 844)
@Composable
private fun P02Detail() {
    LocalIpTheme {
        PhoneFrame(Dest.Detail) {
            DetailContent(net = wiredNet, onRefresh = {})
        }
    }
}

@Preview(name = "03 连接记录", widthDp = 390, heightDp = 844)
@Composable
private fun P03Records() {
    LocalIpTheme {
        PhoneFrame(Dest.Records) {
            RecordsContent(
                records = sampleRecords,
                recordLimit = 20,
                currentIp = wiredNet.ipv4,
                currentSource = NetSource.ETHERNET,
                sourceLabel = ::srcLabel,
                onClear = {},
            )
        }
    }
}

@Preview(name = "04 设置", widthDp = 390, heightDp = 844)
@Composable
private fun P04Settings() {
    LocalIpTheme {
        PhoneFrame(Dest.Settings) {
            SettingsContent(
                settings = AppSettings(),
                versionName = "1.4.0",
                onAutoDetect = {}, onNotify = {}, onAutoCopy = {},
                onOpenFormat = {}, onOpenLimit = {}, onOpenAbout = {}, onExport = {},
            )
        }
    }
}

// ---------------------------------------------------------------------------
// 07 清空确认弹窗 + 网关探测弹窗
// ---------------------------------------------------------------------------

@Preview(name = "07 清空确认弹窗", widthDp = 390, heightDp = 844)
@Composable
private fun P07ClearDialog() {
    LocalIpTheme {
        PhoneFrame(
            dest = Dest.Records,
            overlay = {
                ClearDialogOverlay(count = 6, onCancel = {}, onConfirm = {})
            },
        ) {
            RecordsContent(
                records = sampleRecords,
                recordLimit = 20,
                currentIp = wiredNet.ipv4,
                currentSource = NetSource.ETHERNET,
                sourceLabel = ::srcLabel,
                onClear = {},
            )
        }
    }
}

@Preview(name = "网关探测结果", widthDp = 390, heightDp = 844)
@Composable
private fun PPingDialog() {
    LocalIpTheme {
        PhoneFrame(
            dest = Dest.Overview,
            overlay = {
                PingDialogOverlay(
                    state = PingUi.Done(
                        GatewayPing.Result(
                            host = "192.168.1.1",
                            avgMs = 1.2,
                            minMs = 0.9,
                            sent = 3,
                            received = 3,
                        )
                    ),
                    onDismiss = {},
                )
            },
        ) {
            OverviewContent(
                state = OverviewState.Wired(wiredNet),
                sourceLabel = ::srcLabel,
                shareText = { "" },
                onCopy = {}, onRefresh = {}, onGatewayPing = {},
                onOpenDetail = {}, onShare = {},
            )
        }
    }
}
