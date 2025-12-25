package com.mvi.demo.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mvi.core.base.UiState
import com.mvi.demo.databinding.ActivityUserListBinding
import com.mvi.demo.databinding.ItemUserBinding
import com.mvi.ui.base.MviUiActivity
import com.mvi.ui.ext.gone
import com.mvi.ui.ext.visible
import com.mvi.ui.widget.EmptyStateConfig
import kotlinx.coroutines.launch

/**
 * 用户列表示例 - 展示 MviUiActivity 的使用
 *
 * 特性演示:
 * 1. 继承 MviUiActivity，自动支持空状态/错误状态管理
 * 2. ViewModel 自动通过 MviUiEvent 显示空状态和错误状态
 * 3. 支持下拉刷新
 * 4. 展示数据列表
 */
class UserListActivity : MviUiActivity<ActivityUserListBinding, UserListViewModel, UserListIntent>() {

    private val userAdapter by lazy {
        UserAdapter(
            onDeleteClick = { user ->
                sendIntent(UserListIntent.DeleteUser(user.id))
            }
        )
    }

    override fun createBinding() = ActivityUserListBinding.inflate(layoutInflater)

    override fun getViewModelClass() = UserListViewModel::class.java

    override fun initView() {
        setupToolbar()
        setupRecyclerView()
        setupRefresh()

        // 首次加载
        sendIntent(UserListIntent.LoadUsers)
    }

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
            sendIntent(UserListIntent.RefreshUsers)
        }

        // 模拟状态切换按钮
        binding.btnToggleState.setOnClickListener {
            sendIntent(UserListIntent.LoadUsers)
        }
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userState.collect { state ->
                    binding.swipeRefresh.isRefreshing = state is UiState.Loading

                    when (state) {
                        is UiState.Success -> {
                            binding.recyclerView.visible()
                            userAdapter.submitList(state.data)
                        }
                        is UiState.Loading -> {
                            // Loading 由 ViewModel 的 showLoading 处理
                        }
                        is UiState.Empty, is UiState.Error -> {
                            // 空状态和错误状态由 ViewModel 通过 MviUiEvent 自动处理
                            binding.recyclerView.gone()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    /**
     * 自定义缺省页配置
     */
    override fun getEmptyStateConfig() = EmptyStateConfig(
        emptyMessage = "还没有用户哦~",
        errorMessage = "加载失败了",
        loadingMessage = "加载中..."
    )

    override fun handleLoading(show: Boolean) {
        // 可以在这里显示自定义的 Loading 对话框
        // 默认情况下，ViewModel 会通过 MviUiEvent 自动处理
    }
}

/**
 * 用户列表适配器
 * 优化：使用 ListAdapter + DiffUtil 提升性能
 */
class UserAdapter(
    private val onDeleteClick: (User) -> Unit
) : androidx.recyclerview.widget.ListAdapter<User, UserAdapter.UserViewHolder>(
    object : androidx.recyclerview.widget.DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(
        private val binding: ItemUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.tvName.text = user.name
            binding.tvEmail.text = user.email

            binding.btnDelete.setOnClickListener {
                onDeleteClick(user)
            }
        }
        
        /**
         * 清理监听器 - RecyclerView 回收时自动调用
         */
        fun unbind() {
            binding.btnDelete.setOnClickListener(null)
        }
    }
    
    override fun onViewRecycled(holder: UserViewHolder) {
        super.onViewRecycled(holder)
        holder.unbind()
    }
}
