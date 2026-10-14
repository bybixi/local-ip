package com.localip.app.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.localip.app.domain.AppViewModel
import com.localip.app.ui.components.BottomTabBar
import com.localip.app.ui.components.ClearDialogOverlay
import com.localip.app.ui.components.PingDialogOverlay
import com.localip.app.ui.nav.Dest
import com.localip.app.ui.screens.DetailScreen
import com.localip.app.ui.screens.OverviewScreen
import com.localip.app.ui.screens.RecordsScreen
import com.localip.app.ui.screens.SettingsScreen
import com.localip.app.ui.screens.SpeedScreen
import com.localip.app.ui.theme.AppColors

/**
 * 应用根。
 *
 * 弹窗浮层刻意放在 Scaffold **之外** —— 设计稿 07 屏的 scrim 会连底部 TabBar 一起压暗，
 * 放进 NavHost 内就只能盖住内容区。
 */
@Composable
fun AppRoot() {
    val vm: AppViewModel = viewModel()
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val current = Dest.entries.firstOrNull { it.route == currentRoute } ?: Dest.Overview

    val showClearDialog by vm.showClearDialog.collectAsStateWithLifecycle()
    val ping by vm.ping.collectAsStateWithLifecycle()
    val records by vm.records.collectAsStateWithLifecycle()

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        vm.messages.collect { text ->
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Bg)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            // 状态栏 inset 由各屏的 ScreenScaffold 自己吃，这里不重复处理
            contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
            bottomBar = {
                BottomTabBar(
                    current = current,
                    onSelect = { dest ->
                        if (dest.route != current.route) {
                            navController.navigate(dest.route) {
                                popUpTo(Dest.Overview.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                )
            },
        ) { inner ->
            NavHost(
                navController = navController,
                startDestination = Dest.Overview.route,
                modifier = Modifier.padding(inner),
                // Material 3 的 FadeThrough：顶级导航之间切换 —— 旧内容快速淡出，
                // 新内容淡入并轻微放大，两者不重叠，表达"平级目的地"的关系。
                enterTransition = { Motion.fEnter },
                exitTransition = { Motion.fExit },
                popEnterTransition = { Motion.fEnter },
                popExitTransition = { Motion.fExit },
            ) {
                composable(Dest.Overview.route) {
                    OverviewScreen(
                        vm = vm,
                        onOpenDetail = {
                            navController.navigate(Dest.Detail.route) { launchSingleTop = true }
                        },
                    )
                }
                composable(Dest.Detail.route) { DetailScreen(vm) }
                composable(Dest.Speed.route) {
                    // 测速页有自己的 ViewModel（iperf3 进程、服务端状态都归它管）
                    SpeedScreen(viewModel())
                }
                composable(Dest.Records.route) { RecordsScreen(vm) }
                composable(Dest.Settings.route) { SettingsScreen(vm) }
            }
        }

        // ---- 浮层（覆盖 TabBar）----
        if (showClearDialog) {
            ClearDialogOverlay(
                count = records.size,
                onCancel = vm::closeClearDialog,
                onConfirm = vm::confirmClear,
            )
        }
        ping?.let { state ->
            PingDialogOverlay(state = state, onDismiss = vm::dismissPing)
        }
    }
}
