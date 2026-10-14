package com.localip.app.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith

/**
 * Material 设计规范里的动效，按"关系"选型：
 *
 * - [sharedAxisZ]：**进入下一层**（待机 → 测速中 → 结果）。缩放 + 淡入淡出，
 *   表达"内容被替换成了更深一层的状态"。
 * - [fadeThroughSpec]：**顶级导航切换**（底部 Tab 之间）。旧内容快速淡出、
 *   新内容淡入并轻微放大，两者不重叠 —— 即 Material 3 的 FadeThrough。
 *
 * 参数取自 Material 3 动效规范：进入 220ms / 退出 140ms 左右，进入用
 * LinearOutSlowIn（快进慢出）、退出用 FastOutLinearIn（慢进快出）。
 */
private val enterEasing = LinearOutSlowInEasing
private val exitEasing = FastOutLinearInEasing

/** 深度方向（Z 轴）的层级切换 */
fun AnimatedContentTransitionScope<*>.sharedAxisZ(): ContentTransform =
    (
        fadeIn(tween(Motion.ENTER_MS, easing = enterEasing)) +
            scaleIn(tween(Motion.ENTER_MS, easing = enterEasing), initialScale = 0.92f)
        ) togetherWith (
        fadeOut(tween(Motion.EXIT_MS, easing = exitEasing)) +
            scaleOut(tween(Motion.EXIT_MS, easing = exitEasing), targetScale = 1.06f)
        ) using SizeTransform(clip = false)

/** FadeThrough 的两半：退出更快、进入稍慢且带缩放，形成"穿透"感 */
object Motion {
    const val ENTER_MS = 220
    const val EXIT_MS = 140

    val fEnter get() = fadeIn(tween(ENTER_MS, easing = enterEasing)) +
        scaleIn(tween(ENTER_MS, easing = enterEasing), initialScale = 0.92f)

    val fExit get() = fadeOut(tween(EXIT_MS, easing = exitEasing)) +
        scaleOut(tween(EXIT_MS, easing = exitEasing), targetScale = 1.08f)
}
