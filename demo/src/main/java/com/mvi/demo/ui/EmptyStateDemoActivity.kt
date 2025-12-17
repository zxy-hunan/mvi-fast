package com.mvi.demo.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.mvi.ui.base.MviUiActivity
import com.mvi.core.base.MviIntent
import com.mvi.ui.base.MviUiViewModel
import com.mvi.core.base.UiState
import com.mvi.ui.widget.EmptyStateConfig
import com.mvi.demo.databinding.ActivityEmptyStateDemoBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 缺省页示例页面
 * 展示各种缺省页状态：Loading、Empty、Error、NetworkError
 */
class EmptyStateDemoActivity : MviUiActivity<ActivityEmptyStateDemoBinding, EmptyStateDemoViewModel, EmptyStateDemoIntent>() {

    override fun createBinding(): ActivityEmptyStateDemoBinding {
        return ActivityEmptyStateDemoBinding.inflate(LayoutInflater.from(this))
    }

    override fun getViewModelClass(): Class<EmptyStateDemoViewModel> {
        return EmptyStateDemoViewModel::class.java
    }

    override fun getEmptyStateContainer(): ViewGroup {
        // 指定缺省页显示在哪个容器中
        return binding.contentContainer
    }

    override fun getEmptyStateConfig(): EmptyStateConfig {
        // 自定义缺省页配置
        return EmptyStateConfig(
            loadingMessage = "正在加载数据...",
            emptyMessage = "暂无数据",
            errorMessage = "加载失败，请重试"
        )
    }

    override fun initView() {
        setupToolbar()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.toolbar?.title = "缺省页示例"
        binding.toolbar?.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupClickListeners() {
        // 显示 Loading 状态
        binding.btnShowLoading.setOnClickListener {
            sendIntent(EmptyStateDemoIntent.ShowLoading)
        }

        // 显示 Empty 状态
        binding.btnShowEmpty.setOnClickListener {
            sendIntent(EmptyStateDemoIntent.ShowEmpty)
        }

        // 显示 Error 状态
        binding.btnShowError.setOnClickListener {
            sendIntent(EmptyStateDemoIntent.ShowError)
        }

        // 显示 NetworkError 状态
        binding.btnShowNetworkError.setOnClickListener {
            sendIntent(EmptyStateDemoIntent.ShowNetworkError)
        }

        // 显示成功状态（隐藏缺省页）
        binding.btnShowSuccess.setOnClickListener {
            sendIntent(EmptyStateDemoIntent.ShowSuccess)
        }

        // 模拟网络请求
        binding.btnSimulateRequest.setOnClickListener {
            sendIntent(EmptyStateDemoIntent.SimulateNetworkRequest)
        }
    }

    override fun observeData() {
        lifecycleScope.launch {
            viewModel.demoState.collect { state ->
                when (state) {
                    is UiState.Idle -> {
                        // 初始状态
                        emptyStateManager.hide()
                    }
                    is UiState.Loading -> {
                        // Loading 状态
                        emptyStateManager.showLoading(state.message)
                    }
                    is UiState.Success -> {
                        // 成功状态，隐藏缺省页，显示内容
                        emptyStateManager.hide()
                        binding.tvContent.text = "数据加载成功！\n内容：${state.data}"
                    }
                    is UiState.Empty -> {
                        // 空数据状态
                        emptyStateManager.showEmpty(
                            message = state.message,
                            onRetry = {
                                sendIntent(EmptyStateDemoIntent.SimulateNetworkRequest)
                            }
                        )
                    }
                    is UiState.Error -> {
                        // 错误状态
                        emptyStateManager.showError(
                            message = state.message,
                            onRetry = {
                                sendIntent(EmptyStateDemoIntent.SimulateNetworkRequest)
                            }
                        )
                    }
                    is UiState.NetworkError -> {
                        // 网络错误状态
                        emptyStateManager.showNetworkError(
                            message = state.message,
                            onRetry = {
                                sendIntent(EmptyStateDemoIntent.SimulateNetworkRequest)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * EmptyStateDemo Intent
 */
sealed class EmptyStateDemoIntent : MviIntent {
    data object ShowLoading : EmptyStateDemoIntent()
    data object ShowEmpty : EmptyStateDemoIntent()
    data object ShowError : EmptyStateDemoIntent()
    data object ShowNetworkError : EmptyStateDemoIntent()
    data object ShowSuccess : EmptyStateDemoIntent()
    data object SimulateNetworkRequest : EmptyStateDemoIntent()
}

/**
 * EmptyStateDemo ViewModel
 */
class EmptyStateDemoViewModel : MviUiViewModel<EmptyStateDemoIntent>() {

    private val _demoState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val demoState: StateFlow<UiState<String>> = _demoState

    override fun handleIntent(intent: EmptyStateDemoIntent) {
        when (intent) {
            is EmptyStateDemoIntent.ShowLoading -> {
                _demoState.value = UiState.Loading("正在加载...")
            }
            is EmptyStateDemoIntent.ShowEmpty -> {
                _demoState.value = UiState.Empty("暂无数据")
            }
            is EmptyStateDemoIntent.ShowError -> {
                _demoState.value = UiState.Error("加载失败，请重试")
            }
            is EmptyStateDemoIntent.ShowNetworkError -> {
                _demoState.value = UiState.NetworkError("网络连接失败，请检查网络设置")
            }
            is EmptyStateDemoIntent.ShowSuccess -> {
                _demoState.value = UiState.Success("这是成功加载的数据内容")
            }
            is EmptyStateDemoIntent.SimulateNetworkRequest -> {
                simulateNetworkRequest()
            }
        }
    }

    /**
     * 模拟网络请求
     * 演示完整的加载流程：Loading -> Success/Empty/Error
     */
    private fun simulateNetworkRequest() {
        viewModelScope.launch {
            // 1. 显示 Loading
            _demoState.value = UiState.Loading("正在加载数据...")

            // 2. 模拟网络延迟
            delay(2000)

            // 3. 随机返回不同的结果
            val result = (0..3).random()
            _demoState.value = when (result) {
                0 -> UiState.Success("加载成功！获取到数据：${System.currentTimeMillis()}")
                1 -> UiState.Empty("暂无数据")
                2 -> UiState.Error("服务器错误，请稍后重试")
                else -> UiState.NetworkError("网络连接超时")
            }
        }
    }
}
