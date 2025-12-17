package com.mvi.ui.ext

import android.view.View
import android.view.ViewGroup
import com.mvi.ui.R
import com.mvi.ui.widget.EmptyStateConfig
import com.mvi.ui.widget.EmptyStateManager
import com.mvi.ui.widget.RecyclerViewSkeletonManager
import com.mvi.ui.widget.SkeletonConfig
import com.mvi.ui.widget.SkeletonManager

/**
 * 缺省页和骨架屏扩展函数
 * 简化使用方式
 */

// ============ 缺省页扩展 ============

/**
 * 为ViewGroup添加缺省页管理器
 */
fun ViewGroup.emptyStateManager(config: EmptyStateConfig = EmptyStateConfig()): EmptyStateManager {
    return EmptyStateManager(this, config)
}

/**
 * 显示加载中
 */
fun ViewGroup.showLoading(
    message: String? = null,
    config: EmptyStateConfig = EmptyStateConfig()
) {
    val actualMessage = message ?: context.getString(R.string.empty_state_loading)
    emptyStateManager(config).showLoading(actualMessage)
}

/**
 * 显示空数据
 */
fun ViewGroup.showEmpty(
    message: String? = null,
    iconResId: Int = 0,
    config: EmptyStateConfig = EmptyStateConfig(),
    onRetry: (() -> Unit)? = null
) {
    val actualMessage = message ?: context.getString(R.string.empty_state_no_data)
    emptyStateManager(config).showEmpty(actualMessage, iconResId, onRetry)
}

/**
 * 显示错误
 */
fun ViewGroup.showError(
    message: String? = null,
    iconResId: Int = 0,
    config: EmptyStateConfig = EmptyStateConfig(),
    onRetry: (() -> Unit)? = null
) {
    val actualMessage = message ?: context.getString(R.string.empty_state_load_failed)
    emptyStateManager(config).showError(actualMessage, iconResId, onRetry)
}

/**
 * 显示网络错误
 */
fun ViewGroup.showNetworkError(
    message: String? = null,
    config: EmptyStateConfig = EmptyStateConfig(),
    onRetry: (() -> Unit)? = null
) {
    val actualMessage = message ?: context.getString(R.string.empty_state_network_error)
    emptyStateManager(config).showNetworkError(actualMessage, onRetry)
}

// ============ 骨架屏扩展 ============

/**
 * 为View添加骨架屏管理器
 */
fun View.skeletonManager(config: SkeletonConfig = SkeletonConfig()): SkeletonManager {
    return SkeletonManager(this, config)
}

/**
 * 显示骨架屏
 */
fun View.showSkeleton(config: SkeletonConfig = SkeletonConfig()): SkeletonManager {
    return skeletonManager(config).also { it.show() }
}

/**
 * 隐藏骨架屏
 */
fun View.hideSkeleton(manager: SkeletonManager?) {
    manager?.hide()
}

/**
 * 为RecyclerView添加骨架屏
 */
fun androidx.recyclerview.widget.RecyclerView.showSkeletonList(
    skeletonLayoutResId: Int,
    itemCount: Int = 10,
    config: SkeletonConfig = SkeletonConfig()
): RecyclerViewSkeletonManager {
    return RecyclerViewSkeletonManager(this, skeletonLayoutResId, itemCount, config).also {
        it.show()
    }
}

/**
 * 隐藏RecyclerView骨架屏
 */
fun androidx.recyclerview.widget.RecyclerView.hideSkeletonList(manager: RecyclerViewSkeletonManager?) {
    manager?.hide()
}

// ============ DSL风格扩展 ============

/**
 * 缺省页DSL配置
 */
inline fun ViewGroup.emptyState(
    config: EmptyStateConfig = EmptyStateConfig(),
    block: EmptyStateManager.() -> Unit
) {
    emptyStateManager(config).apply(block)
}

/**
 * 骨架屏DSL配置
 */
inline fun View.skeleton(
    config: SkeletonConfig = SkeletonConfig(),
    block: SkeletonManager.() -> Unit
) {
    skeletonManager(config).apply(block)
}

// ============ 使用示例 ============

/**
 * 示例1: 简单使用缺省页
 * ```kotlin
 * binding.root.showLoading()
 * binding.root.showEmpty("暂无数据") { retry() }
 * binding.root.showError("加载失败") { retry() }
 * ```
 *
 * 示例2: 自定义配置
 * ```kotlin
 * val config = EmptyStateConfig(
 *     loadingMessage = "Custom loading...",
 *     emptyMessage = "No items found",
 *     errorMessage = "Something went wrong"
 * )
 * binding.root.showLoading(config = config)
 * ```
 *
 * 示例3: DSL风格
 * ```kotlin
 * binding.root.emptyState {
 *     showLoading("加载中...")
 *     // 加载完成后
 *     showEmpty("暂无数据") { retry() }
 *     // 或显示错误
 *     showError("加载失败") { retry() }
 *     // 隐藏
 *     hide()
 * }
 * ```
 *
 * 示例4: 骨架屏
 * ```kotlin
 * // 单个View
 * val skeleton = binding.contentView.showSkeleton()
 * // 数据加载完成后
 * skeleton.hide()
 *
 * // RecyclerView
 * val listSkeleton = binding.recyclerView.showSkeletonList(
 *     R.layout.item_skeleton,
 *     itemCount = 10
 * )
 * // 数据加载完成后
 * listSkeleton.hide()
 * ```
 *
 * 示例5: 在指定容器中显示缺省页
 * ```kotlin
 * // 在RecyclerView的父容器中显示缺省页
 * val container = binding.recyclerView.parent as ViewGroup
 * val emptyManager = container.emptyStateManager()
 *
 * // 显示加载中
 * emptyManager.showLoading()
 *
 * // 加载完成后隐藏
 * emptyManager.hide()
 * ```
 */
