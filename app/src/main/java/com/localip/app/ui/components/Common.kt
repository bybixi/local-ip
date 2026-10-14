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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.localip.app.R
import com.localip.app.ui.theme.AppColors
import com.localip.app.ui.theme.AppTypo
import com.localip.app.ui.theme.D

/**
 * 页面外壳：底色 + 状态栏 inset + 左右 20 / 上 8 / 下 20 的页面内边距。
 * （设计稿里的 62px 是"假状态栏"，真机上用系统的，所以这里吃 [WindowInsets.statusBars]）
 */
@Composable
fun ScreenScaffold(
    gap: Dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Bg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(
                start = D.PageSide,
                end = D.PageSide,
                top = D.PageTop,
                bottom = D.PageBottom,
            ),
        verticalArrangement = Arrangement.spacedBy(gap),
        content = content,
    )
}

/** 分级标题：左标题 + 可选右侧动作（青色） */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = title, style = AppTypo.groupTitle, color = AppColors.TextSecondary)
        if (action != null) {
            Text(
                text = action,
                style = AppTypo.captionSemiBold,
                color = AppColors.AccentCyan,
                modifier = Modifier.clickable(enabled = onAction != null) { onAction?.invoke() },
            )
        }
    }
}

/**
 * 分组卡片。
 * 内边距刻意做成「左右 16 + 上下 4」——这样内部的分隔线天然左缩进，
 * 行元素只需要设上下 padding，和设计稿的视觉一致。
 */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = D.CardR18,
    fill: Color = AppColors.Card,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(fill)
            .padding(
                start = D.CardPad,
                end = D.CardPad,
                top = D.CardPadV,
                bottom = D.CardPadV,
            ),
        content = content,
    )
}

/** 卡内 1px 分隔线（卡片已带左右内边距 → 自动左缩进） */
@Composable
fun ThinDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AppColors.Divider)
    )
}

/** 「标签 —— 值（+ chevron）」一行 */
@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    mono: Boolean = true,
    valueStyle: TextStyle? = null,
    chevron: Boolean = false,
    valueColor: Color = AppColors.TextPrimary,
    labelColor: Color = AppColors.TextSecondary,
    verticalPadding: Dp = D.RowPadV,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(D.RowGap),
    ) {
        Text(
            text = label,
            style = AppTypo.rowLabel,
            color = labelColor,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = valueStyle ?: if (mono) AppTypo.monoValue else AppTypo.sansCaption,
            color = valueColor,
            textAlign = TextAlign.End,
        )
        if (chevron) {
            Icon(
                painter = painterResource(R.drawable.ic_chevron_right),
                contentDescription = null,
                tint = AppColors.Chevron,
                modifier = Modifier.size(D.Ic16),
            )
        }
    }
}

/** 状态胶囊：圆点 + 文字，底色是同色 16% 叠加 */
@Composable
fun StatusChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    dot: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(D.ChipR))
            .background(color.copy(alpha = 0.16f))
            .padding(
                start = D.ChipPadStart,
                end = D.ChipPadEnd,
                top = D.ChipPadV,
                bottom = D.ChipPadV,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(D.ChipGap),
    ) {
        when {
            leading != null -> leading()
            dot -> Box(
                Modifier
                    .size(D.DotSize)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Text(text = text, style = AppTypo.captionSemiBold, color = color)
    }
}

/** 提示条：01 屏的青色信息条 / 05 屏的琥珀警示条 */
enum class HintTone { Cyan, Amber, Danger }

@Composable
fun HintBar(
    text: String,
    tone: HintTone,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = R.drawable.ic_info,
) {
    val color = when (tone) {
        HintTone.Cyan -> AppColors.AccentCyan
        HintTone.Amber -> AppColors.Amber
        HintTone.Danger -> AppColors.Danger
    }
    val fillAlpha = if (tone == HintTone.Cyan) 0.07f else 0.09f
    val strokeAlpha = if (tone == HintTone.Cyan) 0.22f else 0.32f
    val textColor = if (tone == HintTone.Amber) AppColors.AmberText else AppColors.TextSecondary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(D.HintR))
            .background(color.copy(alpha = fillAlpha))
            .border(1.dp, color.copy(alpha = strokeAlpha), RoundedCornerShape(D.HintR))
            .padding(horizontal = D.HintPadH, vertical = D.HintPadV),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(D.HintGap),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(D.Ic16),
        )
        Text(
            text = text,
            style = AppTypo.hintText,
            color = textColor,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 06 屏：IP 位置的骨架条 */
@Composable
fun SkeletonBar(modifier: Modifier = Modifier) {
    Box(
        modifier
            .width(D.SkeletonW)
            .height(D.SkeletonH)
            .clip(RoundedCornerShape(D.SkeletonR))
            .background(
                Brush.linearGradient(
                    listOf(AppColors.SkeletonStart, AppColors.SkeletonEnd)
                )
            )
    )
}

/** 06 屏：细进度条 */
@Composable
fun ThinProgress(
    fraction: Float = 0.555f,
    modifier: Modifier = Modifier,
) {
    val radius = RoundedCornerShape(D.ProgressH / 2)
    Box(
        modifier
            .fillMaxWidth()
            .height(D.ProgressH)
            .clip(radius)
            .background(AppColors.Divider),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(D.ProgressH)
                .clip(radius)
                .background(AppColors.AccentCyan)
        )
    }
}

/** 按钮：底/前景/描边三者可配，覆盖设计稿里的全部按钮样式 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = AppColors.AccentCyan,
    content: Color = AppColors.OnAccent,
    border: Color? = null,
    height: Dp = D.BtnH,
    cornerRadius: Dp = D.BtnR,
    enabled: Boolean = true,
    textStyle: TextStyle = AppTypo.button15,
    leading: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val effectiveContainer = if (enabled) container else container.copy(alpha = 0.45f)
    val effectiveContent = if (enabled) content else content.copy(alpha = 0.6f)
    Row(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(effectiveContainer)
            .then(
                if (border != null) Modifier.border(1.dp, border, shape) else Modifier
            )
            .clickable(enabled = enabled) { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(8.dp))
        }
        Text(text = text, style = textStyle, color = effectiveContent)
    }
}

/** 38×38 的方形图标按钮（刷新 / 返回） */
@Composable
fun IconSquare(
    @DrawableRes icon: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AppColors.IconBright,
    size: Dp = D.IconBtn,
    cornerRadius: Dp = D.IconBtnR,
    contentDescription: String? = null,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(AppColors.Card)
            .border(1.dp, AppColors.StrokeTabBar, shape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(D.Ic18),
        )
    }
}

/** 04 屏开关：46×28 轨道 + 22 圆钮 */
@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(D.SwitchW)
            .height(D.SwitchH)
            .clip(RoundedCornerShape(D.SwitchR))
            .background(if (checked) AppColors.AccentCyan else AppColors.SwitchOffTrack)
            .clickable { onCheckedChange(!checked) },
    ) {
        Box(
            Modifier
                .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                .padding(D.SwitchPad)
                .size(D.Knob)
                .clip(RoundedCornerShape(D.KnobR))
                .background(if (checked) Color.White else AppColors.SwitchOffThumb)
        )
    }
}

/** 06 屏进度步骤：图标 + 单行文字 */
enum class StepState { Done, Active, Todo }

@Composable
fun ProgressStepRow(
    text: String,
    state: StepState,
    modifier: Modifier = Modifier,
    accent: Color = AppColors.AccentCyan,
) {
    val (icon, tint) = when (state) {
        StepState.Done -> R.drawable.ic_check_circle to AppColors.Success
        StepState.Active -> R.drawable.ic_ring_active to accent
        StepState.Todo -> R.drawable.ic_circle_pending to AppColors.Chevron
    }
    val textColor = when (state) {
        StepState.Done -> AppColors.TextSecondary
        StepState.Active -> accent
        StepState.Todo -> AppColors.TextTertiary
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = D.RowPadVStep),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(D.HintGap),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(D.Ic16),
        )
        Text(
            text = text,
            style = if (state == StepState.Active) AppTypo.body12Medium else AppTypo.caption,
            color = textColor,
            modifier = Modifier.weight(1f),
        )
    }
}

/** 05 屏引导步骤：编号徽章 + 标题 + 说明 */
@Composable
fun GuideStepRow(
    index: Int,
    title: String,
    desc: String,
    modifier: Modifier = Modifier,
    accent: Color = AppColors.Amber,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(D.RowGapWide),
    ) {
        Box(
            Modifier
                .size(D.StepBadge)
                .clip(RoundedCornerShape(D.StepBadgeR))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = index.toString(),
                style = AppTypo.monoTag.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                color = accent,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(text = title, style = AppTypo.rowLabelMedium, color = AppColors.TextPrimary)
            Text(text = desc, style = AppTypo.caption, color = AppColors.TextTertiary)
        }
    }
}

/** 撑满高度的占位，避免 Column 内容不足时被拉伸 */
@Composable
fun FillSpacer(modifier: Modifier = Modifier) {
    Spacer(modifier.fillMaxHeight())
}
