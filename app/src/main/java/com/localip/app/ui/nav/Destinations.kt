package com.localip.app.ui.nav

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.localip.app.R

/** 五个顶级 Tab，顺序与设计稿的底部导航一致 */
enum class Dest(
    val route: String,
    @DrawableRes val icon: Int,
    @StringRes val label: Int,
) {
    Overview("overview", R.drawable.ic_tab_overview, R.string.tab_overview),
    Detail("detail", R.drawable.ic_tab_detail, R.string.tab_detail),
    Speed("speed", R.drawable.ic_tab_speed, R.string.tab_speed),
    Records("records", R.drawable.ic_tab_history, R.string.tab_records),
    Settings("settings", R.drawable.ic_tab_settings, R.string.tab_settings),
}
