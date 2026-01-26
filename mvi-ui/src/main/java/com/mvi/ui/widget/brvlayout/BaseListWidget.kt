package com.mvi.ui.widget.brvlayout

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.mvi.core.base.UiState
import com.mvi.ui.R
import com.mvi.ui.databinding.LayoutBaseListBinding
import com.drake.brv.PageRefreshLayout
import com.drake.brv.utils.models
import com.mvi.ui.widget.RecyclerViewSkeletonManager
import com.drake.brv.utils.setup
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 通用列表组件
 * 基于PageRefreshLayout和BRV库实现的通用列表组件
 *
 * 特性：
 * 1. 自动处理 UiState 状态变化（Loading、Empty、Error、Success）
 * 2. 自动处理分页逻辑（第一页/加载更多）
 * 3. 内置骨架屏和缺省页
 * 4. 支持 configure + setup 两步初始化
 * 5. 支持扩展（open class）
 *
 * @param T 列表数据类型
 */
open class BaseListWidget<T> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding: LayoutBaseListBinding = LayoutBaseListBinding.inflate(
        LayoutInflater.from(context), this, true
    )

    // 配置参数
    private var config: ListWidgetConfig = ListWidgetConfig()

    // 页码（从1开始）
    private var currentPage = 1

    // 是否还有更多数据
    private var hasMoreData = true

    // 骨架屏管理器
    private lateinit var skeletonManager: RecyclerViewSkeletonManager

    // 分页信息提取器（从 UiState.Success 中提取）
    private var pageInfoExtractor: ((Any?) -> PageInfo)? = null

    // 数据绑定的协程 Job（用于取消，防止内存泄漏）
    private var bindDataJob: Job? = null

    init {
        initializeViews()
    }

    /**
     * 当 View 从窗口分离时自动清理资源
     * 防止内存泄漏
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        release()
    }

    private fun initializeViews() {
        // 初始化骨架屏管理器
        skeletonManager = RecyclerViewSkeletonManager(
            recyclerView = binding.recyclerView,
            skeletonLayoutResId = R.layout.item_skeleton_default,
            itemCount = 10
        )
    }

    private fun initializeLoadMore() {
        if (config.enableLoadMore) {
            // 启用加载更多功能
            binding.swipeRefresh.onLoadMore {
                onLoadMore()
            }
        }
    }

    /**
     * 显示加载中状态
     */
    fun showLoading() {
        hideSkeleton() // 先隐藏骨架屏
        binding.swipeRefresh.showLoading()
    }

    /**
     * 显示空数据状态
     */
    fun showEmpty(message: String = config.emptyMessage) {
        hideSkeleton() // 先隐藏骨架屏
        binding.swipeRefresh.showEmpty()
    }

    /**
     * 显示错误状态
     */
    fun showError(message: String = config.errorMessage) {
        hideSkeleton() // 先隐藏骨架屏
        binding.swipeRefresh.showError()
    }

    /**
     * 隐藏缺省页
     */
    fun hideEmptyState() {
        binding.swipeRefresh.showContent()
    }

    /**
     * 显示骨架屏
     */
    fun showSkeleton() {
        hideEmptyState() // 先隐藏缺省页
        skeletonManager.show()
    }

    /**
     * 隐藏骨架屏
     */
    fun hideSkeleton() {
        skeletonManager.hide()
    }

    /**
     * 显示列表内容（隐藏所有缺省页和骨架屏）
     */
    fun showListContent() {
        hideEmptyState()
        hideSkeleton()
    }

    /**
     * 配置组件
     */
    fun configure(configuration: ListWidgetConfig.() -> Unit) {
        config = ListWidgetConfig().apply(configuration)
        applyConfig()
    }

    private fun applyConfig() {
        // 设置下拉刷新回调
        binding.swipeRefresh.onRefresh {
            onRefresh()
        }

        // 初始化加载更多功能
        initializeLoadMore()
    }

    /**
     * 使用 BRV 的 setup 函数初始化 RecyclerView
     * @param setupBlock BRV setup 配置块
     * @return RecyclerView.Adapter
     */
    fun <A : RecyclerView.Adapter<*>> setup(setupBlock: RecyclerView.() -> A): A {
        val adapter = binding.recyclerView.setupBlock()
        return adapter
    }

    /**
     * 下拉刷新
     */
    private fun onRefresh() {
        currentPage = 1
        config.onRefresh?.invoke()
    }

    /**
     * 加载更多
     */
    private fun onLoadMore() {
        if (hasMoreData && config.enableLoadMore) {
            currentPage++ // 自动递增页码
            config.onLoadMore?.invoke(currentPage)
        }
    }

    /**
     * 设置当前页码（用于手动控制分页）
     * @param page 页码
     */
    fun setPage(page: Int) {
        currentPage = page
    }

    /**
     * 自动判断是否有更多数据
     * @param hasMore 是否还有更多数据
     * @param pageSize 页面大小，默认为20
     * @param totalCount 总数据量
     */
    fun autoCalculateHasMore(hasMore: Boolean? = null, pageSize: Int = 20, totalCount: Int? = null) {
        if (hasMore != null) {
            // 如果明确指定了是否有更多数据，则直接使用
            hasMoreData = hasMore
        } else if (totalCount != null) {
            // 根据总数量和当前页码计算是否有更多
            val expectedTotal = currentPage * pageSize
            hasMoreData = expectedTotal < totalCount
        } else {
            // 默认策略：如果当前页面数据满额，则认为可能还有更多
            val currentDataCount = getRecyclerView().models?.size ?: 0
            hasMoreData = currentDataCount >= pageSize
        }
    }

    /**
     * 设置是否有更多数据
     */
    fun setHasMore(hasMore: Boolean) {
        hasMoreData = hasMore
        if (!hasMore) {
            // 当没有更多数据时，可以显示提示
            config.onNoMore?.invoke()
        }
    }

    /**
     * 结束刷新状态
     */
    fun finishRefresh() {
        binding.swipeRefresh.finish()
    }

    /**
     * 结束加载更多状态
     */
    fun finishLoadMore() {
        binding.swipeRefresh.finishLoadMore()
    }

    /**
     * 结束加载更多并显示没有更多数据
     */
    fun finishLoadMoreWithNoMoreData() {
        binding.swipeRefresh.finishLoadMoreWithNoMoreData()
    }

    /**
     * 隐藏骨架屏并显示内容
     */
    fun finishShowContent() {
        hideSkeleton()
        showListContent()
    }

    /**
     * 获取PageRefreshLayout
     */
    fun getPageRefreshLayout(): PageRefreshLayout = binding.swipeRefresh

    /**
     * 获取RecyclerView
     */
    fun getRecyclerView() = binding.recyclerView

    /**
     * 释放资源
     *
     * 注意：通常不需要手动调用，onDetachedFromWindow 会自动调用
     * 但在某些特殊情况下（如 Fragment 快速切换），可以手动调用
     */
    open fun release() {
        // 1. 取消协程，防止内存泄漏
        bindDataJob?.cancel()
        bindDataJob = null

        // 2. 释放骨架屏管理器
        if (::skeletonManager.isInitialized) {
            skeletonManager.release()
        }

        // 3. 清空回调引用，防止持有外部引用
        config = ListWidgetConfig()
        pageInfoExtractor = null

        // 4. 清理 RecyclerView
        binding.recyclerView.adapter = null
    }

    // ========== 新增：自动状态绑定 ==========

    /**
     * 绑定 UiState，自动处理状态变化
     *
     * 使用示例：
     * ```kotlin
     * binding.listWidget.bindState(this, viewModel.uiState) { data ->
     *     // 可选：处理成功后的额外逻辑
     * }
     * ```
     *
     * @param lifecycleOwner 生命周期所有者
     * @param uiStateFlow UiState 流
     * @param onSuccess 成功时的额外回调（可选）
     */
    open fun bindData(
        lifecycleOwner: LifecycleOwner,
        uiStateFlow: StateFlow<UiState<List<T>>>,
        onSuccess: ((List<T>) -> Unit)? = null
    ) {
        // 取消之前的协程，防止重复绑定导致内存泄漏
        bindDataJob?.cancel()

        // 启动新的协程并保存引用
        bindDataJob = lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                uiStateFlow.collect { state ->
                    when (state) {
                        is UiState.Loading -> showLoading()
                        is UiState.Empty -> showEmpty()
                        is UiState.Error -> showError(state.message)
                        is UiState.Success -> {
                            val data = state.data
                            updateData(data)
                            // 使用弱引用或确保回调不会持有长生命周期引用
                            onSuccess?.invoke(data)
                        }
                        is UiState.Idle -> {}
                        is UiState.NetworkError -> showError(state.message)
                    }
                }
            }
        }
    }

    /**
     * 更新列表数据（自动处理分页）
     * @param data 列表数据
     * @param page 页码（可选，如果不传则使用内部维护的页码）
     */
    private fun updateData(data: List<T>) {
        if (currentPage == 1) {
            // 第一页：直接设置数据
            binding.recyclerView.models = data
        } else {
            // 加载更多：使用 BRV 的优化方法追加数据
            // PageRefreshLayout 的 addData 方法内部会优化增量更新
            binding.swipeRefresh.addData(data)
        }

        // 显示内容并隐藏骨架屏
        finishShowContent()

        // 自动计算是否有更多数据
        if (pageInfoExtractor != null) {
            // 使用自定义提取器
            autoCalculateHasMoreWithExtractor()
        }
    }

    /**
     * 设置分页信息提取器
     * 用于从 UiState.Success.data 中提取分页信息
     *
     * @param extractor 分页信息提取器，返回 PageInfo
     *
     * 使用示例：
     * ```kotlin
     * binding.listWidget.setPageInfoExtractor { response ->
     *     PageInfo(
     *         page = response.page,
     *         pageSize = response.pageSize,
     *         total = response.total
     *     )
     * }
     * ```
     */
    fun setPageInfoExtractor(extractor: (Any?) -> PageInfo) {
        this.pageInfoExtractor = extractor
    }

    /**
     * 使用提取器自动计算是否有更多数据
     */
    private fun autoCalculateHasMoreWithExtractor() {
        val extractor = pageInfoExtractor ?: return
        val currentData = binding.recyclerView.models as? List<*> ?: return

        if (currentData.isNotEmpty()) {
            val firstItem = currentData.first()
            val pageInfo = extractor(firstItem)

            setPage(pageInfo.page)

            hasMoreData = when {
                pageInfo.total >= 0 -> {
                    // 有总数量，根据总数量计算
                    pageInfo.page * pageInfo.pageSize < pageInfo.total
                }
                else -> {
                    // 没有总数量，根据当前页数据量判断
                    currentData.size >= pageInfo.pageSize
                }
            }

            if (!hasMoreData) {
                finishLoadMoreWithNoMoreData()
            }
        }
    }

    /**
     * 手动添加数据（用于分页加载）
     * 使用 BRV 优化的 addData 方法，避免全量 Diff
     * @param data 新数据
     */
    fun addData(data: List<T>) {
        binding.swipeRefresh.addData(data)
    }

    /**
     * 设置数据（第一页）
     * @param data 数据列表
     */
    fun setModels(data: List<T>) {
        currentPage = 1
        binding.recyclerView.models = data
    }

    // ========== 局部刷新功能 ==========

    /**
     * 刷新指定位置的单个 item
     * @param position item 位置
     */
    fun refreshItem(position: Int) {
        binding.recyclerView.adapter?.notifyItemChanged(position)
    }

    /**
     * 刷新指定位置的 item（局部刷新，使用 Payload）
     * 只有在 onBind 中处理了 payload 才能生效
     *
     * 使用示例：
     * ```kotlin
     * binding.listWidget.refreshItem(0, "update_like")
     * ```
     *
     * 在 setup 中处理 payload：
     * ```kotlin
     * onBind {
     *     val model = getModel<UserModel>()
     *     val binding = ItemUserBinding.bind(itemView)
     *
     *     // 处理 payload
     *     if (payload.isNotEmpty()) {
     *         when (payload[0]) {
     *             "update_like" -> binding.btnLike.isChecked = model.isLiked
     *         }
     *         return@onBind
     *     }
     *
     *     // 完整绑定
     *     binding.tvName.text = model.name
     *     binding.btnLike.isChecked = model.isLiked
     * }
     * ```
     *
     * @param position item 位置
     * @param payload 负载数据（任意类型）
     */
    fun refreshItem(position: Int, payload: Any) {
        binding.recyclerView.adapter?.notifyItemChanged(position, payload)
    }

    /**
     * 刷新指定数据的 item（自动查找位置）
     * @param item 要刷新的数据
     * @return 是否找到并刷新
     */
    fun refreshItem(item: T): Boolean {
        val position = findItemPosition(item)
        return if (position >= 0) {
            refreshItem(position)
            true
        } else {
            false
        }
    }

    /**
     * 刷新指定数据的 item（局部刷新）
     * @param item 要刷新的数据
     * @param payload 负载数据
     * @return 是否找到并刷新
     */
    fun refreshItem(item: T, payload: Any): Boolean {
        val position = findItemPosition(item)
        return if (position >= 0) {
            refreshItem(position, payload)
            true
        } else {
            false
        }
    }

    /**
     * 批量刷新多个 item
     * @param positions item 位置列表
     */
    fun refreshItems(positions: List<Int>) {
        positions.forEach { position ->
            binding.recyclerView.adapter?.notifyItemChanged(position)
        }
    }

    /**
     * 批量刷新多个 item（局部刷新）
     * @param refreshData 位置和 payload 的映射
     */
    fun refreshItems(refreshData: Map<Int, Any>) {
        refreshData.forEach { (position, payload) ->
            binding.recyclerView.adapter?.notifyItemChanged(position, payload)
        }
    }

    /**
     * 刷新指定范围的 item
     * @param startPosition 起始位置
     * @param count 数量
     */
    fun refreshRange(startPosition: Int, count: Int) {
        binding.recyclerView.adapter?.notifyItemRangeChanged(startPosition, count)
    }

    /**
     * 刷新指定范围的 item（局部刷新）
     * @param startPosition 起始位置
     * @param count 数量
     * @param payload 负载数据
     */
    fun refreshRange(startPosition: Int, count: Int, payload: Any) {
        binding.recyclerView.adapter?.notifyItemRangeChanged(startPosition, count, payload)
    }

    /**
     * 插入单个 item
     * @param position 插入位置
     * @param item 数据
     */
    fun insertItem(position: Int, item: T) {
        val currentModels = (binding.recyclerView.models as? MutableList<T>) ?: mutableListOf()
        currentModels.add(position, item)
        binding.recyclerView.adapter?.notifyItemInserted(position)
    }

    /**
     * 移除指定位置的 item
     * @param position 位置
     * @return 被移除的 item
     */
    fun removeItemAt(position: Int): T? {
        val currentModels = binding.recyclerView.models as? MutableList<T> ?: return null
        if (position in currentModels.indices) {
            val item = currentModels.removeAt(position)
            binding.recyclerView.adapter?.notifyItemRemoved(position)
            return item
        }
        return null
    }

    /**
     * 移除指定的 item
     * @param item 要移除的数据
     * @return 是否移除成功
     */
    fun removeItem(item: T): Boolean {
        val currentModels = binding.recyclerView.models as? MutableList<T> ?: return false
        val position = currentModels.indexOf(item)
        return if (position >= 0) {
            currentModels.removeAt(position)
            binding.recyclerView.adapter?.notifyItemRemoved(position)
            true
        } else {
            false
        }
    }

    /**
     * 移动 item
     * @param fromPosition 源位置
     * @param toPosition 目标位置
     */
    fun moveItem(fromPosition: Int, toPosition: Int) {
        val currentModels = binding.recyclerView.models as? MutableList<T> ?: return
        if (fromPosition in currentModels.indices && toPosition in currentModels.indices) {
            val item = currentModels.removeAt(fromPosition)
            currentModels.add(toPosition, item)
            binding.recyclerView.adapter?.notifyItemMoved(fromPosition, toPosition)
        }
    }

    /**
     * 查找 item 的位置
     * @param item 要查找的数据
     * @return 位置，未找到返回 -1
     */
    private fun findItemPosition(item: T): Int {
        val currentModels = binding.recyclerView.models as? List<T> ?: return -1
        return currentModels.indexOf(item)
    }

    /**
     * 获取当前数据列表
     * @return 数据列表的不可变副本
     */
    fun getCurrentList(): List<T> {
        return (binding.recyclerView.models as? List<T>)?.toList() ?: emptyList()
    }

    /**
     * 获取数据数量
     * @return 数据数量
     */
    fun getItemCount(): Int {
        return binding.recyclerView.adapter?.itemCount ?: 0
    }
}

/**
 * 分页信息
 */
data class PageInfo(
    val page: Int = 1,           // 当前页码
    val pageSize: Int = 10,      // 每页数量
    val total: Int = -1          // 总数量（-1 表示未知）
)

/**
 * 列表组件配置类
 */
data class ListWidgetConfig(
    var enableRefresh: Boolean = true,
    var enableLoadMore: Boolean = true,
    var refreshColors: IntArray = intArrayOf(
        android.R.color.holo_blue_light,

        android.R.color.holo_green_light,
        android.R.color.holo_orange_light,
        android.R.color.holo_red_light
    ),
    var emptyMessage: String = "暂无数据",
    var errorMessage: String = "加载失败",
    var skeletonLayoutResId: Int = R.layout.item_skeleton_default,
    var skeletonItemCount: Int = 10,
    // 回调函数
    var onRefresh: (() -> Unit)? = null,
    var onLoadMore: ((page: Int) -> Unit)? = null,
    var onNoMore: (() -> Unit)? = null,
    var onRetry: (() -> Unit)? = null
)
