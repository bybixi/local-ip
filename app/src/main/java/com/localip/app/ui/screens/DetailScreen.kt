package com.localip.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.localip.app.R
import com.localip.app.core.net.NetState
import com.localip.app.domain.AppViewModel
import com.localip.app.ui.components.IconSquare
import com.localip.app.ui.components.InfoRow
import com.localip.app.ui.components.ScreenScaffold
import com.localip.app.ui.components.SectionCard
import com.localip.app.ui.components.SectionHeader
import com.localip.app.ui.components.ThinDivider
import com.localip.app.ui.theme.AppColors
import com.localip.app.ui.theme.AppTypo
import com.localip.app.ui.theme.D

/** 02 屏：网络详情（状态容器） */
@Composable
fun DetailScreen(
    vm: AppViewModel,
    modifier: Modifier = Modifier,
) {
    val net by vm.net.collectAsStateWithLifecycle()
    DetailContent(net = net, onRefresh = vm::refresh, modifier = modifier)
}

/** 02 屏：网络详情（无状态内容） */
@Composable
fun DetailContent(
    net: NetState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val empty = stringResource(R.string.value_empty)

    ScreenScaffold(gap = D.GapSection, modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(D.RowGapWide),
        ) {
            Text(
                text = stringResource(R.string.title_detail),
                style = AppTypo.pageTitle22,
                color = AppColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            IconSquare(
                icon = R.drawable.ic_refresh,
                onClick = onRefresh,
                contentDescription = stringResource(R.string.cd_refresh),
            )
        }

        // ---- 网络身份 ----
        SectionHeader(title = stringResource(R.string.sec_identity))
        SectionCard {
            InfoRow(stringResource(R.string.row_ipv4), net.ipv4 ?: empty)
            ThinDivider()
            InfoRow(stringResource(R.string.row_ipv6), net.ipv6 ?: empty)
            ThinDivider()
            InfoRow(stringResource(R.string.row_mac), net.mac ?: empty)
        }

        // ---- 路由与 DNS ----
        SectionHeader(title = stringResource(R.string.sec_routing))
        SectionCard {
            InfoRow(stringResource(R.string.row_gateway), net.gateway ?: empty)
            ThinDivider()
            InfoRow(stringResource(R.string.row_mask), net.subnetMask ?: empty)
            ThinDivider()
            InfoRow(stringResource(R.string.row_dns1), net.dnsPrimary ?: empty)
            ThinDivider()
            InfoRow(stringResource(R.string.row_dns2), net.dnsSecondary ?: empty)
        }

        // ---- 链路信息 ----
        SectionHeader(title = stringResource(R.string.sec_link))
        SectionCard {
            InfoRow(
                label = stringResource(R.string.row_speed),
                value = net.linkSpeedMbps?.let { mbps ->
                    // 「全双工」只对有线成立；Wi-Fi 上报的是估算带宽，不该带这个后缀
                    if (net.fullDuplex) {
                        stringResource(R.string.speed_duplex_fmt, mbps)
                    } else {
                        stringResource(R.string.speed_plain_fmt, mbps)
                    }
                } ?: empty,
            )
            ThinDivider()
            InfoRow(stringResource(R.string.row_mtu), net.mtu?.toString() ?: empty)
            ThinDivider()
            InfoRow(
                label = stringResource(R.string.row_adapter),
                value = net.adapterName ?: empty,
                // 动态文本走系统字体，杜绝衬线子集缺字出现豆腐块
                valueStyle = AppTypo.sansCaption,
            )
        }
    }
}
