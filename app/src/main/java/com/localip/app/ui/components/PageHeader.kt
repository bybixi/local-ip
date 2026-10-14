package com.localip.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localip.app.ui.theme.AppColors
import com.localip.app.ui.theme.AppTypo
import com.localip.app.ui.theme.D

/**
 * 页面大字标题 + 副标题（01 / 04 / 05 / 06 屏）。
 * 设计稿里所有页面的标题字号统一为 24，保持全局一致。
 */
@Composable
fun PageHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = AppTypo.pageTitle24,
            color = AppColors.TextPrimary,
            letterSpacing = 0.5.sp,
        )
        if (subtitle != null) {
            Text(text = subtitle, style = AppTypo.caption, color = AppColors.TextTertiary)
        }
    }
}

/**
 * 检测中态的脉冲圆点：12dp 外环（28% 透明）+ 6dp 实心核。
 * 对应设计稿 06 屏状态胶囊左侧的指示器。
 */
@Composable
fun PulseDot(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val coreAlpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "coreAlpha",
    )

    Box(
        modifier = modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.28f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .graphicsLayer { alpha = coreAlpha }
                .clip(CircleShape)
                .background(color)
        )
    }
}
