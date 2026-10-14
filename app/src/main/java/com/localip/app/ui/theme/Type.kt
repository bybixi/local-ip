package com.localip.app.ui.theme

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.localip.app.R

/**
 * 字体家族。
 *
 * - [SerifSC] 衬线：标题、大号 IP、分组标题、行标签、正文说明（设计稿用 Noto Serif SC）
 * - [MonoJB]  等宽：IP / MAC / 网关 / DNS 等数值（设计稿用 JetBrains Mono）
 * - 无衬线：直接用 FontFamily.Default —— 安卓中文机系统默认就是 Noto Sans CJK，
 *   与设计稿的 Noto Sans SC 同源。动态文本（SSID、适配器型号）也走它，
 *   靠系统 font fallback 杜绝豆腐块。
 */
val SerifSC = FontFamily(
    Font(R.font.noto_serif_sc_400, FontWeight.Normal),
    Font(R.font.noto_serif_sc_500, FontWeight.Medium),
    Font(R.font.noto_serif_sc_600, FontWeight.SemiBold),
    Font(R.font.noto_serif_sc_700, FontWeight.Bold),
)

val MonoJB = FontFamily(
    Font(R.font.jetbrains_mono_400, FontWeight.Normal),
    Font(R.font.jetbrains_mono_500, FontWeight.Medium),
    Font(R.font.jetbrains_mono_700, FontWeight.Bold),
)

/** 关掉 Android 字体额外行距，让行高贴近设计稿（设计稿绝大多数文字是 AUTO 行高） */
private val NoFontPadding = PlatformTextStyle(includeFontPadding = false)

private fun serif(
    size: Int,
    weight: FontWeight = FontWeight.Normal,
    lineHeight: Int? = null,
) = TextStyle(
    fontFamily = SerifSC,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight?.sp ?: androidx.compose.ui.unit.TextUnit.Unspecified,
    platformStyle = NoFontPadding,
)

private fun mono(
    size: Int,
    weight: FontWeight = FontWeight.Medium,
) = TextStyle(
    fontFamily = MonoJB,
    fontWeight = weight,
    fontSize = size.sp,
    platformStyle = NoFontPadding,
)

private fun sans(
    size: Int,
    weight: FontWeight = FontWeight.Normal,
) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = size.sp,
    platformStyle = NoFontPadding,
)

/**
 * 命名文字样式（刻意不用 Material 的语义名，因为对不上设计稿）。
 * 数值 = 设计稿实测 fontSize。
 */
object AppTypo {

    // 页面标题：01/04/05/06 用 24，02/03 用 22；记录页 22
    val pageTitle24 = serif(24, FontWeight.Bold)
    val pageTitle22 = serif(22, FontWeight.Bold)

    // 大号 IP
    val ipLarge = serif(40, FontWeight.Bold)

    // 分组标题
    val groupTitle = serif(14, FontWeight.SemiBold)

    // 行标签 / 正文
    val rowLabel = serif(13)
    val rowLabelMedium = serif(13, FontWeight.Medium)
    val caption = serif(12)
    val captionMedium = serif(12, FontWeight.Medium)
    val captionSemiBold = serif(12, FontWeight.SemiBold)
    val hintText = serif(12, lineHeight = 18)

    // 按钮
    val button15 = serif(15, FontWeight.SemiBold)
    val button14 = serif(14, FontWeight.SemiBold)
    val button14Medium = serif(14, FontWeight.Medium)

    // 检测中/记录页的状态文字
    val body13 = serif(13, FontWeight.Medium)
    val body12Medium = serif(12, FontWeight.Medium)

    // 弹窗
    val dialogTitle = serif(17, FontWeight.Bold)
    val dialogDesc = serif(13, lineHeight = 20)

    // 等宽数值
    val monoLarge = mono(15, FontWeight.Bold)   // 记录卡 IP
    val monoValue = mono(13, FontWeight.Medium) // 详情/概览行值
    val monoTag = mono(12, FontWeight.Medium)   // 速率、WLAN、DHCP 标签

    /** 测速历史里的带宽数值 */
    val speedValue = serif(15, FontWeight.SemiBold)

    // 无衬线（系统默认）
    val sansCaption = sans(12)
    val tabLabel = sans(10, FontWeight.Medium)
    val statusTime = sans(16, FontWeight.SemiBold)
}
