package com.localip.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * 全部尺寸 / 圆角 / 间距 token —— 数值 1:1 取自 Ardot 设计稿。
 * 设计稿画板为 390×844，其坐标单位即 dp。
 */
object D {

    // ---- 画板 ----
    val ScreenW = 390.dp
    val ScreenH = 844.dp

    // ---- 状态栏（设计稿画了一条 62px 的假状态栏；真机上用系统状态栏 + 该高度作为 inset 参考） ----
    val StatusBarH = 62.dp

    // ---- 底部胶囊 TabBar ----
    val TabBarArea = 95.dp        // 12 + 62 + 21
    val TabBarTopPad = 12.dp
    val TabBarSidePad = 21.dp
    val TabPillH = 62.dp
    val TabPillR = 36.dp
    val TabPillPad = 4.dp
    val TabItemR = 26.dp
    val TabItemGap = 4.dp
    val TabIcon = 20.dp
    val TabLabelSize = 10

    // ---- 页面正文区 ----
    val PageSide = 20.dp
    val PageTop = 8.dp
    val PageBottom = 20.dp

    /** 主列 gap：概览类页面（01/05/06） */
    val GapMain = 20.dp

    /** 主列 gap：详情 / 设置（02/04） */
    val GapSection = 18.dp

    /** 主列 gap：记录页（03/07） */
    val GapRecords = 14.dp

    /** 分组内：标题与卡片之间 */
    val GapInSection = 12.dp

    // ---- Hero 卡 ----
    val HeroPad = 22.dp
    val HeroGap = 18.dp
    val HeroR = 20.dp
    val ChipR = 100.dp
    val ChipPadStart = 10.dp
    val ChipPadEnd = 12.dp
    val ChipPadV = 6.dp
    val ChipGap = 6.dp
    val DotSize = 7.dp
    val IpBlockGap = 8.dp
    val IpBlockGapDetecting = 10.dp

    // ---- 卡片 / 行 ----
    val CardR = 16.dp
    val CardR18 = 18.dp
    val CardPad = 16.dp
    val CardPadV = 4.dp          // 分组卡上下内边距（让分隔线自然左缩进）
    val RowPadV = 13.dp
    val RowPadVNav = 15.dp
    val RowPadVSwitch = 16.dp
    val RowPadVStep = 12.dp
    val RowGap = 8.dp
    val RowGapWide = 12.dp

    // ---- 快捷操作 ----
    val QaPad = 12.dp
    val QaR = 16.dp
    val QaGap = 10.dp

    // ---- 按钮 ----
    val BtnH = 48.dp
    val BtnR = 14.dp
    val DialogBtnH = 46.dp
    val DialogBtnR = 12.dp
    val IconBtn = 38.dp
    val IconBtnR = 12.dp
    val MiniBtnR = 10.dp
    val MiniBtnPadH = 12.dp
    val MiniBtnPadV = 7.dp

    // ---- 提示条 ----
    val HintR = 14.dp
    val HintPadH = 14.dp
    val HintPadV = 12.dp
    val HintGap = 10.dp
    val HintLineHeight = 18

    // ---- 弹窗 ----
    val DialogW = 318.dp
    val DialogH = 190.dp
    val DialogR = 20.dp
    val DialogPad = 22.dp
    val DialogGap = 16.dp
    val DialogActionsGap = 10.dp
    val DialogDescLineHeight = 20

    // ---- 开关 ----
    val SwitchW = 46.dp
    val SwitchH = 28.dp
    val SwitchPad = 3.dp
    val SwitchR = 14.dp
    val Knob = 22.dp
    val KnobR = 11.dp

    // ---- 检测中 ----
    val SkeletonW = 238.dp
    val SkeletonH = 42.dp
    val SkeletonR = 10.dp
    val ProgressH = 4.dp
    val ProgressFillW = 170.dp
    val StepBadge = 26.dp
    val StepBadgeR = 13.dp

    // ---- 分段控件（客户端 / 服务端） ----
    val SegmentR = 12.dp
    val SegmentItemR = 10.dp
    val SegmentPad = 3.dp
    val SegmentHeight = 40.dp

    // ---- 图标 ----
    val Ic16 = 16.dp
    val Ic18 = 18.dp
    val Ic20 = 20.dp
}
