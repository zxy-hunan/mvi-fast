package com.mvi.demo.ui

import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.mvi.ui.base.MviUiActivity
import com.mvi.core.base.MviIntent
import com.mvi.ui.base.MviUiViewModel
import com.mvi.ui.widget.RecyclerViewSkeletonManager
import com.mvi.ui.widget.SkeletonConfig
import com.mvi.ui.widget.SkeletonManager
import com.mvi.demo.R
import com.mvi.demo.databinding.ActivitySkeletonDemoBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 骨架屏示例页面
 * 展示单个View和RecyclerView的骨架屏效果
 */
class SkeletonDemoActivity : MviUiActivity<ActivitySkeletonDemoBinding, SkeletonDemoViewModel, SkeletonDemoIntent>() {

    private var viewSkeletonManager: SkeletonManager? = null
    private var listSkeletonManager: RecyclerViewSkeletonManager? = null

    override fun createBinding(): ActivitySkeletonDemoBinding {
        return ActivitySkeletonDemoBinding.inflate(LayoutInflater.from(this))
    }

    override fun getViewModelClass(): Class<SkeletonDemoViewModel> {
        return SkeletonDemoViewModel::class.java
    }

    override fun getSkeletonConfig(): SkeletonConfig {
        // 自定义骨架屏配置
        return SkeletonConfig(
            skeletonColor = 0xFFE0E0E0.toInt(),
            shimmerHighlightColor = 0xFFF5F5F5.toInt(),
            enableShimmer = true,
            shimmerDuration = 1500L
        )
    }

    override fun initView() {
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
    }

    private fun setupToolbar() {
        binding.toolbar?.title = "骨架屏示例"
        binding.toolbar?.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupClickListeners() {
        // 显示单个View的骨架屏
        binding.btnShowViewSkeleton.setOnClickListener {
            sendIntent(SkeletonDemoIntent.ShowViewSkeleton)
        }

        // 隐藏单个View的骨架屏
        binding.btnHideViewSkeleton.setOnClickListener {
            sendIntent(SkeletonDemoIntent.HideViewSkeleton)
        }

        // 显示列表骨架屏
        binding.btnShowListSkeleton.setOnClickListener {
            sendIntent(SkeletonDemoIntent.ShowListSkeleton)
        }

        // 隐藏列表骨架屏
        binding.btnHideListSkeleton.setOnClickListener {
            sendIntent(SkeletonDemoIntent.HideListSkeleton)
        }

        // 模拟加载数据（完整流程）
        binding.btnSimulateLoading.setOnClickListener {
            sendIntent(SkeletonDemoIntent.SimulateDataLoading)
        }
    }

    override fun observeData() {
        // 观察骨架屏状态变化
        lifecycleScope.launch {
            viewModel.showViewSkeleton.collect { show ->
                if (show) {
                    showViewSkeleton()
                } else {
                    hideViewSkeleton()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.showListSkeleton.collect { show ->
                if (show) {
                    showListSkeleton()
                } else {
                    hideListSkeleton()
                }
            }
        }

        lifecycleScope.launch {
            viewModel.listData.collect { data ->
                updateListData(data)
            }
        }
    }

    /**
     * 显示单个View的骨架屏
     */
    private fun showViewSkeleton() {
        if (viewSkeletonManager == null) {
            viewSkeletonManager = SkeletonManager(
                targetView = binding.contentCard,
                config = getSkeletonConfig()
            )
        }
        viewSkeletonManager?.show()
    }

    /**
     * 隐藏单个View的骨架屏
     */
    private fun hideViewSkeleton() {
        viewSkeletonManager?.hide()
    }

    /**
     * 显示列表骨架屏
     */
    private fun showListSkeleton() {
        if (listSkeletonManager == null) {
            listSkeletonManager = RecyclerViewSkeletonManager(
                recyclerView = binding.recyclerView,
                skeletonLayoutResId = R.layout.item_skeleton_demo, // 骨架屏布局
                itemCount = 10,
                config = getSkeletonConfig()
            )
        }
        listSkeletonManager?.show()
    }

    /**
     * 隐藏列表骨架屏
     */
    private fun hideListSkeleton() {
        listSkeletonManager?.hide()
    }

    /**
     * 更新列表数据
     */
    private fun updateListData(data: List<String>) {
        if (data.isNotEmpty()) {
            binding.tvContentText.text = "数据加载完成！共 ${data.size} 条数据"
            // 这里应该设置真实的Adapter
            // binding.recyclerView.adapter = YourAdapter(data)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewSkeletonManager?.hide()
        listSkeletonManager?.hide()
        viewSkeletonManager = null
        listSkeletonManager = null
    }
}

/**
 * SkeletonDemo Intent
 */
sealed class SkeletonDemoIntent : MviIntent {
    data object ShowViewSkeleton : SkeletonDemoIntent()
    data object HideViewSkeleton : SkeletonDemoIntent()
    data object ShowListSkeleton : SkeletonDemoIntent()
    data object HideListSkeleton : SkeletonDemoIntent()
    data object SimulateDataLoading : SkeletonDemoIntent()
}

/**
 * SkeletonDemo ViewModel
 */
class SkeletonDemoViewModel : MviUiViewModel<SkeletonDemoIntent>() {

    private val _showViewSkeleton = MutableStateFlow(false)
    val showViewSkeleton: StateFlow<Boolean> = _showViewSkeleton

    private val _showListSkeleton = MutableStateFlow(false)
    val showListSkeleton: StateFlow<Boolean> = _showListSkeleton

    private val _listData = MutableStateFlow<List<String>>(emptyList())
    val listData: StateFlow<List<String>> = _listData

    override fun handleIntent(intent: SkeletonDemoIntent) {
        when (intent) {
            is SkeletonDemoIntent.ShowViewSkeleton -> {
                _showViewSkeleton.value = true
            }
            is SkeletonDemoIntent.HideViewSkeleton -> {
                _showViewSkeleton.value = false
            }
            is SkeletonDemoIntent.ShowListSkeleton -> {
                _showListSkeleton.value = true
            }
            is SkeletonDemoIntent.HideListSkeleton -> {
                _showListSkeleton.value = false
            }
            is SkeletonDemoIntent.SimulateDataLoading -> {
                simulateDataLoading()
            }
        }
    }

    /**
     * 模拟数据加载
     * 演示完整的骨架屏流程
     */
    private fun simulateDataLoading() {
        viewModelScope.launch {
            // 1. 显示骨架屏
            _showViewSkeleton.value = true
            _showListSkeleton.value = true

            // 2. 模拟网络延迟
            delay(3000)

            // 3. 隐藏骨架屏，显示数据
            _showViewSkeleton.value = false
            _showListSkeleton.value = false

            // 4. 更新数据
            _listData.value = listOf(
                "数据项 1",
                "数据项 2",
                "数据项 3",
                "数据项 4",
                "数据项 5"
            )

            showToast("数据加载完成")
        }
    }
}
