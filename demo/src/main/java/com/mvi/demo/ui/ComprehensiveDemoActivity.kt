package com.mvi.demo.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mvi.ui.base.MviUiActivity
import com.mvi.core.base.MviIntent
import com.mvi.ui.base.MviUiViewModel
import com.mvi.core.base.UiState
import com.mvi.ui.widget.EmptyStateConfig
import com.mvi.ui.widget.RecyclerViewSkeletonManager
import com.mvi.ui.widget.SkeletonConfig
import com.mvi.demo.R
import com.mvi.demo.databinding.ActivityComprehensiveDemoBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 综合示例页面
 * 展示骨架屏 + 缺省页的完整使用场景
 * 模拟真实的列表数据加载流程
 */
class ComprehensiveDemoActivity : MviUiActivity<ActivityComprehensiveDemoBinding, ComprehensiveDemoViewModel, ComprehensiveDemoIntent>() {

    private var listSkeletonManager: RecyclerViewSkeletonManager? = null

    override fun createBinding(): ActivityComprehensiveDemoBinding {
        return ActivityComprehensiveDemoBinding.inflate(LayoutInflater.from(this))
    }

    override fun getViewModelClass(): Class<ComprehensiveDemoViewModel> {
        return ComprehensiveDemoViewModel::class.java
    }

    override fun getEmptyStateContainer(): ViewGroup {
        return binding.contentContainer
    }

    override fun getEmptyStateConfig(): EmptyStateConfig {
        return EmptyStateConfig(
            loadingMessage = "正在加载列表数据...",
            emptyMessage = "暂无数据",
            errorMessage = "加载失败"
        )
    }

    override fun getSkeletonConfig(): SkeletonConfig {
        return SkeletonConfig(
            enableShimmer = true,
            shimmerDuration = 1200L
        )
    }

    override fun initView() {
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()

        // 自动触发一次加载
        sendIntent(ComprehensiveDemoIntent.LoadData(LoadType.SUCCESS))
    }

    private fun setupToolbar() {
        binding.toolbar?.title = "综合示例"
        binding.toolbar?.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupClickListeners() {
        // 模拟加载成功
        binding.btnLoadSuccess.setOnClickListener {
            sendIntent(ComprehensiveDemoIntent.LoadData(LoadType.SUCCESS))
        }

        // 模拟空数据
        binding.btnLoadEmpty.setOnClickListener {
            sendIntent(ComprehensiveDemoIntent.LoadData(LoadType.EMPTY))
        }

        // 模拟加载失败
        binding.btnLoadError.setOnClickListener {
            sendIntent(ComprehensiveDemoIntent.LoadData(LoadType.ERROR))
        }

        // 模拟网络错误
        binding.btnLoadNetworkError.setOnClickListener {
            sendIntent(ComprehensiveDemoIntent.LoadData(LoadType.NETWORK_ERROR))
        }

        // 切换使用骨架屏还是缺省页
        binding.switchUseSkeleton.setOnCheckedChangeListener { _, isChecked ->
            sendIntent(ComprehensiveDemoIntent.SetUseSkeleton(isChecked))
        }
    }

    override fun observeData() {
        lifecycleScope.launch {
            viewModel.dataState.collect { state ->
                handleDataState(state)
            }
        }

        lifecycleScope.launch {
            viewModel.useSkeleton.collect { useSkeleton ->
                binding.tvLoadingType.text = if (useSkeleton) {
                    "当前使用：骨架屏"
                } else {
                    "当前使用：缺省页"
                }
            }
        }
    }

    /**
     * 处理数据状态
     */
    private fun handleDataState(state: UiState<List<String>>) {
        when (state) {
            is UiState.Idle -> {
                hideAllLoadingStates()
            }
            is UiState.Loading -> {
                if (viewModel.useSkeleton.value) {
                    // 使用骨架屏
                    showSkeletonLoading()
                } else {
                    // 使用缺省页
                    emptyStateManager.showLoading(state.message)
                }
            }
            is UiState.Success -> {
                hideAllLoadingStates()
                showSuccessData(state.data)
            }
            is UiState.Empty -> {
                hideAllLoadingStates()
                emptyStateManager.showEmpty(
                    message = state.message,
                    onRetry = {
                        sendIntent(ComprehensiveDemoIntent.LoadData(LoadType.SUCCESS))
                    }
                )
            }
            is UiState.Error -> {
                hideAllLoadingStates()
                emptyStateManager.showError(
                    message = state.message,
                    onRetry = {
                        sendIntent(ComprehensiveDemoIntent.LoadData(LoadType.SUCCESS))
                    }
                )
            }
            is UiState.NetworkError -> {
                hideAllLoadingStates()
                emptyStateManager.showNetworkError(
                    message = state.message,
                    onRetry = {
                        sendIntent(ComprehensiveDemoIntent.LoadData(LoadType.SUCCESS))
                    }
                )
            }
        }
    }

    /**
     * 显示骨架屏加载
     */
    private fun showSkeletonLoading() {
        emptyStateManager.hide()
        if (listSkeletonManager == null) {
            listSkeletonManager = RecyclerViewSkeletonManager(
                recyclerView = binding.recyclerView,
                skeletonLayoutResId = R.layout.item_skeleton_demo,
                itemCount = 8,
                config = getSkeletonConfig()
            )
        }
        listSkeletonManager?.show()
    }

    /**
     * 隐藏所有加载状态
     */
    private fun hideAllLoadingStates() {
        emptyStateManager.hide()
        listSkeletonManager?.hide()
    }

    /**
     * 显示成功数据
     */
    private fun showSuccessData(data: List<String>) {
        binding.tvDataCount.text = "共 ${data.size} 条数据"
        // 这里应该设置真实的Adapter
        // binding.recyclerView.adapter = YourAdapter(data)
    }

    override fun onDestroy() {
        super.onDestroy()
        listSkeletonManager?.hide()
        listSkeletonManager = null
    }
}

/**
 * 加载类型
 */
enum class LoadType {
    SUCCESS,        // 成功
    EMPTY,          // 空数据
    ERROR,          // 错误
    NETWORK_ERROR   // 网络错误
}

/**
 * ComprehensiveDemo Intent
 */
sealed class ComprehensiveDemoIntent : MviIntent {
    data class LoadData(val type: LoadType) : ComprehensiveDemoIntent()
    data class SetUseSkeleton(val use: Boolean) : ComprehensiveDemoIntent()
}

/**
 * ComprehensiveDemo ViewModel
 */
class ComprehensiveDemoViewModel : MviUiViewModel<ComprehensiveDemoIntent>() {

    private val _dataState = MutableStateFlow<UiState<List<String>>>(UiState.Idle)
    val dataState: StateFlow<UiState<List<String>>> = _dataState

    private val _useSkeleton = MutableStateFlow(true)
    val useSkeleton: StateFlow<Boolean> = _useSkeleton

    override fun handleIntent(intent: ComprehensiveDemoIntent) {
        when (intent) {
            is ComprehensiveDemoIntent.LoadData -> {
                loadData(intent.type)
            }
            is ComprehensiveDemoIntent.SetUseSkeleton -> {
                _useSkeleton.value = intent.use
            }
        }
    }

    /**
     * 加载数据
     */
    private fun loadData(type: LoadType) {
        viewModelScope.launch {
            // 1. 显示 Loading
            _dataState.value = UiState.Loading("正在加载数据...")

            // 2. 模拟网络延迟
            delay(2000)

            // 3. 根据类型返回不同结果
            _dataState.value = when (type) {
                LoadType.SUCCESS -> {
                    val data = (1..20).map { "列表项 $it - ${System.currentTimeMillis()}" }
                    UiState.Success(data)
                }
                LoadType.EMPTY -> {
                    UiState.Empty("暂无数据")
                }
                LoadType.ERROR -> {
                    UiState.Error("服务器错误，请稍后重试")
                }
                LoadType.NETWORK_ERROR -> {
                    UiState.NetworkError("网络连接失败，请检查网络设置")
                }
            }
        }
    }
}
