package com.localip.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 全部颜色 token —— 数值 1:1 取自 Ardot 设计稿（fileId 733128799153745）。
 * 约定：Color(0x...) 只允许出现在本文件；其它地方一律从 AppColors 取。
 */
object AppColors {

    // ---- 底色 ----
    /** 页面底色（设计稿 01/02/03/04/05/06/07 的根 frame fill） */
    val Bg = Color(0xFF0C0E13)

    /** 状态栏 / TabBar 底色 */
    val BarBg = Color(0xFF0B0F17)

    /** 普通卡片（快捷操作、记录卡、分组卡） */
    val Card = Color(0xFF151A22)

    /** Hero 卡底色 */
    val Hero = Color(0xFF141A24)

    /** 弹窗底色 */
    val Dialog = Color(0xFF1B212C)

    /** 底部 TabBar 胶囊底色（也用作未选中 tab 的底色） */
    val TabPill = Color(0xFF101823)

    /** 弹窗遮罩：#05080D @ 0.76 → 覆盖 TabBar 一起压暗 */
    val Scrim = Color(0xFF05080D).copy(alpha = 0.76f)

    // ---- 描边 / 分隔线 ----
    val StrokeTabBar = Color(0xFF262E3C)
    val StrokeDialog = Color(0xFF2E3645)
    val StrokeNeutral = Color(0xFF3A4554)
    val Divider = Color(0xFF232A35)

    // ---- 文字 ----
    /** 主文字（暖白，比纯白更衬衬线体） */
    val TextPrimary = Color(0xFFEDECE6)
    val TextSecondary = Color(0xFF9AA2AE)
    val TextTertiary = Color(0xFF6A7280)
    val TextFooter = Color(0xFF5A6270)

    /** 图标用的亮色（快捷操作、刷新按钮） */
    val IconBright = Color(0xFFDDE3EC)

    /** 行尾 chevron */
    val Chevron = Color(0xFF4C566A)

    // ---- 强调色 ----
    /** 有线状态主色 */
    val AccentCyan = Color(0xFF22D3EE)

    /** 青色按钮上的深色前景 */
    val OnAccent = Color(0xFF071019)

    /** 已连接 / 成功 */
    val Success = Color(0xFF3FB950)

    /** 未连接转接器（琥珀态） */
    val Amber = Color(0xFFF2B33D)

    /** 琥珀提示条里的正文色 */
    val AmberText = Color(0xFFE3C27F)

    /** 破坏性操作 */
    val Danger = Color(0xFFE5484D)

    // ---- 控件 ----
    val SwitchOffTrack = Color(0xFF2A3542)
    val SwitchOffThumb = Color(0xFF8B949E)
    val DisabledTrack = Color(0xFF1E2530)
    val BtnNeutral = Color(0xFF232B36)
    val SkeletonStart = Color(0xFF212633)
    val SkeletonEnd = Color(0xFF333B4A)
}
