package com.mvi.example.ui

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.mvi.core.base.UiState
import com.mvi.demo.databinding.ActivityUserListBinding
import com.mvi.ui.base.MviUiActivity
import com.mvi.ui.ext.gone
import com.mvi.ui.ext.visible
import kotlinx.coroutines.launch

/**
 * 基础页面模板 - MviUiActivity
 * 
 * 这是一个完整的示例，展示了如何使用 MviUiActivity 创建页面
 * 
 * 特性：
 * 1. 自动管理空状态和错误状态
 * 2. 支持下拉刷新
 * 3. 支持骨架屏
 * 4. 生命周期安全的 Flow 收集
 */
class UserListActivity : MviUiActivity<ActivityUserListBinding, UserViewModel, UserIntent>() {

    // 列表适配器
    private val userAdapter by lazy {
        UserAdapter(
            onItemClick = { user ->
                // 点击项处理
                navigateToUserDetail(user.id)
            },
            onDeleteClick = { user ->
                // 删除项处理
                sendIntent(UserIntent.DeleteUser(user.id))
            }
        )
    }

    // ========== 必须实现的方法 ==========
    
    override fun createBinding() = ActivityUserListBinding.inflate(layoutInflater)

    override fun getViewModelClass() = UserViewModel::class.java

    override fun initView() {
        setupToolbar()
        setupRecyclerView()
        setupRefresh()
        
        // 首次加载数据
        sendIntent(UserIntent.LoadUsers)
    }

    override fun observeData() {
        // ✅ 正确：使用 repeatOnLifecycle 确保生命周期安全
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userState.collect { state ->
                    handleState(state)
                }
            }
        }
        
        // ✅ 正确：观察一次性事件
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEvent.collect { event ->
                    handleEvent(event)
                }
            }
        }
    }

    // ========== 可选配置方法 ==========
    
    /**
     * 自定义空状态配置（可选）
     */
    override fun getEmptyStateConfig() = EmptyStateConfig(
        emptyMessage = "还没有用户哦~",
        errorMessage = "加载失败，请重试",
        loadingMessage = "加载中..."
    )

    /**
     * 是否显示通用标题栏（可选）
     */
    override fun showCommonTitleBar(): Boolean = true

    /**
     * 配置通用标题栏（可选）
     */
    override fun setupTitleBar(titleBar: CommonTitleBar) {
        titleBar.apply {
            setTitle("用户列表")
            setOnLeftClickListener { finish() }
            setRightText("添加")
            setOnRightClickListener {
                navigateToAddUser()
            }
        }
    }

    // ========== 私有方法 ==========
    
    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@UserListActivity)
            adapter = userAdapter
        }
    }

    private fun setupRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            sendIntent(UserIntent.RefreshUsers)
        }
    }

    /**
     * 处理状态变化
     */
    private fun handleState(state: UiState<List<User>>) {
        // 更新下拉刷新状态
        binding.swipeRefresh.isRefreshing = state is UiState.Loading

        when (state) {
            is UiState.Idle -> {
                // 初始状态，不处理
            }
            
            is UiState.Loading -> {
                // Loading 状态由 ViewModel 的 showLoading 自动处理
                // 或者可以在这里显示骨架屏
                // SkeletonManager.showSkeleton(binding.recyclerView)
            }
            
            is UiState.Success -> {
                // ✅ 显示数据
                binding.recyclerView.visible()
                userAdapter.submitList(state.data)
                
                // 隐藏骨架屏（如果显示）
                // SkeletonManager.hideSkeleton()
            }
            
            is UiState.Empty -> {
                // 空状态由 MviUiActivity 自动处理
                // 或者可以在这里自定义处理
                binding.recyclerView.gone()
            }
            
            is UiState.Error -> {
                // 错误状态由 MviUiActivity 自动处理
                // 或者可以在这里自定义处理
                binding.recyclerView.gone()
            }
            
            is UiState.NetworkError -> {
                // 网络错误状态由 MviUiActivity 自动处理
                binding.recyclerView.gone()
            }
        }
    }

    /**
     * 处理一次性事件
     */
    private fun handleEvent(event: UiEvent) {
        when (event) {
            is UiEvent.ShowToast -> {
                // Toast 已在 MviActivity 中自动处理
                // 如果需要自定义，可以重写 showToast 方法
            }
            
            is UiEvent.ShowLoading -> {
                // Loading 已在 MviActivity 中自动处理
                // 如果需要自定义，可以重写 handleLoading 方法
            }
            
            is UiEvent.Navigate -> {
                // 导航事件需要自己处理
                navigateTo(event.route)
            }
        }
    }

    /**
     * 导航到用户详情
     */
    private fun navigateToUserDetail(userId: String) {
        // 使用导航工具
        // NavigationHelper.navigateToUserDetail(this, userId)
    }

    /**
     * 导航到添加用户
     */
    private fun navigateToAddUser() {
        // 使用导航工具
        // NavigationHelper.navigateToAddUser(this)
    }
}
