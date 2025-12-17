package com.mvi.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.mvi.core.base.MviFragment
import com.mvi.core.base.MviIntent
import com.mvi.core.base.MviViewModel
import com.mvi.core.base.UiEvent
import com.mvi.ui.widget.EmptyStateConfig
import com.mvi.ui.widget.EmptyStateManager
import com.mvi.ui.widget.SkeletonConfig
import com.mvi.ui.widget.SkeletonManager
import kotlinx.coroutines.launch

/**
 * MVI UI Fragment 增强版
 *
 * 在 MviFragment 基础上添加了 UI 管理器功能:
 * 1. EmptyStateManager - 缺省页管理(空数据、错误、无网络等)
 * 2. SkeletonManager - 骨架屏管理(加载占位)
 *
 * 使用示例:
 * ```kotlin
 * class UserFragment : MviUiFragment<FragmentUserBinding, UserViewModel, UserIntent>() {
 *
 *     override fun createBinding(inflater: LayoutInflater, container: ViewGroup?) =
 *         FragmentUserBinding.inflate(inflater, container, false)
 *
 *     override fun getViewModelClass() = UserViewModel::class.java
 *
 *     override fun initView() {
 *         // 初始化视图
 *         binding.btnRetry.setOnClickListener {
 *             sendIntent(UserIntent.LoadData)
 *         }
 *     }
 *
 *     override fun observeData() {
 *         viewLifecycleOwner.lifecycleScope.launch {
 *             repeatOnLifecycle(Lifecycle.State.STARTED) {
 *                 viewModel.userState.collect { state ->
 *                     when (state) {
 *                         is UiState.Loading -> showLoading()
 *                         is UiState.Success -> {
 *                             hideEmptyState()
 *                             showData(state.data)
 *                         }
 *                         is UiState.Empty -> showEmpty("暂无数据")
 *                         is UiState.Error -> showError(state.message) {
 *                             // 重试回调
 *                             sendIntent(UserIntent.LoadData)
 *                         }
 *                     }
 *                 }
 *             }
 *         }
 *     }
 *
 *     // 可选: 自定义缺省页配置
 *     override fun getEmptyStateConfig(): EmptyStateConfig {
 *         return EmptyStateConfig(
 *             emptyMessage = "自定义空数据提示",
 *             errorMessage = "自定义错误提示"
 *         )
 *     }
 * }
 * ```
 *
 * @param VB ViewBinding类型
 * @param VM ViewModel类型
 * @param I Intent类型
 */
abstract class MviUiFragment<VB : ViewBinding, VM : MviUiViewModel<I>, I : MviIntent> :
    MviFragment<VB, VM, I>() {

    // 缺省页管理器
    private var emptyStateManager: EmptyStateManager? = null

    // 骨架屏管理器
    private var skeletonManager: SkeletonManager? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化缺省页管理器
        initEmptyStateManager()

        // 观察 UI 扩展事件
        observeUiExtEvents()
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
     * 初始化缺省页管理器
     */
    private fun initEmptyStateManager() {
        emptyStateManager = EmptyStateManager(
            getEmptyStateContainer(),
            getEmptyStateConfig()
        )
    }

    /**
     * 获取缺省页管理器
     */
    protected fun getEmptyStateManager(): EmptyStateManager {
        return emptyStateManager ?: throw IllegalStateException("EmptyStateManager not initialized")
    }

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
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiUiEvent.collect { event ->
                    when (event) {
                        is MviUiEvent.ShowEmptyState -> {
                            emptyStateManager?.showEmpty(
                                message = event.message,
                                iconResId = event.iconResId,
                                onRetry = event.onRetry
                            )
                        }
                        is MviUiEvent.ShowErrorState -> {
                            emptyStateManager?.showError(
                                message = event.message,
                                iconResId = event.iconResId,
                                onRetry = event.onRetry
                            )
                        }
                        is MviUiEvent.HideEmptyState -> {
                            emptyStateManager?.hide()
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
        emptyStateManager?.showEmpty(message, iconResId, onRetry)
    }

    /**
     * 显示错误状态
     */
    protected fun showError(
        message: String = "加载失败",
        iconResId: Int = getEmptyStateConfig().errorIconResId,
        onRetry: (() -> Unit)? = null
    ) {
        emptyStateManager?.showError(message, iconResId, onRetry)
    }

    /**
     * 显示无网络状态
     */
    protected fun showNetworkError(onRetry: (() -> Unit)? = null) {
        emptyStateManager?.showNetworkError(onRetry = onRetry)
    }

    /**
     * 隐藏所有状态
     */
    protected fun hideEmptyState() {
        emptyStateManager?.hide()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 清理资源
        emptyStateManager?.hide()
        skeletonManager?.hide()
    }
}
