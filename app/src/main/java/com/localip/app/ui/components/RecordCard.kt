package com.localip.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.localip.app.ui.theme.AppColors
import com.localip.app.ui.theme.AppTypo
import com.localip.app.ui.theme.D

/**
 * 连接记录卡（03 屏）。
 * 当前连接用青色描边 + 青色 IP + 绿色「当前连接」高亮。
 */
@Composable
fun RecordCard(
    ip: String,
    statusText: String,
    metaText: String,
    current: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(D.CardR)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(AppColors.Card)
            .then(
                if (current) {
                    Modifier.border(1.dp, AppColors.AccentCyan.copy(alpha = 0.35f), shape)
                } else {
                    Modifier
                }
            )
            .padding(D.CardPad),
        verticalArrangement = Arrangement.spacedBy(D.RowGap),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = ip,
                style = AppTypo.monoLarge,
                color = if (current) AppColors.AccentCyan else AppColors.TextPrimary,
            )
            Text(
                text = statusText,
                style = if (current) AppTypo.captionSemiBold else AppTypo.caption,
                color = if (current) AppColors.Success else AppColors.TextTertiary,
            )
        }
        Text(
            text = metaText,
            style = AppTypo.caption,
            color = AppColors.TextTertiary,
        )
    }
}

/** 空列表占位（设计稿未画，沿用同一套 token 保证风格一致） */
@Composable
fun EmptyRecords(
    title: String,
    desc: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(D.CardR18))
            .background(AppColors.Card)
            .padding(D.CardPad),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(text = title, style = AppTypo.body13, color = AppColors.TextSecondary)
        Text(text = desc, style = AppTypo.caption, color = AppColors.TextTertiary)
    }
}

/** 颜色小工具 */
internal fun Color.tint(alpha: Float): Color = copy(alpha = alpha)
