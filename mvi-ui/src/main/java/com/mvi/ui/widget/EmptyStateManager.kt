package com.mvi.ui.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.mvi.ui.R

/**
 * 缺省页管理器
 * 支持在任意ViewGroup下显示不同状态的缺省页
 *
 * 使用示例:
 * ```kotlin
 * val emptyStateManager = EmptyStateManager(binding.root)
 *
 * // 显示加载中
 * emptyStateManager.showLoading()
 *
 * // 显示空数据
 * emptyStateManager.showEmpty("暂无数据")
 *
 * // 显示错误
 * emptyStateManager.showError("加载失败") {
 *     // 重试回调
 * }
 *
 * // 隐藏缺省页
 * emptyStateManager.hide()
 * ```
 *
 * 自定义布局:
 * ```kotlin
 * val config = EmptyStateConfig(
 *     loadingLayoutResId = R.layout.custom_loading,
 *     emptyLayoutResId = R.layout.custom_empty,
 *     errorLayoutResId = R.layout.custom_error
 * )
 * val emptyStateManager = EmptyStateManager(binding.root, config)
 * ```
 */
class EmptyStateManager(
    private val container: ViewGroup,
    private val config: EmptyStateConfig = EmptyStateConfig()
) {

    private var currentStateView: View? = null

    /**
     * 显示加载中状态
     */
    fun showLoading(message: String = config.getLoadingMessage(container.context)) {
        show(createLoadingView(message))
    }

    /**
     * 显示空数据状态
     */
    fun showEmpty(
        message: String = config.getEmptyMessage(container.context),
        iconResId: Int = config.emptyIconResId,
        onRetry: (() -> Unit)? = null
    ) {
        show(createEmptyView(message, iconResId, onRetry))
    }

    /**
     * 显示错误状态
     */
    fun showError(
        message: String = config.getErrorMessage(container.context),
        iconResId: Int = config.errorIconResId,
        onRetry: (() -> Unit)? = null
    ) {
        show(createErrorView(message, iconResId, onRetry))
    }

    /**
     * 显示网络错误状态
     */
    fun showNetworkError(
        message: String = config.getNetworkErrorMessage(container.context),
        onRetry: (() -> Unit)? = null
    ) {
        show(createErrorView(message, config.networkErrorIconResId, onRetry))
    }

    /**
     * 显示自定义状态
     */
    fun showCustom(customView: View) {
        show(customView)
    }

    /**
     * 显示自定义状态（通过布局ID）
     */
    fun showCustom(layoutResId: Int) {
        val view = LayoutInflater.from(container.context).inflate(layoutResId, container, false)
        show(view)
    }

    /**
     * 隐藏缺省页
     */
    fun hide() {
        currentStateView?.let {
            container.removeView(it)
            currentStateView = null
        }
    }

    /**
     * 是否正在显示缺省页
     */
    fun isShowing(): Boolean = currentStateView != null

    /**
     * 显示缺省页视图
     */
    private fun show(view: View) {
        // 移除旧的缺省页
        hide()

        // 添加新的缺省页
        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        container.addView(view, layoutParams)
        currentStateView = view
    }

    /**
     * 创建加载中视图
     */
    private fun createLoadingView(message: String): View {
        // 如果配置了自定义创建器，使用自定义创建器
        config.loadingViewCreator?.let {
            return it(container.context, message)
        }

        // 使用 XML 布局
        val layoutResId = config.loadingLayoutResId
        val view = LayoutInflater.from(container.context).inflate(layoutResId, container, false)

        // 设置消息文本
        view.findViewById<TextView>(R.id.tvMessage)?.text = message

        return view
    }

    /**
     * 创建空数据视图
     */
    private fun createEmptyView(message: String, iconResId: Int, onRetry: (() -> Unit)?): View {
        // 如果配置了自定义创建器，使用自定义创建器
        config.emptyViewCreator?.let {
            return it(container.context, message, iconResId, onRetry)
        }

        // 使用 XML 布局
        val layoutResId = config.emptyLayoutResId
        val view = LayoutInflater.from(container.context).inflate(layoutResId, container, false)

        // 设置图标
        view.findViewById<ImageView>(R.id.ivIcon)?.let { imageView ->
            if (iconResId != 0) {
                imageView.setImageResource(iconResId)
                imageView.visibility = View.VISIBLE
            } else {
                imageView.visibility = View.GONE
            }
        }

        // 设置消息文本
        view.findViewById<TextView>(R.id.tvMessage)?.text = message

        // 设置重试按钮
        view.findViewById<Button>(R.id.btnRetry)?.let { button ->
            if (onRetry != null) {
                button.visibility = View.VISIBLE
                button.text = config.getEmptyRetryButtonText(container.context)
                button.setOnClickListener { onRetry() }
            } else {
                button.visibility = View.GONE
            }
        }

        return view
    }

    /**
     * 创建错误视图
     */
    private fun createErrorView(message: String, iconResId: Int, onRetry: (() -> Unit)?): View {
        // 如果配置了自定义创建器，使用自定义创建器
        config.errorViewCreator?.let {
            return it(container.context, message, iconResId, onRetry)
        }

        // 使用 XML 布局
        val layoutResId = config.errorLayoutResId
        val view = LayoutInflater.from(container.context).inflate(layoutResId, container, false)

        // 设置图标
        view.findViewById<ImageView>(R.id.ivIcon)?.let { imageView ->
            if (iconResId != 0) {
                imageView.setImageResource(iconResId)
                imageView.visibility = View.VISIBLE
            } else {
                imageView.visibility = View.GONE
            }
        }

        // 设置消息文本
        view.findViewById<TextView>(R.id.tvMessage)?.text = message

        // 设置重试按钮
        view.findViewById<Button>(R.id.btnRetry)?.let { button ->
            if (onRetry != null) {
                button.visibility = View.VISIBLE
                button.text = config.getErrorRetryButtonText(container.context)
                button.setOnClickListener { onRetry() }
            } else {
                button.visibility = View.GONE
            }
        }

        return view
    }
}

/**
 * 缺省页配置
 */
data class EmptyStateConfig(
    // 默认消息（如果不提供，将使用字符串资源）
    val loadingMessage: String? = null,
    val emptyMessage: String? = null,
    val errorMessage: String? = null,
    val networkErrorMessage: String? = null,

    // 默认图标资源ID
    val emptyIconResId: Int = 0,
    val errorIconResId: Int = 0,
    val networkErrorIconResId: Int = 0,

    // 按钮文字（如果不提供，将使用字符串资源）
    val emptyRetryButtonText: String? = null,
    val errorRetryButtonText: String? = null,

    // 默认布局资源ID（可自定义）
    val loadingLayoutResId: Int = R.layout.mvi_empty_state_loading,
    val emptyLayoutResId: Int = R.layout.mvi_empty_state_empty,
    val errorLayoutResId: Int = R.layout.mvi_empty_state_error,

    // 自定义视图创建器（优先级高于布局资源ID）
    val loadingViewCreator: ((Context, String) -> View)? = null,
    val emptyViewCreator: ((Context, String, Int, (() -> Unit)?) -> View)? = null,
    val errorViewCreator: ((Context, String, Int, (() -> Unit)?) -> View)? = null
) {
    /**
     * 获取加载中消息（优先使用自定义，否则使用字符串资源）
     */
    fun getLoadingMessage(context: Context): String {
        return loadingMessage ?: context.getString(R.string.empty_state_loading)
    }

    /**
     * 获取空数据消息
     */
    fun getEmptyMessage(context: Context): String {
        return emptyMessage ?: context.getString(R.string.empty_state_no_data)
    }

    /**
     * 获取错误消息
     */
    fun getErrorMessage(context: Context): String {
        return errorMessage ?: context.getString(R.string.empty_state_load_failed)
    }

    /**
     * 获取网络错误消息
     */
    fun getNetworkErrorMessage(context: Context): String {
        return networkErrorMessage ?: context.getString(R.string.empty_state_network_error)
    }

    /**
     * 获取空数据重试按钮文字
     */
    fun getEmptyRetryButtonText(context: Context): String {
        return emptyRetryButtonText ?: context.getString(R.string.empty_state_reload)
    }

    /**
     * 获取错误重试按钮文字
     */
    fun getErrorRetryButtonText(context: Context): String {
        return errorRetryButtonText ?: context.getString(R.string.empty_state_retry)
    }
}
