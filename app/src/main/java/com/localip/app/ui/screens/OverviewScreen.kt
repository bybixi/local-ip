package com.localip.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localip.app.R
import com.localip.app.core.net.NetSource
import com.localip.app.core.net.NetState
import com.localip.app.domain.AppViewModel
import com.localip.app.domain.OverviewState
import com.localip.app.ui.components.GuideStepRow
import com.localip.app.ui.components.HeroCard
import com.localip.app.ui.components.HintBar
import com.localip.app.ui.components.HintTone
import com.localip.app.ui.components.InfoRow
import com.localip.app.ui.components.PageHeader
import com.localip.app.ui.components.ProgressStepRow
import com.localip.app.ui.components.PulseDot
import com.localip.app.ui.components.QuickAction
import com.localip.app.ui.components.QuickActionsRow
import com.localip.app.ui.components.ScreenScaffold
import com.localip.app.ui.components.SectionCard
import com.localip.app.ui.components.SectionHeader
import com.localip.app.ui.components.StepState
import com.localip.app.ui.components.ThinDivider
import com.localip.app.ui.components.ThinProgress
import com.localip.app.ui.theme.AppColors
import com.localip.app.ui.theme.AppTypo
import com.localip.app.ui.theme.D

// ---------------------------------------------------------------------------
// 状态容器：接 ViewModel，只负责取状态与转发事件
// ---------------------------------------------------------------------------

@Composable
fun OverviewScreen(
    vm: AppViewModel,
    onOpenDetail: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by vm.overview.collectAsStateWithLifecycle()
    val context = LocalContext.current

    OverviewContent(
        state = state,
        sourceLabel = vm::sourceLabel,
        shareText = vm::shareText,
        onCopy = { vm.copyIp(state.net.ipv4) },
        onRefresh = vm::refresh,
        onGatewayPing = vm::runGatewayPing,
        onOpenDetail = onOpenDetail,
        onShare = { text ->
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(intent, null))
        },
        modifier = modifier,
    )
}

// ---------------------------------------------------------------------------
// 无状态内容：纯函数式，便于 @Preview 与截图回归
// ---------------------------------------------------------------------------

/**
 * 概览页的完整视觉 —— 承载设计稿的 01 / 05 / 06 三屏 + 一个兜底态。
 * 四态共用同一骨架，只换色调与内容区。
 */
@Composable
fun OverviewContent(
    state: OverviewState,
    sourceLabel: (NetSource) -> String,
    shareText: () -> String,
    onCopy: () -> Unit,
    onRefresh: () -> Unit,
    onGatewayPing: () -> Unit,
    onOpenDetail: () -> Unit,
    onShare: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val net = state.net
    val tone = when (state) {
        is OverviewState.Wired, is OverviewState.Detecting -> AppColors.AccentCyan
        else -> AppColors.Amber
    }
    val borderAlpha = when (state) {
        is OverviewState.Wired -> 0.20f
        is OverviewState.Detecting -> 0.18f
        else -> 0.28f
    }
    val detecting = state is OverviewState.Detecting

    ScreenScaffold(gap = D.GapMain, modifier = modifier) {
        PageHeader(
            title = stringResource(R.string.app_name),
            subtitle = stringResource(R.string.app_subtitle),
        )

        HeroCard(
            chipText = stringResource(chipRes(state)),
            tone = tone,
            borderAlpha = borderAlpha,
            chipLeading = if (detecting) {
                { PulseDot(tone) }
            } else null,
            trailingText = trailingText(state, sourceLabel),
            ipText = if (detecting) null else (net.ipv4 ?: "—"),
            subText = subText(state, sourceLabel),
            ipBlockGap = if (detecting) D.IpBlockGapDetecting else D.IpBlockGap,
            buttonText = stringResource(
                if (detecting) R.string.btn_getting else R.string.btn_copy_ip
            ),
            buttonEnabled = !detecting,
            buttonIcon = if (detecting) null else {
                {
                    Icon(
                        painter = painterResource(R.drawable.ic_copy),
                        contentDescription = null,
                        tint = AppColors.OnAccent,
                        modifier = Modifier.size(D.Ic18),
                    )
                }
            },
            onButton = onCopy,
            extras = if (detecting) {
                { ThinProgress() }
            } else null,
        )

        when (state) {
            is OverviewState.Wired -> WiredBody(
                net = net,
                sourceLabel = sourceLabel,
                shareText = shareText,
                onRefresh = onRefresh,
                onGatewayPing = onGatewayPing,
                onOpenDetail = onOpenDetail,
                onShare = onShare,
            )
            is OverviewState.Detecting -> DetectingBody(timedOut = state.timedOut)
            is OverviewState.Fallback -> GuideBody()
            is OverviewState.Disconnected -> GuideBody()
        }
    }
}

// ---------------------------------------------------------------- 状态相关区

@Composable
private fun ColumnScope.WiredBody(
    net: NetState,
    sourceLabel: (NetSource) -> String,
    shareText: () -> String,
    onRefresh: () -> Unit,
    onGatewayPing: () -> Unit,
    onOpenDetail: () -> Unit,
    onShare: (String) -> Unit,
) {
    QuickActionsRow(
        actions = listOf(
            QuickAction(
                icon = R.drawable.ic_share,
                label = stringResource(R.string.qa_share),
                onClick = { onShare(shareText()) },
            ),
            QuickAction(
                icon = R.drawable.ic_refresh,
                label = stringResource(R.string.qa_refresh),
                onClick = onRefresh,
            ),
            QuickAction(
                icon = R.drawable.ic_latency,
                label = stringResource(R.string.qa_gateway),
                onClick = onGatewayPing,
                enabled = !net.gateway.isNullOrBlank(),
            ),
        )
    )

    SectionHeader(
        title = stringResource(R.string.section_overview),
        action = stringResource(R.string.section_more),
        onAction = onOpenDetail,
    )
    SectionCard {
        InfoRow(
            label = stringResource(R.string.row_gateway),
            value = net.gateway ?: "—",
            chevron = true,
        )
        ThinDivider()
        InfoRow(
            label = stringResource(R.string.row_mask),
            value = net.subnetMask ?: "—",
            chevron = true,
        )
        ThinDivider()
        InfoRow(
            label = stringResource(R.string.row_dns),
            value = net.dnsPrimary ?: "—",
            chevron = true,
        )
    }

    HintBar(
        text = stringResource(R.string.notice_wired),
        tone = HintTone.Cyan,
    )
}

@Composable
private fun ColumnScope.DetectingBody(timedOut: Boolean) {
    SectionHeader(title = stringResource(R.string.section_progress))
    SectionCard {
        ProgressStepRow(
            text = stringResource(R.string.step_detected),
            state = StepState.Done,
        )
        ThinDivider()
        ProgressStepRow(
            text = stringResource(R.string.step_requesting),
            state = if (timedOut) StepState.Done else StepState.Active,
        )
        ThinDivider()
        ProgressStepRow(
            text = stringResource(
                if (timedOut) R.string.step_timeout else R.string.step_waiting
            ),
            state = if (timedOut) StepState.Active else StepState.Todo,
        )
    }
    if (timedOut) {
        HintBar(
            text = stringResource(R.string.step_timeout_desc),
            tone = HintTone.Amber,
            icon = R.drawable.ic_warning,
        )
    }
}

@Composable
private fun ColumnScope.GuideBody() {
    HintBar(
        text = stringResource(R.string.hint_no_adapter),
        tone = HintTone.Amber,
        icon = R.drawable.ic_warning,
    )
    SectionHeader(title = stringResource(R.string.section_guide))
    SectionCard {
        GuideStepRow(
            index = 1,
            title = stringResource(R.string.guide_1_title),
            desc = stringResource(R.string.guide_1_desc),
        )
        ThinDivider()
        GuideStepRow(
            index = 2,
            title = stringResource(R.string.guide_2_title),
            desc = stringResource(R.string.guide_2_desc),
        )
        ThinDivider()
        GuideStepRow(
            index = 3,
            title = stringResource(R.string.guide_3_title),
            desc = stringResource(R.string.guide_3_desc),
        )
    }
}

// ------------------------------------------------------------------ 文案

private fun chipRes(state: OverviewState): Int = when (state) {
    is OverviewState.Wired -> R.string.chip_wired
    is OverviewState.Detecting -> R.string.chip_detecting
    is OverviewState.Fallback -> R.string.chip_no_adapter
    is OverviewState.Disconnected -> R.string.chip_disconnected
}

@Composable
private fun trailingText(state: OverviewState, sourceLabel: (NetSource) -> String): String? =
    when (state) {
        is OverviewState.Wired -> state.net.linkSpeedMbps?.let { "$it Mbps" }
        is OverviewState.Detecting -> stringResource(R.string.proto_dhcp)
        is OverviewState.Fallback -> sourceLabel(state.net.source)
        is OverviewState.Disconnected -> null
    }

@Composable
private fun subText(state: OverviewState, sourceLabel: (NetSource) -> String): String? {
    val net = state.net
    return when (state) {
        is OverviewState.Wired -> net.adapterName?.let {
            stringResource(R.string.wired_meta_fmt, it)
        }
        is OverviewState.Detecting -> net.adapterName?.let {
            stringResource(R.string.detecting_meta, it)
        }
        is OverviewState.Fallback -> buildString {
            append(stringResource(R.string.source_prefix))
            append(sourceLabel(net.source))
            if (net.source == NetSource.WIFI && !net.ssid.isNullOrBlank()) {
                append(" · ").append(net.ssid)
            }
        }
        is OverviewState.Disconnected -> null
    }
}
