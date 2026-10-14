package com.localip.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.localip.app.ui.nav.Dest
import com.localip.app.ui.theme.AppColors
import com.localip.app.ui.theme.AppTypo
import com.localip.app.ui.theme.D

/**
 * 底部胶囊导航。
 * 设计稿的 21dp 底部内边距对应手势条；真机上取"系统导航栏 inset 与 21dp 的较大值"，
 * 既贴合设计稿，又不会在 inset 更大的机型上挤在一起。
 */
@Composable
fun BottomTabBar(
    current: Dest,
    onSelect: (Dest) -> Unit,
    modifier: Modifier = Modifier,
) {
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomPad = maxOf(navInset, D.TabBarSidePad)
    val pillShape = RoundedCornerShape(D.TabPillR)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(AppColors.BarBg)
            .padding(
                start = D.TabBarSidePad,
                end = D.TabBarSidePad,
                top = D.TabBarTopPad,
                bottom = bottomPad,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(D.TabPillH)
                .clip(pillShape)
                .background(AppColors.TabPill)
                .border(1.dp, AppColors.StrokeTabBar, pillShape)
                .padding(D.TabPillPad),
        ) {
            Dest.entries.forEach { dest ->
                TabItem(
                    dest = dest,
                    selected = dest == current,
                    onClick = { onSelect(dest) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    dest: Dest,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(D.TabItemR))
            .background(if (selected) AppColors.AccentCyan else Color.Transparent)
            .clickable { onClick() },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(dest.icon),
            contentDescription = null,
            tint = if (selected) AppColors.OnAccent else AppColors.TextTertiary,
            modifier = Modifier.size(D.TabIcon),
        )
        Spacer(Modifier.height(D.TabItemGap))
        Text(
            text = stringResource(dest.label),
            style = AppTypo.tabLabel,
            color = if (selected) AppColors.OnAccent else AppColors.TextTertiary,
            letterSpacing = 0.4.sp,
        )
    }
}
