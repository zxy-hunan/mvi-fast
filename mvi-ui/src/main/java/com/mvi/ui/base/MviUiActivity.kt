package com.mvi.ui.base

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.mvi.core.base.MviActivity
import com.mvi.core.base.MviIntent
import com.mvi.core.base.MviViewModel
import com.mvi.core.base.UiEvent
import com.mvi.ui.util.StatusBarUtil
import com.mvi.ui.widget.CommonTitleBar
import com.mvi.ui.widget.EmptyStateConfig
import com.mvi.ui.widget.EmptyStateManager
import com.mvi.ui.widget.SkeletonConfig
import com.mvi.ui.widget.SkeletonManager
import kotlinx.coroutines.launch

/**
 * MVI UI Activity 增强版
 *
 * 在 MviActivity 基础上添加了 UI 管理器功能:
 * 1. EmptyStateManager - 缺省页管理(空数据、错误、无网络等)
 * 2. SkeletonManager - 骨架屏管理(加载占位)
 * 3. 沉浸式状态栏控制 - 通过 fitSystemWindows() 控制页面是否从状态栏开始
 * 4. 状态栏样式控制 - 支持设置状态栏背景色和文字颜色
 * 5. 通用标题栏 - 自动管理标题栏显示（沉浸式模式下不显示）
 *
 * 使用示例:
 * ```kotlin
 * class UserActivity : MviUiActivity<ActivityUserBinding, UserViewModel, UserIntent>() {
 *
 *     override fun createBinding() = ActivityUserBinding.inflate(layoutInflater)
 *
 *     override fun getViewModelClass() = UserViewModel::class.java
 *
 *     // 可选: 使用通用标题栏并配置
 *     override fun showCommonTitleBar(): Boolean = true
 *
 *     override fun setupTitleBar(titleBar: CommonTitleBar) {
 *         titleBar.apply {
 *             setTitle("我的页面")
 *             setOnLeftClickListener { finish() }
 *             setRightText("保存")
 *             setOnRightClickListener { saveData() }
 *         }
 *     }
 * }
 * ```
 *
 * @param VB ViewBinding类型
 * @param VM ViewModel类型
 * @param I Intent类型
 */
abstract class MviUiActivity<VB : ViewBinding, VM : MviUiViewModel<I>, I : MviIntent> :
    MviActivity<VB, VM, I>() {

    // 缺省页管理器 - 使用 lazy 延迟初始化，子类可访问
    protected val emptyStateManager: EmptyStateManager by lazy {
        EmptyStateManager(
            getEmptyStateContainer(),
            getEmptyStateConfig()
        )
    }

    // 骨架屏管理器
    private var skeletonManager: SkeletonManager? = null

    // 通用标题栏
    protected var commonTitleBar: CommonTitleBar? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 设置状态栏样式
        setupStatusBar()

        // 设置通用标题栏（仅在非沉浸式模式下）
        if (fitSystemWindows() && showCommonTitleBar()) {
            setupCommonTitleBar()
        }

        // 观察 UI 扩展事件
        observeUiExtEvents()
    }

    /**
     * 是否显示通用标题栏
     * @return true=显示, false=不显示（默认）
     * 注意：仅在非沉浸式模式（fitSystemWindows() = true）下生效
     */
    protected open fun showCommonTitleBar(): Boolean = false

    /**
     * 配置通用标题栏
     * 子类可重写此方法来自定义标题栏
     *
     * @param titleBar 标题栏实例
     */
    protected open fun setupTitleBar(titleBar: CommonTitleBar) {
        // 默认设置：左侧返回按钮点击finish
        titleBar.setOnLeftClickListener { finish() }
    }

    /**
     * 设置通用标题栏
     */
    private fun setupCommonTitleBar() {
        val contentView = binding.root as? ViewGroup ?: return

        // 创建标题栏
        commonTitleBar = CommonTitleBar(this).also { titleBar ->
            // 添加到内容视图的顶部
            contentView.addView(titleBar, 0)

            // 调用子类配置
            setupTitleBar(titleBar)
        }
    }

    /**
     * 控制页面是否从状态栏开始显示
     * @return true = 从状态栏下方开始显示(默认模式), false = 从状态栏开始显示(沉浸式模式)
     */
    protected open fun fitSystemWindows(): Boolean = true

    /**
     * 获取状态栏背景色
     * @return 状态栏背景色，默认为null(使用系统默认)
     */
    @ColorInt
    protected open fun getStatusBarColor(): Int? = null

    /**
     * 设置状态栏文字颜色模式
     * @return true = 深色文字(浅色背景), false = 浅色文字(深色背景)，默认为false
     */
    protected open fun isStatusBarLightMode(): Boolean = false

    /**
     * 设置状态栏样式
     */
    private fun setupStatusBar() {
        val config = StatusBarUtil.Config(
            fitSystemWindows = fitSystemWindows(),
            statusBarColor = getStatusBarColor(),
            lightMode = isStatusBarLightMode()
        )

        StatusBarUtil.applyConfig(
            activity = this,
            config = config,
            rootView = binding.root,
            onApplyInsets = { view, insets ->
                onApplyWindowInsets(view, insets)
            }
        )
    }

    /**
     * 处理窗口插入（状态栏、导航栏等）
     * 子类可重写此方法来自定义内边距处理
     *
     * @param view 根视图
     * @param insets 窗口插入
     */
    protected open fun onApplyWindowInsets(view: View, insets: WindowInsetsCompat) {
        // 默认不做处理，子类可以重写来添加 padding
        // 例如：
        // val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        // view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
    }

    /**
     * 动态更新状态栏背景色
     * @param color 状态栏背景色
     */
    protected fun updateStatusBarColor(@ColorInt color: Int) {
        window?.let { StatusBarUtil.setStatusBarColor(it, color) }
    }

    /**
     * 动态更新状态栏文字颜色
     * @param lightMode true=深色文字(浅色背景), false=浅色文字(深色背景)
     */
    protected fun updateStatusBarTextColor(lightMode: Boolean) {
        window?.let { StatusBarUtil.setStatusBarTextColor(it, lightMode) }
    }

    /**
     * 获取缺省页容器 - 子类可重写
     * 默认使用根视图
     */
    protected open fun getEmptyStateContainer(): ViewGroup = binding.root as ViewGroup

    /**
     * 获取缺省页配置 - 子类可重写
     */
    protected open fun getEmptyStateConfig(): EmptyStateConfig = EmptyStateConfig()

    /**
     * 获取骨架屏配置 - 子类可重写
     */
    protected open fun getSkeletonConfig(): SkeletonConfig = SkeletonConfig()

    /**
     * 创建骨架屏管理器
     * @param targetView 目标视图
     */
    protected fun createSkeletonManager(targetView: View): SkeletonManager {
        return SkeletonManager(targetView, getSkeletonConfig()).also {
            skeletonManager = it
        }
    }

    /**
     * 观察 UI 扩展事件
     */
    private fun observeUiExtEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiUiEvent.collect { event ->
                    when (event) {
                        is MviUiEvent.ShowEmptyState -> {
                            emptyStateManager.showEmpty(
                                message = event.message,
                                iconResId = event.iconResId,
                                onRetry = event.onRetry
                            )
                        }
                        is MviUiEvent.ShowErrorState -> {
                            emptyStateManager.showError(
                                message = event.message,
                                iconResId = event.iconResId,
                                onRetry = event.onRetry
                            )
                        }
                        is MviUiEvent.HideEmptyState -> {
                            emptyStateManager.hide()
                        }
                    }
                }
            }
        }
    }

    /**
     * 显示空状态
     */
    protected fun showEmpty(
        message: String = "暂无数据",
        iconResId: Int = getEmptyStateConfig().emptyIconResId,
        onRetry: (() -> Unit)? = null
    ) {
        emptyStateManager.showEmpty(message, iconResId, onRetry)
    }

    /**
     * 显示错误状态
     */
    protected fun showError(
        message: String = "加载失败",
        iconResId: Int = getEmptyStateConfig().errorIconResId,
        onRetry: (() -> Unit)? = null
    ) {
        emptyStateManager.showError(message, iconResId, onRetry)
    }

    /**
     * 显示无网络状态
     */
    protected fun showNetworkError(onRetry: (() -> Unit)? = null) {
        emptyStateManager.showNetworkError(onRetry = onRetry)
    }

    /**
     * 隐藏所有状态
     */
    protected fun hideEmptyState() {
        emptyStateManager.hide()
    }

    override fun onDestroy() {
        super.onDestroy()
        // 清理资源 - 使用 release() 确保动画监听器被完全清理
        emptyStateManager.hide()
        skeletonManager?.release()
    }
}
