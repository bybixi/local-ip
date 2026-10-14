package com.localip.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.localip.app.R
import com.localip.app.domain.PingUi
import com.localip.app.ui.theme.AppColors
import com.localip.app.ui.theme.AppTypo
import com.localip.app.ui.theme.D

/** 弹窗外壳：318 宽、圆角 20、内边距 22、间距 16 —— 与设计稿 07 屏一致 */
@Composable
fun DialogCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(D.DialogR)
    Column(
        modifier = modifier
            .width(D.DialogW)
            .heightIn(min = D.DialogH)
            .shadow(
                elevation = 24.dp,
                shape = shape,
                ambientColor = Color.Black,
                spotColor = Color.Black,
            )
            .clip(shape)
            .background(AppColors.Dialog)
            .border(1.dp, AppColors.StrokeDialog, shape)
            .padding(D.DialogPad),
        verticalArrangement = Arrangement.spacedBy(D.DialogGap),
        content = content,
    )
}

/**
 * 清空确认弹窗（07 屏）。
 * scrim 铺满全屏 —— 因此必须挂在 Scaffold 之外，才能把底部 TabBar 一起压暗。
 */
@Composable
fun ClearDialogOverlay(
    count: Int,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Scrim),
        contentAlignment = Alignment.Center,
    ) {
        DialogCard {
            Text(
                text = stringResource(R.string.dialog_title),
                style = AppTypo.dialogTitle,
                color = AppColors.TextPrimary,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.dialog_desc, count),
                style = AppTypo.dialogDesc,
                color = AppColors.TextSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(D.DialogActionsGap),
            ) {
                AppButton(
                    text = stringResource(R.string.btn_cancel),
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                    container = AppColors.BtnNeutral,
                    content = AppColors.TextPrimary,
                    height = D.DialogBtnH,
                    cornerRadius = D.DialogBtnR,
                    textStyle = AppTypo.button14Medium,
                )
                AppButton(
                    text = stringResource(R.string.btn_clear),
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    container = AppColors.Danger,
                    content = Color.White,
                    height = D.DialogBtnH,
                    cornerRadius = D.DialogBtnR,
                    textStyle = AppTypo.button14,
                )
            }
        }
    }
}

/** 网关探测结果弹窗（沿用同一套弹窗样式） */
@Composable
fun PingDialogOverlay(
    state: PingUi,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Scrim),
        contentAlignment = Alignment.Center,
    ) {
        DialogCard {
            Text(
                text = stringResource(R.string.ping_title),
                style = AppTypo.dialogTitle,
                color = AppColors.TextPrimary,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = when (state) {
                    is PingUi.Running -> stringResource(R.string.ping_running)
                    is PingUi.Done -> stringResource(
                        R.string.ping_result,
                        state.result.host,
                        state.result.avgText(),
                        state.result.minText(),
                        state.result.lost,
                        state.result.sent,
                    )
                    is PingUi.Failed -> stringResource(R.string.ping_fail, state.host)
                    PingUi.NoGateway -> stringResource(R.string.ping_no_gateway)
                },
                style = AppTypo.dialogDesc,
                color = AppColors.TextSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
            AppButton(
                text = stringResource(R.string.btn_ok),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                container = AppColors.BtnNeutral,
                content = AppColors.TextPrimary,
                height = D.DialogBtnH,
                cornerRadius = D.DialogBtnR,
                textStyle = AppTypo.button14Medium,
            )
        }
    }
}

/** 单选弹窗：IP 显示格式 / 记录保留数量 */
@Composable
fun SingleChoiceDialog(
    title: String,
    options: List<Pair<String, String>>, // value to label
    selected: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Scrim),
        contentAlignment = Alignment.Center,
    ) {
        DialogCard {
            Text(
                text = title,
                style = AppTypo.dialogTitle,
                color = AppColors.TextPrimary,
                modifier = Modifier.fillMaxWidth(),
            )
            options.forEach { (value, label) ->
                val active = value == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (active) AppColors.AccentCyan.copy(alpha = 0.12f)
                            else Color.Transparent
                        )
                        .clickable { onPick(value) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = label,
                        style = AppTypo.rowLabel,
                        color = if (active) AppColors.AccentCyan else AppColors.TextSecondary,
                    )
                    if (active) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(AppColors.AccentCyan)
                        )
                    }
                }
            }
            AppButton(
                text = stringResource(R.string.btn_cancel),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                container = AppColors.BtnNeutral,
                content = AppColors.TextPrimary,
                height = D.DialogBtnH,
                cornerRadius = D.DialogBtnR,
                textStyle = AppTypo.button14Medium,
            )
        }
    }
}

/** 通用信息弹窗（关于） */@Composable
fun InfoDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Scrim),
        contentAlignment = Alignment.Center,
    ) {
        DialogCard {
            Text(
                text = title,
                style = AppTypo.dialogTitle,
                color = AppColors.TextPrimary,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = body,
                style = AppTypo.dialogDesc,
                color = AppColors.TextSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
            AppButton(
                text = stringResource(R.string.btn_ok),
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                container = AppColors.BtnNeutral,
                content = AppColors.TextPrimary,
                height = D.DialogBtnH,
                cornerRadius = D.DialogBtnR,
                textStyle = AppTypo.button14Medium,
            )
        }
    }
}

/**
 * 文本输入弹窗（服务器地址这类自由输入）。
 * 用 BasicTextField 手绘，保持与设计稿的输入框观感一致。
 */
@Composable
fun TextInputDialog(
    title: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    var text by remember(initial) { mutableStateOf(initial) }
    val shape = RoundedCornerShape(D.MiniBtnR)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppColors.Scrim),
        contentAlignment = Alignment.Center,
    ) {
        DialogCard {
            Text(
                text = title,
                style = AppTypo.dialogTitle,
                color = AppColors.TextPrimary,
                modifier = Modifier.fillMaxWidth(),
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(shape)
                    .background(AppColors.Bg)
                    .border(1.dp, AppColors.StrokeDialog, shape)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (text.isEmpty() && placeholder != null) {
                    Text(
                        text = placeholder,
                        style = AppTypo.monoValue,
                        color = AppColors.TextTertiary,
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    textStyle = AppTypo.monoValue.copy(color = AppColors.TextPrimary),
                    cursorBrush = SolidColor(AppColors.AccentCyan),
                    keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(D.DialogActionsGap),
            ) {
                AppButton(
                    text = stringResource(R.string.btn_cancel),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    container = AppColors.BtnNeutral,
                    content = AppColors.TextPrimary,
                    height = D.DialogBtnH,
                    cornerRadius = D.DialogBtnR,
                    textStyle = AppTypo.button14Medium,
                )
                AppButton(
                    text = stringResource(R.string.btn_ok),
                    onClick = { onConfirm(text.trim()) },
                    modifier = Modifier.weight(1f),
                    container = AppColors.AccentCyan,
                    content = AppColors.OnAccent,
                    height = D.DialogBtnH,
                    cornerRadius = D.DialogBtnR,
                    textStyle = AppTypo.button14,
                )
            }
        }
    }
}
