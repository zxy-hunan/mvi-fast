package com.mvi.ui.ext

import android.view.View

/**
 * View扩展函数
 */

/**
 * 显示View
 */
fun View.visible() {
    visibility = View.VISIBLE
}

/**
 * 隐藏View (占位)
 */
fun View.invisible() {
    visibility = View.INVISIBLE
}

/**
 * 隐藏View (不占位)
 */
fun View.gone() {
    visibility = View.GONE
}

/**
 * 切换可见性
 */
fun View.toggleVisibility() {
    visibility = if (visibility == View.VISIBLE) View.GONE else View.VISIBLE
}

/**
 * 根据条件显示/隐藏
 */
fun View.visibleOrGone(visible: Boolean) {
    visibility = if (visible) View.VISIBLE else View.GONE
}

/**
 * 防抖点击
 */
fun View.setOnClickListener(interval: Long = 500, onClick: (View) -> Unit) {
    var lastClickTime = 0L
    setOnClickListener { view ->
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime > interval) {
            lastClickTime = currentTime
            onClick(view)
        }
    }
}
