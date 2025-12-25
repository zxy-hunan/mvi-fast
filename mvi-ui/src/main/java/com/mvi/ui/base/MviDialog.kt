package com.mvi.ui.base

import android.app.Activity
import android.view.Gravity
import android.view.LayoutInflater
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.mvi.core.base.MviIntent
import com.mvi.core.base.MviViewModel
import com.mvi.core.base.UiEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import per.goweii.layer.dialog.DialogLayer
import per.goweii.layer.core.anim.AnimStyle
import per.goweii.layer.core.ktx.onClick
import per.goweii.layer.core.widget.SwipeLayout
import per.goweii.layer.dialog.ktx.animStyle
import per.goweii.layer.dialog.ktx.backgroundDimDefault
import per.goweii.layer.dialog.ktx.contentView
import per.goweii.layer.dialog.ktx.gravity
import per.goweii.layer.dialog.ktx.swipeDismiss

/**
 * DialogLayer 扩展函数,支持 lambda 方式添加显示监听器
 */
inline fun DialogLayer.onShow(crossinline block: (DialogLayer) -> Unit): DialogLayer {
    addOnShowListener(object : per.goweii.layer.core.Layer.OnShowListener {
        override fun onPreShow(layer: per.goweii.layer.core.Layer) {
            // 动画开始前不执行
        }

        override fun onPostShow(layer: per.goweii.layer.core.Layer) {
            // 动画结束后执行回调
            block(this@onShow)
        }
    })
    return this
}

/**
 * MVI Dialog 基类
 *
 * 基于 Layer 框架封装，支持两种使用方式：
 * 1. 继承方式（推荐）：使用 ViewBinding，类型安全
 * 2. 函数式方式：直接配置 DialogLayer，灵活便捷
 *
 * ## 使用方式1：继承方式（ViewBinding）
 * ```kotlin
 * class ConfirmDialog(activity: Activity) : MviCenterDialog<DialogConfirmBinding>(activity) {
 *
 *     override fun createBinding(inflater: LayoutInflater): DialogConfirmBinding {
 *         return DialogConfirmBinding.inflate(inflater)
 *     }
 *
 *     override fun initView() {
 *         binding.btnConfirm.setOnClickListener {
 *             dismiss()
 *         }
 *     }
 *
 *     fun setTitle(title: String): ConfirmDialog {
 *         binding.tvTitle.text = title
 *         return this
 *     }
 * }
 *
 * // 使用
 * ConfirmDialog(this)
 *     .setTitle("确认删除？")
 *     .show()
 * ```
 *
 * ## 使用方式2：函数式方式（直接配置）
 * ```kotlin
 * MviDialog.show(this) {
 *     contentView(R.layout.dialog_confirm)
 *     gravity(Gravity.CENTER)
 *     backgroundDimDefault()
 *     setCancelableOnTouchOutside(true)
 *     animStyle(AnimStyle.ZOOM_ALPHA_IN)
 *     onShow {
 *         val binding = viewHolder.content.getBinding<DialogConfirmBinding>()
 *         binding?.apply {
 *             btnConfirm.setOnClickListener { dismiss() }
 *         }
 *     }
 *     onClick(R.id.btnCancel) { dismiss() }
 * }
 * ```
 *
 * ## 使用方式3：函数式方式（底部弹窗）
 * ```kotlin
 * MviDialog.showBottom(this) {
 *     contentView(R.layout.dialog_list)
 *     onShow {
 *         val binding = viewHolder.content.getBinding<DialogListBinding>()
 *         binding?.apply {
 *             // 初始化视图
 *         }
 *     }
 *     onClick(R.id.ivClose) { dismiss() }
 * }
 * ```
 */
abstract class MviDialog<VB : ViewBinding>(
    protected val activity: Activity
) {

    protected lateinit var binding: VB
    protected var dialogLayer: DialogLayer? = null

    /**
     * 创建 ViewBinding
     */
    protected abstract fun createBinding(inflater: LayoutInflater): VB

    /**
     * 配置 Dialog 样式
     * 子类重写此方法来自定义 Dialog 的显示样式
     */
    protected open fun initDialog(layer: DialogLayer) {
        // 默认配置
        layer.gravity(Gravity.CENTER)
            .backgroundDimDefault()
            .setCancelableOnTouchOutside(true)
    }

    /**
     * 初始化视图
     * 子类重写此方法来设置视图内容和事件
     */
    protected abstract fun initView()

    /**
     * 显示 Dialog
     */
    open fun show(): MviDialog<VB> {
        if (dialogLayer != null && dialogLayer!!.isShown) {
            return this
        }

        // 创建 Binding
        binding = createBinding(LayoutInflater.from(activity))

        // 创建 DialogLayer
        dialogLayer = DialogLayer(activity)
            .contentView(binding.root)

        // 初始化配置
        initDialog(dialogLayer!!)

        // 在显示前初始化视图
        dialogLayer!!.addOnShowListener(object : per.goweii.layer.core.Layer.OnShowListener {
            override fun onPreShow(layer: per.goweii.layer.core.Layer) {
                // 在动画开始前初始化视图
                initView()
            }

            override fun onPostShow(layer: per.goweii.layer.core.Layer) {
                // 动画结束后
            }
        })
        
        // 添加关闭监听，确保资源清理
        dialogLayer!!.addOnDismissListener(object : per.goweii.layer.core.Layer.OnDismissListener {
            override fun onPreDismiss(layer: per.goweii.layer.core.Layer) {
                // 开始关闭
            }

            override fun onPostDismiss(layer: per.goweii.layer.core.Layer) {
                // 完全关闭后清理 DialogLayer 引用
                dialogLayer = null
            }
        })

        // 显示
        dialogLayer!!.show()

        return this
    }

    /**
     * 关闭 Dialog
     */
    open fun dismiss() {
        dialogLayer?.dismiss()
        dialogLayer = null
    }

    /**
     * 判断是否正在显示
     */
    fun isShowing(): Boolean {
        return dialogLayer?.isShown ?: false
    }

    /**
     * 设置点击外部是否可关闭
     */
    fun setCancelableOnTouchOutside(cancelable: Boolean): MviDialog<VB> {
        dialogLayer?.setCancelableOnTouchOutside(cancelable)
        return this
    }

    /**
     * 设置按返回键是否可关闭
     */
    fun setCancelableOnClickKeyBack(cancelable: Boolean): MviDialog<VB> {
        dialogLayer?.setCancelableOnClickKeyBack(cancelable)
        return this
    }

    companion object {
        /**
         * 函数式方式显示 Dialog
         *
         * 使用示例：
         * ```kotlin
         * MviDialog.show(this) {
         *     contentView(R.layout.dialog_custom)
         *     gravity(Gravity.CENTER)
         *     backgroundDimDefault()
         *     onShow {
         *         val binding = viewHolder.content.getBinding<DialogCustomBinding>()
         *         binding?.apply {
         *             // 初始化视图
         *         }
         *     }
         *     onClick(R.id.btnClose) { dismiss() }
         * }
         * ```
         */
        fun show(activity: Activity, config: DialogLayer.() -> Unit): DialogLayer {
            return DialogLayer(activity).apply {
                config()
                show()
            }
        }

        /**
         * 函数式方式显示底部 Dialog
         * 默认从底部弹出，支持下滑关闭
         *
         * 使用示例：
         * ```kotlin
         * MviDialog.showBottom(this) {
         *     contentView(R.layout.dialog_bottom)
         *     onShow {
         *         val binding = viewHolder.content.getBinding<DialogBottomBinding>()
         *         binding?.apply {
         *             // 初始化视图
         *         }
         *     }
         * }
         * ```
         */
        fun showBottom(activity: Activity, config: DialogLayer.() -> Unit): DialogLayer {
            return DialogLayer(activity).apply {
                gravity(Gravity.BOTTOM)
                backgroundDimDefault()
                setCancelableOnTouchOutside(true)
                swipeDismiss(SwipeLayout.Direction.BOTTOM)
                animStyle(AnimStyle.BOTTOM)
                config()
                show()
            }
        }

        /**
         * 函数式方式显示居中 Dialog
         * 默认居中显示，缩放动画
         *
         * 使用示例：
         * ```kotlin
         * MviDialog.showCenter(this) {
         *     contentView(R.layout.dialog_center)
         *     onShow {
         *         val binding = viewHolder.content.getBinding<DialogCenterBinding>()
         *         binding?.apply {
         *             // 初始化视图
         *         }
         *     }
         * }
         * ```
         */
        fun showCenter(activity: Activity, config: DialogLayer.() -> Unit): DialogLayer {
            return DialogLayer(activity).apply {
                gravity(Gravity.CENTER)
                backgroundDimDefault()
                setCancelableOnTouchOutside(true)
                animStyle(AnimStyle.ZOOM_ALPHA)
                config()
                show()
            }
        }

        /**
         * 创建 DialogLayer（不自动显示）
         * 用于需要手动控制显示时机的场景
         */
        fun create(activity: Activity, config: DialogLayer.() -> Unit): DialogLayer {
            return DialogLayer(activity).apply(config)
        }
    }
}

/**
 * 底部弹窗基类
 * 默认从底部弹出，支持下滑关闭
 */
abstract class MviBottomDialog<VB : ViewBinding>(activity: Activity) : MviDialog<VB>(activity) {

    override fun initDialog(layer: DialogLayer) {
        layer.gravity(Gravity.BOTTOM)
            .backgroundDimDefault()
            .setCancelableOnTouchOutside(true)
            .swipeDismiss(SwipeLayout.Direction.BOTTOM)
            .animStyle(AnimStyle.BOTTOM)
    }
}

/**
 * 中间弹窗基类
 * 默认居中显示
 */
abstract class MviCenterDialog<VB : ViewBinding>(activity: Activity) : MviDialog<VB>(activity) {

    override fun initDialog(layer: DialogLayer) {
        layer.gravity(Gravity.CENTER)
            .backgroundDimDefault()
            .setCancelableOnTouchOutside(true)
            .animStyle(AnimStyle.ZOOM_ALPHA)
    }
}

/**
 * 支持 ViewModel 的 MVI Dialog 基类
 *
 * 使用示例：
 * ```kotlin
 * class UserInfoDialog(activity: AppCompatActivity) :
 *     MviViewModelDialog<DialogUserInfoBinding, UserInfoViewModel, UserInfoIntent>(activity) {
 *
 *     override fun createBinding(inflater: LayoutInflater): DialogUserInfoBinding {
 *         return DialogUserInfoBinding.inflate(inflater)
 *     }
 *
 *     override fun getViewModelClass(): Class<UserInfoViewModel> {
 *         return UserInfoViewModel::class.java
 *     }
 *
 *     override fun initView() {
 *         binding.btnSave.setOnClickListener {
 *             sendIntent(UserInfoIntent.SaveUserInfo(binding.etName.text.toString()))
 *         }
 *     }
 *
 *     override fun observeData() {
 *         viewModel.userState.collectOn(this) { state ->
 *             when (state) {
 *                 is UiState.Success -> {
 *                     binding.tvName.text = state.data.name
 *                 }
 *                 is UiState.Error -> {
 *                     showToast(state.message)
 *                 }
 *             }
 *         }
 *     }
 * }
 *
 * // 使用
 * UserInfoDialog(this).show()
 * ```
 *
 * @param VB ViewBinding 类型
 * @param VM ViewModel 类型
 * @param I Intent 类型
 */
abstract class MviViewModelDialog<VB : ViewBinding, VM : MviViewModel<I>, I : MviIntent>(
    activity: AppCompatActivity
) : MviDialog<VB>(activity) {

    protected lateinit var viewModel: VM
    private var observeJob: Job? = null
    private val appCompatActivity: AppCompatActivity = activity

    /**
     * 获取 ViewModel 类 - 子类实现
     */
    protected abstract fun getViewModelClass(): Class<VM>

    /**
     * 观察数据 - 子类实现（在协程作用域中调用）
     */
    protected abstract suspend fun observeData()

    override fun show(): MviDialog<VB> {
        if (dialogLayer != null && dialogLayer!!.isShown) {
            return this
        }

        // 创建 Binding
        binding = createBinding(LayoutInflater.from(activity))

        // 初始化 ViewModel
        viewModel = ViewModelProvider(appCompatActivity)[getViewModelClass()]

        // 创建 DialogLayer
        dialogLayer = DialogLayer(activity)
            .contentView(binding.root)

        // 初始化配置
        initDialog(dialogLayer!!)

        // 在显示前初始化视图
        dialogLayer!!.addOnShowListener(object : per.goweii.layer.core.Layer.OnShowListener {
            override fun onPreShow(layer: per.goweii.layer.core.Layer) {
                // 在动画开始前初始化视图
                initView()
                // 开始观察数据
                startObserving()
            }

            override fun onPostShow(layer: per.goweii.layer.core.Layer) {
                // 动画结束后
            }
        })

        // 监听关闭事件
        dialogLayer!!.addOnDismissListener(object : per.goweii.layer.core.Layer.OnDismissListener {
            override fun onPreDismiss(layer: per.goweii.layer.core.Layer) {
                // 开始关闭动画
                stopObserving()
            }

            override fun onPostDismiss(layer: per.goweii.layer.core.Layer) {
                // 已完全关闭,停止观察数据

            }
        })

        // 显示
        dialogLayer!!.show()

        return this
    }

    /**
     * 开始观察数据
     */
    private fun startObserving() {
        observeJob?.cancel()
        observeJob = appCompatActivity.lifecycleScope.launch {
            appCompatActivity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 观察 UI 事件
                launch {
                    viewModel.uiEvent.collect { event ->
                        handleUiEvent(event)
                    }
                }

                // 观察子类的数据
                observeData()
            }
        }
    }

    /**
     * 停止观察数据
     */
    private fun stopObserving() {
        observeJob?.cancel()
        observeJob = null
    }

    /**
     * 处理 UI 事件
     */
    protected open fun handleUiEvent(event: UiEvent) {
        when (event) {
            is UiEvent.ShowToast -> showToast(event.message)
            is UiEvent.ShowLoading -> handleLoading(event.show)
            else -> {
                // 其他事件由子类处理
            }
        }
    }

    /**
     * 显示 Toast - 可重写
     */
    protected open fun showToast(message: String) {
        android.widget.Toast.makeText(activity, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    /**
     * 处理 Loading - 可重写
     */
    protected open fun handleLoading(show: Boolean) {
        // 默认空实现,子类可重写
    }

    /**
     * 发送 Intent
     */
    protected fun sendIntent(intent: I) {
        viewModel.sendIntent(intent)
    }

    override fun dismiss() {
        stopObserving()
        // 清理 ViewModel 引用，防止内存泄漏
        // 注意：不能清理 ViewModel 本身，它由 Activity 管理
        super.dismiss()
    }
    
    /**
     * 完全释放资源 - 防止内存泄漏
     * 在不再使用 Dialog 时应该调用此方法
     */
    fun release() {
        stopObserving()
        dismiss()
        dialogLayer = null
    }
}
