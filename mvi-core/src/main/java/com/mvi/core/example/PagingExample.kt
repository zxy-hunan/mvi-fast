package com.mvi.core.example

import com.mvi.core.base.MviIntent
import com.mvi.core.base.MviViewModel
import com.mvi.core.base.PagingState
import com.mvi.core.base.PagedList
import com.mvi.core.base.PagingIntent
import com.mvi.core.ext.launchPagingRequest
import com.mvi.core.ext.canLoadMore
import com.mvi.core.ext.getData
import com.mvi.core.ext.resetPaging
import com.mvi.core.network.ApiResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 分页使用示例
 *
 * 完整示例展示了如何使用分页功能
 */

// ==================== 1. 定义 Intent ====================

sealed class UserIntent : MviIntent {
    // 分页相关 Intent
    data object LoadUsers : UserIntent(), PagingIntent
    data object LoadMore : UserIntent(), PagingIntent
    data object Refresh : UserIntent(), PagingIntent
    data object Retry : UserIntent(), PagingIntent

    // 其他业务 Intent
    data class DeleteUser(val userId: String) : UserIntent()
    data class SearchUser(val keyword: String) : UserIntent()
}

// ==================== 2. API 响应格式 ====================

/**
 * 用户数据
 */
data class User(
    val id: String,
    val name: String,
    val email: String
)

/**
 * API 服务接口（示例）
 */
interface UserService {
    /**
     * 获取用户列表（分页）
     *
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @return ApiResponse<PagedList<User>>
     */
    suspend fun getUsers(page: Int, size: Int): ApiResponse<PagedList<User>>
}

/**
 * 原始分页响应（示例）
 */
data class PagedUserResponse(
    val data: List<User>,
    val page: Int,
    val size: Int,
    val total: Int,
    val totalPages: Int
) {
    /**
     * 转换为 PagedList
     */
    fun toPagedList(): PagedList<User> {
        return PagedList(
            data = data,
            currentPage = page,
            totalPages = totalPages,
            pageSize = size,
            total = total,
            hasMore = page < totalPages
        )
    }

    /**
     * 转换为 ApiResponse
     */
    fun toApiResponse(): ApiResponse<PagedList<User>> {
        return ApiResponse(code = 200, message = "success", data = toPagedList())
    }
}

// ==================== 3. ViewModel 实现 ====================

/**
 * 用户列表 ViewModel（分页示例）
 */
class UserPagingViewModel(
    private val userService: UserService
) : MviViewModel<UserIntent>() {

    // 分页配置
    private var currentPage = 1
    private val pageSize = 20

    // 所有已加载的数据（用于展示）
    private val allUsers = mutableListOf<User>()

    // 分页状态
    private val _pagingState = MutableStateFlow<PagingState<User>>(PagingState.Idle)
    val pagingState = _pagingState.asStateFlow()

    override fun handleIntent(intent: UserIntent) {
        when (intent) {
            // 首次加载
            is UserIntent.LoadUsers -> loadUsers(refresh = false)

            // 加载更多
            is UserIntent.LoadMore -> loadMore()

            // 刷新（重置到第一页）
            is UserIntent.Refresh -> loadUsers(refresh = true)

            // 重试（失败后重新加载）
            is UserIntent.Retry -> retry()

            // 其他业务逻辑
            is UserIntent.DeleteUser -> deleteUser(intent.userId)
            is UserIntent.SearchUser -> searchUser(intent.keyword)
        }
    }

    /**
     * 加载用户列表
     *
     * @param refresh 是否刷新（清空当前数据）
     */
    private fun loadUsers(refresh: Boolean) {
        if (refresh) {
            // 重置状态
            currentPage = 1
            allUsers.clear()
            _pagingState.resetPaging()
        }

        launchPagingRequest(
            stateFlow = _pagingState,
            currentPage = currentPage,
            pageSize = pageSize,
            currentData = allUsers.toList(),
            request = { page, size ->
                userService.getUsers(page, size)
            }
        ) { pagedData ->
            // 成功回调：更新本地数据
            if (pagedData.currentPage == 1) {
                allUsers.clear()
            }
            allUsers.addAll(pagedData.data)
            currentPage++ // 准备加载下一页
        }
    }

    /**
     * 加载更多
     */
    private fun loadMore() {
        // 检查是否可以加载更多
        if (!_pagingState.canLoadMore()) {
            return
        }

        // 检查是否正在加载
        if (_pagingState.value.isLoading()) {
            return
        }

        launchPagingRequest(
            stateFlow = _pagingState,
            currentPage = currentPage,
            pageSize = pageSize,
            currentData = allUsers.toList(),
            request = { page, size ->
                userService.getUsers(page, size)
            }
        ) { pagedData ->
            allUsers.addAll(pagedData.data)
            currentPage++
        }
    }

    /**
     * 重试
     */
    private fun retry() {
        // 如果当前有数据，保持数据并重试最后一页
        if (allUsers.isNotEmpty()) {
            launchPagingRequest(
                stateFlow = _pagingState,
                currentPage = currentPage,
                pageSize = pageSize,
                currentData = allUsers.toList(),
                request = { page, size ->
                    userService.getUsers(page, size)
                }
            ) { pagedData ->
                if (pagedData.currentPage == 1) {
                    allUsers.clear()
                }
                allUsers.addAll(pagedData.data)
                currentPage++
            }
        } else {
            // 没有数据，重新加载第一页
            loadUsers(refresh = false)
        }
    }

    /**
     * 删除用户
     */
    private fun deleteUser(userId: String) {
        // 删除逻辑...
        allUsers.removeAll { it.id == userId }
        // 刷新当前页
        loadUsers(refresh = true)
    }

    /**
     * 搜索用户
     */
    private fun searchUser(keyword: String) {
        // 搜索逻辑...
        loadUsers(refresh = true)
    }

    /**
     * 获取当前所有数据
     */
    fun getCurrentData(): List<User> {
        return allUsers.toList()
    }
}

// ==================== 4. Activity/Fragment 使用示例 ====================

/**
 * 用户列表 Activity 使用示例
 *
 * 注：这是伪代码，展示如何使用
 */
class UserPagingActivityExample /* : MviActivity<ActivityUserBinding, UserPagingViewModel, UserIntent>() */ {

    /*
    private lateinit var adapter: UserAdapter
    private var isLoadingMore = false

    override fun observeData() {
        // 观察分页状态
        viewModel.pagingState.collectOn(this) { state ->
            when (state) {
                is PagingState.Loading -> {
                    // 首次加载中
                    showLoading()
                }

                is PagingState.LoadingMore -> {
                    // 加载更多中
                    isLoadingMore = true
                    adapter.showLoadingMore()
                }

                is PagingState.Success -> {
                    // 加载成功
                    hideLoading()
                    isLoadingMore = false
                    adapter.hideLoadingMore()

                    val allData = viewModel.getCurrentData()
                    adapter.setNewData(allData)
                }

                is PagingState.NoMoreData -> {
                    // 没有更多数据
                    hideLoading()
                    isLoadingMore = false
                    adapter.hideLoadingMore()
                    adapter.showNoMoreData()

                    val allData = viewModel.getCurrentData()
                    adapter.setNewData(allData)
                }

                is PagingState.Error -> {
                    // 加载失败
                    hideLoading()
                    isLoadingMore = false
                    adapter.hideLoadingMore()

                    if (state.currentData.isNullOrEmpty()) {
                        // 首次加载失败，显示错误页
                        showError(state.message) {
                            sendIntent(UserIntent.Retry)
                        }
                    } else {
                        // 加载更多失败，提示重试
                        showToast(state.message)
                    }
                }

                is PagingState.Empty -> {
                    // 空数据
                    hideLoading()
                    isLoadingMore = false
                    showEmpty()
                }

                else -> {}
            }
        }
    }

    override fun initView() {
        // 初始化 RecyclerView
        adapter = UserAdapter()
        binding.recyclerView.adapter = adapter

        // 设置滚动监听
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                // 检查是否滚动到底部
                if (isScrolledToBottom() && !isLoadingMore) {
                    // 触发加载更多
                    sendIntent(UserIntent.LoadMore)
                }
            }
        })

        // 下拉刷新
        binding.swipeRefresh.setOnRefreshListener {
            sendIntent(UserIntent.Refresh)
        }

        // 首次加载
        sendIntent(UserIntent.LoadUsers)
    }

    /**
     * 检查是否滚动到底部
     */
    private fun isScrolledToBottom(): Boolean {
        val layoutManager = binding.recyclerView.layoutManager as? LinearLayoutManager ?: return false
        val visibleItemCount = layoutManager.childCount
        val totalItemCount = layoutManager.itemCount
        val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

        return (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5 // 提前5项触发
    }
    */
}

// ==================== 5. Adapter 示例（伪代码） ====================

/*
class UserAdapter : RecyclerView.Adapter<UserAdapter.ViewHolder>() {
    private val data = mutableListOf<User>()
    private var loadingMoreEnabled = false
    private var noMoreDataEnabled = false

    fun setNewData(newData: List<User>) {
        data.clear()
        data.addAll(newData)
        notifyDataSetChanged()
    }

    fun showLoadingMore() {
        if (!loadingMoreEnabled) {
            loadingMoreEnabled = true
            notifyItemInserted(data.size)
        }
    }

    fun hideLoadingMore() {
        if (loadingMoreEnabled) {
            loadingMoreEnabled = false
            notifyItemRemoved(data.size)
        }
    }

    fun showNoMoreData() {
        noMoreDataEnabled = true
        notifyItemChanged(data.size)
    }

    override fun getItemCount(): Int {
        return data.size + (if (loadingMoreEnabled || noMoreDataEnabled) 1 else 0)
    }

    override fun getItemViewType(position: Int): Int {
        return when {
            position < data.size -> TYPE_ITEM
            loadingMoreEnabled -> TYPE_LOADING_MORE
            noMoreDataEnabled -> TYPE_NO_MORE_DATA
            else -> TYPE_ITEM
        }
    }

    // 其他实现...
}
*/
