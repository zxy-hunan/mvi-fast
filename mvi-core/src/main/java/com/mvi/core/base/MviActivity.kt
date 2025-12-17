package com.mvi.core.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.launch

/**
 * MVI Activity 基类
 *
 * 优化点:
 * 1. 使用内联函数和泛型简化ViewBinding初始化
 * 2. 自动处理生命周期,使用repeatOnLifecycle避免内存泄漏
 * 3. DSL风格的观察者模式
 *
 * @param VB ViewBinding类型
 * @param VM ViewModel类型
 * @param I Intent类型
 */
abstract class MviActivity<VB : ViewBinding, VM : MviViewModel<I>, I : MviIntent> :
    AppCompatActivity() {

    protected lateinit var binding: VB
    protected lateinit var viewModel: VM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化ViewBinding
        binding = createBinding()
        setContentView(binding.root)

        // 初始化ViewModel
        viewModel = ViewModelProvider(this)[getViewModelClass()]

        // 初始化视图
        initView()

        // 观察UI事件
        observeUiEvents()

        // 观察数据
        observeData()
    }

    /**
     * 创建ViewBinding - 子类实现
     */
    protected abstract fun createBinding(): VB

    /**
     * 获取ViewModel类 - 子类实现
     */
    protected abstract fun getViewModelClass(): Class<VM>

    /**
     * 初始化视图 - 子类实现
     */
    protected abstract fun initView()

    /**
     * 观察数据 - 子类实现
     */
    protected abstract fun observeData()

    /**
     * 自动观察UI事件
     */
    private fun observeUiEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEvent.collect { event ->
                    when (event) {
                        is UiEvent.ShowToast -> showToast(event.message)
                        is UiEvent.ShowLoading -> handleLoading(event.show)
                        is UiEvent.Navigate -> navigate(event.route)
                    }
                }
            }
        }
    }

    /**
     * 显示Toast - 可重写
     */
    protected open fun showToast(message: String) {
        // 默认实现
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    /**
     * 处理Loading - 可重写
     */
    protected open fun handleLoading(show: Boolean) {
        // 默认空实现,子类可重写
    }

    /**
     * 导航 - 可重写
     */
    protected open fun navigate(route: String) {
        // 默认空实现,子类可重写
    }

    /**
     * 发送Intent
     */
    protected fun sendIntent(intent: I) {
        viewModel.sendIntent(intent)
    }
}
