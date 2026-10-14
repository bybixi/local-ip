package com.localip.app.ui.screens.speed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.localip.app.R
import com.localip.app.domain.SpeedRole
import com.localip.app.ui.components.AppButton
import com.localip.app.ui.components.StatusChip
import com.localip.app.ui.theme.AppColors
import com.localip.app.ui.theme.AppTypo
import com.localip.app.ui.theme.D
import java.util.Locale

/** Mbps 的显示精度：三位数以上取整，小数值保留一位，避免"942.37"这种噪声 */
fun speedText(mbps: Double): String = when {
    mbps <= 0.0 -> "0"
    mbps >= 100.0 -> String.format(Locale.US, "%.0f", mbps)
    else -> String.format(Locale.US, "%.1f", mbps)
}

/**
 * 测速页的 Hero 卡 —— 结构与「概览」的 Hero 保持一致（胶囊 / 大数值 / 按钮），
 * 但大数值旁边带单位，且支持插入进度条。
 */
@Composable
fun SpeedHeroCard(
    chipText: String,
    tone: Color,
    value: String,
    buttonText: String,
    buttonContainer: Color,
    buttonContent: Color,
    onButton: () -> Unit,
    modifier: Modifier = Modifier,
    chipLeading: (@Composable () -> Unit)? = null,
    trailingText: String? = null,
    unit: String? = null,
    caption: String? = null,
    extra: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(D.HeroR)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 18.dp,
                shape = shape,
                ambientColor = tone.copy(alpha = 0.30f),
                spotColor = tone.copy(alpha = 0.30f),
            )
            .clip(shape)
            .background(AppColors.Hero)
            .padding(D.HeroPad),
        verticalArrangement = Arrangement.spacedBy(D.HeroGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusChip(text = chipText, color = tone, leading = chipLeading)
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    style = AppTypo.monoTag,
                    color = AppColors.TextTertiary,
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(text = value, style = AppTypo.ipLarge, color = AppColors.TextPrimary)
                if (unit != null) {
                    Text(
                        text = unit,
                        style = AppTypo.rowLabelMedium,
                        color = AppColors.TextTertiary,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }
            if (caption != null) {
                Text(
                    text = caption,
                    style = AppTypo.sansCaption,
                    color = AppColors.TextTertiary,
                )
            }
        }

        if (extra != null) extra()

        AppButton(
            text = buttonText,
            onClick = onButton,
            modifier = Modifier.fillMaxWidth(),
            container = buttonContainer,
            content = buttonContent,
        )
    }
}

/** 客户端 / 服务端角色切换（与底部导航选中态同一套视觉语言） */
@Composable
fun SpeedSegment(
    role: SpeedRole,
    onSelect: (SpeedRole) -> Unit,
    modifier: Modifier = Modifier,
) {
    val containerShape = RoundedCornerShape(D.SegmentR)
    val itemShape = RoundedCornerShape(D.SegmentItemR)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(containerShape)
            .background(AppColors.Card)
            .padding(D.SegmentPad),
        horizontalArrangement = Arrangement.spacedBy(D.SegmentPad),
    ) {
        SegmentItem(
            text = stringResource(R.string.speed_role_client),
            selected = role == SpeedRole.CLIENT,
            shape = itemShape,
            onClick = { onSelect(SpeedRole.CLIENT) },
            modifier = Modifier.weight(1f),
        )
        SegmentItem(
            text = stringResource(R.string.speed_role_server),
            selected = role == SpeedRole.SERVER,
            shape = itemShape,
            onClick = { onSelect(SpeedRole.SERVER) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SegmentItem(
    text: String,
    selected: Boolean,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) AppColors.AccentCyan else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = if (selected) AppTypo.rowLabelMedium else AppTypo.rowLabel,
            color = if (selected) AppColors.OnAccent else AppColors.TextSecondary,
        )
    }
}
