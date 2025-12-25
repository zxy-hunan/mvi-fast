package com.mvi.ui.util

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowInsetsController
import androidx.annotation.ColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/**
 * 状态栏工具类
 *
 * 提供状态栏样式设置功能：
 * 1. 设置状态栏背景色
 * 2. 设置状态栏文字颜色（深色/浅色）
 * 3. 沉浸式状态栏
 * 4. 窗口插入处理
 *
 * @author MVI Framework
 * @date 2024/12/24
 */
object StatusBarUtil {

    /**
     * 状态栏配置
     */
    data class Config(
        /** 是否从状态栏下方开始显示，true=传统模式, false=沉浸式 */
        val fitSystemWindows: Boolean = true,

        /** 状态栏背景色，null=使用系统默认 */
        @ColorInt val statusBarColor: Int? = null,

        /** 是否为浅色模式（深色文字），true=深色文字, false=浅色文字 */
        val lightMode: Boolean = false
    )

    /**
     * 应用状态栏配置
     *
     * @param activity Activity实例
     * @param config 状态栏配置
     * @param rootView 根视图，用于设置窗口插入监听
     * @param onApplyInsets 窗口插入回调，仅在沉浸式模式下调用
     */
    fun applyConfig(
        activity: Activity,
        config: Config,
        rootView: View? = null,
        onApplyInsets: ((View, WindowInsetsCompat) -> Unit)? = null
    ) {
        val window = activity.window ?: return

        // 1. 设置状态栏背景色
        config.statusBarColor?.let { color ->
            window.statusBarColor = color
        }

        // 2. 设置状态栏文字颜色
        setStatusBarTextColor(window, config.lightMode)

        // 3. 设置边到边显示模式
        WindowCompat.setDecorFitsSystemWindows(window, config.fitSystemWindows)

        // 4. 沉浸式模式额外处理
        if (!config.fitSystemWindows) {
            setupImmersiveMode(window, rootView, onApplyInsets)
        }
    }

    /**
     * 设置状态栏文字颜色
     *
     * @param window Window实例
     * @param lightMode true=深色文字(浅色背景), false=浅色文字(深色背景)
     */
    fun setStatusBarTextColor(window: Window, lightMode: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11 及以上
            val insetsController = window.insetsController ?: return
            if (lightMode) {
                insetsController.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            } else {
                insetsController.setSystemBarsAppearance(
                    0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0 - 10
            @Suppress("DEPRECATION")
            val decorView = window.decorView
            var flags = decorView.systemUiVisibility
            flags = if (lightMode) {
                flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
            decorView.systemUiVisibility = flags
        }
        // Android 6.0 以下不支持修改状态栏文字颜色
    }

    /**
     * 设置状态栏背景色
     *
     * @param window Window实例
     * @param color 状态栏背景色
     */
    fun setStatusBarColor(window: Window, @ColorInt color: Int) {
        window.statusBarColor = color
    }

    /**
     * 设置沉浸式模式
     */
    private fun setupImmersiveMode(
        window: Window,
        rootView: View?,
        onApplyInsets: ((View, WindowInsetsCompat) -> Unit)?
    ) {
        // Android 11 以下需要额外设置
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
        }

        // 设置窗口插入监听
        if (rootView != null && onApplyInsets != null) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
                onApplyInsets(view, insets)
                insets
            }
        }
    }

    /**
     * 获取状态栏高度
     *
     * @param view 任意View
     * @return 状态栏高度（px）
     */
    fun getStatusBarHeight(view: View): Int {
        val insets = ViewCompat.getRootWindowInsets(view) ?: return 0
        return insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
    }

    /**
     * 获取导航栏高度
     *
     * @param view 任意View
     * @return 导航栏高度（px）
     */
    fun getNavigationBarHeight(view: View): Int {
        val insets = ViewCompat.getRootWindowInsets(view) ?: return 0
        return insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
    }

    /**
     * 判断颜色是否为浅色
     *
     * @param color 颜色值
     * @return true=浅色, false=深色
     */
    fun isColorLight(@ColorInt color: Int): Boolean {
        val darkness = 1 - (
            0.299 * Color.red(color) +
            0.587 * Color.green(color) +
            0.114 * Color.blue(color)
        ) / 255
        return darkness < 0.5
    }

    /**
     * 常用颜色值
     */
    object Colors {
        /** 透明 */
        const val TRANSPARENT = 0x00000000

        /** 纯白 */
        const val WHITE = 0xFFFFFFFF.toInt()

        /** 纯黑 */
        const val BLACK = 0xFF000000.toInt()

        /** 半透明黑色 */
        const val TRANSLUCENT_BLACK = 0x80000000.toInt()

        /** 半透明白色 */
        const val TRANSLUCENT_WHITE = 0x80FFFFFF.toInt()
    }
}
