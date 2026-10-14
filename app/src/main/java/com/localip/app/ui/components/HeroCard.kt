package com.localip.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.localip.app.R
import com.localip.app.ui.theme.AppColors
import com.localip.app.ui.theme.AppTypo
import com.localip.app.ui.theme.D

/**
 * Hero 卡 —— 01 / 05 / 06 三屏共用同一个结构，只换色调与内容：
 *
 * ```
 * ┌ 状态胶囊 ───────────── 右侧标签 ┐
 * │ 本机 IPv4 地址                  │
 * │ 192.168.1.108   （或骨架条）    │
 * │ 适配器名 / 来源                 │
 * │ [extras：06 屏的进度条]         │
 * │ [ 复制 IP 地址 ]                │
 * └─────────────────────────────────┘
 * ```
 *
 * @param tone        主题色：有线=青、未连接转接器=琥珀
 * @param borderAlpha 描边透明度（01 屏 0.20 / 05 屏 0.28 / 06 屏 0.18）
 * @param ipText      传 null 时渲染骨架条（06 检测中态）
 */
@Composable
fun HeroCard(
    chipText: String,
    tone: Color,
    buttonText: String,
    onButton: () -> Unit,
    modifier: Modifier = Modifier,
    borderAlpha: Float = 0.20f,
    chipLeading: (@Composable () -> Unit)? = null,
    trailingText: String? = null,
    ipText: String? = null,
    subText: String? = null,
    ipColor: Color = AppColors.TextPrimary,
    ipBlockGap: Dp = D.IpBlockGap,
    buttonEnabled: Boolean = true,
    buttonIcon: (@Composable () -> Unit)? = null,
    extras: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(D.HeroR)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = shape,
                ambientColor = tone.copy(alpha = 0.35f),
                spotColor = tone.copy(alpha = 0.35f),
            )
            .clip(shape)
            .background(AppColors.Hero)
            .border(1.dp, tone.copy(alpha = borderAlpha), shape)
            .padding(D.HeroPad),
        verticalArrangement = Arrangement.spacedBy(D.HeroGap),
    ) {
        // 状态胶囊 + 右侧标签
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusChip(text = chipText, color = tone, leading = chipLeading)
            if (trailingText != null) {
                Text(text = trailingText, style = AppTypo.monoTag, color = AppColors.TextTertiary)
            }
        }

        // IP 区块
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(ipBlockGap),
        ) {
            Text(
                text = stringResource(R.string.ip_label),
                style = AppTypo.caption,
                color = AppColors.TextTertiary,
            )
            if (ipText != null) {
                Text(text = ipText, style = AppTypo.ipLarge, color = ipColor)
            } else {
                SkeletonBar()
            }
            if (subText != null) {
                Text(text = subText, style = AppTypo.sansCaption, color = AppColors.TextTertiary)
            }
        }

        if (extras != null) extras()

        AppButton(
            text = buttonText,
            onClick = onButton,
            modifier = Modifier.fillMaxWidth(),
            enabled = buttonEnabled,
            container = if (buttonEnabled) AppColors.AccentCyan else AppColors.DisabledTrack,
            content = if (buttonEnabled) AppColors.OnAccent else AppColors.TextTertiary,
            leading = buttonIcon,
        )
    }
}

/** 快捷操作项 */
data class QuickAction(
    @DrawableRes val icon: Int,
    val label: String,
    val onClick: () -> Unit,
    val enabled: Boolean = true,
)

/** 一行等宽的快捷操作卡（设计稿：分享 / 刷新 / 网关探测） */
@Composable
fun QuickActionsRow(
    actions: List<QuickAction>,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(D.QaGap),
    ) {
        actions.forEach { action ->
            val itemShape = RoundedCornerShape(D.QaR)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(itemShape)
                    .background(AppColors.Card)
                    .clickable(enabled = action.enabled) { action.onClick() }
                    .padding(D.QaPad),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    painter = painterResource(action.icon),
                    contentDescription = null,
                    tint = if (action.enabled) AppColors.IconBright else AppColors.TextTertiary,
                    modifier = Modifier.size(D.Ic20),
                )
                Text(
                    text = action.label,
                    style = AppTypo.caption,
                    color = if (action.enabled) AppColors.TextSecondary else AppColors.TextTertiary,
                )
            }
        }
    }
}

/** 记录卡的来源小圆点（可选装饰） */
@Composable
fun StatusDot(color: Color, size: Dp = D.DotSize, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size / 2))
            .background(color)
    )
}

/** 通用留白 */
@Composable
fun VSpace(height: Dp) = Spacer(Modifier.height(height))
