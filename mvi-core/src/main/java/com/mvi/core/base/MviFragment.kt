package com.mvi.core.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.launch

/**
 * MVI Fragment 基类
 *
 * 优化点:
 * 1. 自动处理生命周期,使用repeatOnLifecycle避免内存泄漏
 * 2. DSL风格的观察者模式
 * 3. 统一的UI事件处理
 *
 * @param VB ViewBinding类型
 * @param VM ViewModel类型
 * @param I Intent类型
 */
abstract class MviFragment<VB : ViewBinding, VM : MviViewModel<I>, I : MviIntent> : Fragment() {

    private var _binding: VB? = null
    protected val binding get() = _binding!!

    protected lateinit var viewModel: VM

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = createBinding(inflater, container)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
    protected abstract fun createBinding(inflater: LayoutInflater, container: ViewGroup?): VB

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
        viewLifecycleOwner.lifecycleScope.launch {
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
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT)
            .show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
