package com.mvi.core.base

/**
 * 分页数据封装
 *
 * @param T 数据类型
 * @param data 数据列表
 * @param currentPage 当前页码（从1开始）
 * @param totalPages 总页数
 * @param pageSize 每页数量
 * @param total 总数据量
 * @param hasMore 是否有更多数据
 */
data class PagedList<T>(
    val data: List<T>,
    val currentPage: Int,
    val totalPages: Int,
    val pageSize: Int,
    val total: Int = data.size,
    val hasMore: Boolean = currentPage < totalPages
) {
    companion object {
        /**
         * 创建空数据
         */
        fun <T> empty(): PagedList<T> {
            return PagedList(
                data = emptyList(),
                currentPage = 1,
                totalPages = 0,
                pageSize = 20,
                total = 0,
                hasMore = false
            )
        }

        /**
         * 从列表创建（不分页）
         */
        fun <T> fromList(list: List<T>): PagedList<T> {
            return PagedList(
                data = list,
                currentPage = 1,
                totalPages = 1,
                pageSize = list.size,
                total = list.size,
                hasMore = false
            )
        }
    }

    /**
     * 获取下一页页码
     */
    fun nextPage(): Int? {
        return if (hasMore) currentPage + 1 else null
    }

    /**
     * 是否为第一页
     */
    fun isFirstPage(): Boolean {
        return currentPage == 1
    }

    /**
     * 是否为最后一页
     */
    fun isLastPage(): Boolean {
        return !hasMore
    }
}

/**
 * 分页状态封装
 *
 * 优化点:
 * 1. 支持首次加载、加载更多、无更多数据等状态
 * 2. 保留当前数据，避免加载时闪烁
 * 3. 清晰的状态转换
 *
 * @param T 数据类型
 */
sealed class PagingState<out T> {

    /**
     * 空闲状态
     */
    data object Idle : PagingState<Nothing>()

    /**
     * 首次加载中
     */
    data object Loading : PagingState<Nothing>()

    /**
     * 加载更多中（保留当前数据）
     *
     * @param currentData 当前已加载的数据
     */
    data class LoadingMore<T>(val currentData: List<T>) : PagingState<T>()

    /**
     * 加载成功
     *
     * @param data 分页数据
     */
    data class Success<T>(val data: PagedList<T>) : PagingState<T>()

    /**
     * 加载失败
     *
     * @param message 错误消息
     * @param currentData 当前已加载的数据（如果有的话）
     */
    data class Error(val message: String, val currentData: List<Any>? = null) : PagingState<Nothing>()

    /**
     * 空数据
     */
    data object Empty : PagingState<Nothing>()

    /**
     * 没有更多数据
     *
     * @param currentData 当前已加载的所有数据
     */
    data class NoMoreData<T>(val currentData: List<T>) : PagingState<T>()

    /**
     * 是否为加载状态
     */
    fun isLoading(): Boolean {
        return this is Loading || this is LoadingMore
    }

    /**
     * 是否为成功状态
     */
    fun isSuccess(): Boolean {
        return this is Success
    }

    /**
     * 是否为错误状态
     */
    fun isError(): Boolean {
        return this is Error
    }

    /**
     * 是否为空状态
     */
    fun isEmpty(): Boolean {
        return this is Empty
    }

    /**
     * 是否可以加载更多
     */
    fun canLoadMore(): Boolean {
        return when (this) {
            is Success -> data.hasMore
            is NoMoreData -> false
            else -> false
        }
    }

    /**
     * 获取当前数据列表
     */
    fun getData(): List<*> {
        return when (this) {
            is Success -> data.data
            is LoadingMore -> currentData
            is NoMoreData -> currentData
            is Error -> currentData ?: emptyList()
            else -> emptyList()
        }
    }
}

/**
 * 分页意图接口
 * 用于标记分页相关的 Intent
 */
interface PagingIntent : MviIntent {
    /**
     * 首次加载或刷新
     */
    data object LoadFirst : PagingIntent

    /**
     * 加载更多
     */
    data object LoadMore : PagingIntent

    /**
     * 重试（失败后重新加载）
     */
    data object Retry : PagingIntent
}
