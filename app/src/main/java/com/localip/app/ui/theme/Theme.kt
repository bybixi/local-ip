package com.localip.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * 纯暗色应用，不提供 light 主题（设计稿就是单一暗色）。
 * 这里把 token 映射进 M3 colorScheme，让 Material 自带的组件
 * （Switch / AlertDialog / HorizontalDivider 等）默认就取到我们的颜色。
 */
private val LocalIpColorScheme = darkColorScheme(
    primary = AppColors.AccentCyan,
    onPrimary = AppColors.OnAccent,
    primaryContainer = AppColors.AccentCyan,
    onPrimaryContainer = AppColors.OnAccent,
    secondary = AppColors.Amber,
    onSecondary = AppColors.OnAccent,
    background = AppColors.Bg,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.Card,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = AppColors.Dialog,
    onSurfaceVariant = AppColors.TextSecondary,
    surfaceContainer = AppColors.Card,
    surfaceContainerHigh = AppColors.Dialog,
    outline = AppColors.StrokeTabBar,
    outlineVariant = AppColors.Divider,
    error = AppColors.Danger,
    onError = AppColors.TextPrimary,
)

@Composable
fun LocalIpTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LocalIpColorScheme,
        content = content,
    )
}
