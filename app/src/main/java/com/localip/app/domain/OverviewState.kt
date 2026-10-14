package com.localip.app.domain

import com.localip.app.core.net.NetState

/**
 * 概览页的四种态 —— 把 7 屏设计稿串成代码的关键。
 *
 * 判定优先级（自上而下，命中即停）：
 *   1. 有线已拿到 IP   → 01 概览·有线（青）
 *   2. 有线在但没 IP   → 06 检测中 DHCP（青）
 *   3. 无有线但有 IP   → 05 概览·未连接转接器（琥珀）
 *   4. 其余            → 兜底「未检测到网络」
 *
 * 1/2 由 wiredPresent 统一收口，天然实现"有线优先"；
 * 3 天生只在"没插网卡"时出现 —— 这正是琥珀色存在的语义。
 */
sealed interface OverviewState {

    val net: NetState

    /** 01 */
    data class Wired(override val net: NetState) : OverviewState

    /** 06 —— [timedOut] 为 true 时文案切到"获取超时" */
    data class Detecting(override val net: NetState, val timedOut: Boolean) : OverviewState

    /** 05（琥珀） */
    data class Fallback(override val net: NetState) : OverviewState

    /** 兜底 */
    data class Disconnected(override val net: NetState) : OverviewState

    companion object {
        /** 检测中超过这个时长仍未拿到 IPv4，就提示超时 */
        const val DETECT_TIMEOUT_MS = 20_000L

        fun decide(net: NetState, detectStartedAt: Long?, now: Long): OverviewState = when {
            net.wiredPresent && net.wiredHasIpv4 -> Wired(net)
            net.wiredPresent -> Detecting(
                net = net,
                timedOut = detectStartedAt != null && now - detectStartedAt > DETECT_TIMEOUT_MS,
            )
            net.hasIp -> Fallback(net)
            else -> Disconnected(net)
        }
    }
}
