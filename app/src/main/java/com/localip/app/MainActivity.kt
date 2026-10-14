package com.localip.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.localip.app.ui.AppRoot
import com.localip.app.ui.theme.LocalIpTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 用系统真实状态栏 / 导航栏，设计稿里的 62px 当作状态栏 inset 吃掉
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            LocalIpTheme {
                AppRoot()
            }
        }
    }
}
