package com.mvi.core.repository

import com.mvi.core.network.ApiResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * 分页数据源结果
 *
 * @param T 数据类型
 */
data class PagedSource<T>(
    val data: List<T>,
    val currentPage: Int,
    val totalPages: Int,
    val hasMore: Boolean
)

/**
 * Repository 扩展函数
 *
 * 提供便捷的数据获取方法
 */

/**
 * 获取单个数据
 *
 * @param dataSource 数据源
 * @param localQuery 本地查询
 * @param networkCall 网络请求
 * @param saveCallResult 保存结果
 * @return 数据
 */
suspend fun <T> fetchSingle(
    dataSource: DataSource? = null,
    localQuery: suspend () -> T?,
    networkCall: suspend () -> ApiResponse<T>,
    saveCallResult: suspend (T) -> Unit = {}
): T {
    val effectiveDataSource = DataSourceConfig.getEffectiveDataSource(dataSource)

    return when (effectiveDataSource) {
        DataSource.LOCAL_ONLY -> {
            localQuery.invoke() ?: throw Exception("Local data not found")
        }

        DataSource.NETWORK_ONLY -> {
            val response = networkCall.invoke()
            if (response.isSuccess() && response.data != null) {
                val data = response.data
                saveCallResult(data)
                data
            } else {
                throw Exception(response.message)
            }
        }

        DataSource.LOCAL_FIRST -> {
            val localData = localQuery.invoke()
            if (localData != null) {
                localData
            } else {
                val response = networkCall.invoke()
                if (response.isSuccess() && response.data != null) {
                    val data = response.data
                    saveCallResult(data)
                    data
                } else {
                    throw Exception(response.message)
                }
            }
        }

        DataSource.NETWORK_FIRST -> {
            try {
                val response = networkCall.invoke()
                if (response.isSuccess() && response.data != null) {
                    val data = response.data
                    saveCallResult(data)
                    data
                } else {
                    throw Exception(response.message)
                }
            } catch (e: Exception) {
                val localData = localQuery.invoke()
                localData ?: throw e
            }
        }

        DataSource.CACHE_THEN_NETWORK -> {
            val localData = localQuery.invoke()
            try {
                val response = networkCall.invoke()
                if (response.isSuccess() && response.data != null) {
                    val data = response.data
                    saveCallResult(data)
                    data
                } else {
                    throw Exception(response.message)
                }
            } catch (e: Exception) {
                localData ?: throw e
            }
        }
    }
}

/**
 * 获取数据流
 *
 * @param dataSource 数据源
 * @param localQuery 本地查询
 * @param networkCall 网络请求
 * @param saveCallResult 保存结果
 * @return Flow<T>
 */
fun <T> fetchFlow(
    dataSource: DataSource? = null,
    localQuery: suspend () -> T?,
    networkCall: suspend () -> ApiResponse<T>,
    saveCallResult: suspend (T) -> Unit = {}
): Flow<T> = flow {
    val effectiveDataSource = DataSourceConfig.getEffectiveDataSource(dataSource)

    when (effectiveDataSource) {
        DataSource.LOCAL_ONLY -> {
            val localData = localQuery.invoke()
            if (localData != null) {
                emit(localData)
            } else {
                throw Exception("Local data not found")
            }
        }

        DataSource.NETWORK_ONLY -> {
            val response = networkCall.invoke()
            if (response.isSuccess() && response.data != null) {
                val data = response.data
                saveCallResult(data)
                emit(data)
            } else {
                throw Exception(response.message)
            }
        }

        DataSource.LOCAL_FIRST -> {
            val localData = localQuery.invoke()
            if (localData != null) {
                emit(localData)
            }
            val response = networkCall.invoke()
            if (response.isSuccess() && response.data != null) {
                val data = response.data
                saveCallResult(data)
                emit(data)
            } else {
                if (localData == null) {
                    throw Exception(response.message)
                }
            }
        }

        DataSource.NETWORK_FIRST -> {
            try {
                val response = networkCall.invoke()
                if (response.isSuccess() && response.data != null) {
                    val data = response.data
                    saveCallResult(data)
                    emit(data)
                } else {
                    throw Exception(response.message)
                }
            } catch (e: Exception) {
                val localData = localQuery.invoke()
                if (localData != null) {
                    emit(localData)
                } else {
                    throw e
                }
            }
        }

        DataSource.CACHE_THEN_NETWORK -> {
            val localData = localQuery.invoke()
            if (localData != null) {
                emit(localData)
            }
            val response = networkCall.invoke()
            if (response.isSuccess() && response.data != null) {
                val data = response.data
                saveCallResult(data)
                emit(data)
            }
        }
    }
}.flowOn(kotlinx.coroutines.Dispatchers.IO).catch { e ->
    throw e
}

/**
 * 获取分页数据
 *
 * @param dataSource 数据源
 * @param page 页码
 * @param size 每页数量
 * @param localQuery 本地查询
 * @param networkCall 网络请求
 * @param saveCallResult 保存结果
 * @return PagedSource<T>
 */
suspend fun <T> fetchPaged(
    dataSource: DataSource? = null,
    page: Int,
    size: Int,
    localQuery: suspend (page: Int, size: Int) -> List<T>?,
    networkCall: suspend (page: Int, size: Int) -> ApiResponse<List<T>>,
    saveCallResult: suspend (List<T>) -> Unit = {}
): PagedSource<T> {
    val effectiveDataSource = DataSourceConfig.getEffectiveDataSource(dataSource)

    return when (effectiveDataSource) {
        DataSource.LOCAL_ONLY -> {
            val localData = localQuery.invoke(page, size)
            PagedSource(
                data = localData ?: emptyList(),
                currentPage = page,
                totalPages = if (localData != null && localData.size < size) page else page + 1,
                hasMore = localData != null && localData.size >= size
            )
        }

        DataSource.NETWORK_ONLY -> {
            val response = networkCall.invoke(page, size)
            if (response.isSuccess() && response.data != null) {
                val data = response.data
                saveCallResult(data)
                PagedSource(
                    data = data,
                    currentPage = page,
                    totalPages = page + 1, // 假设服务器返回
                    hasMore = data.size >= size
                )
            } else {
                throw Exception(response.message)
            }
        }

        DataSource.LOCAL_FIRST -> {
            val localData = localQuery.invoke(page, size)
            if (localData != null && localData.isNotEmpty()) {
                PagedSource(
                    data = localData,
                    currentPage = page,
                    totalPages = page + 1,
                    hasMore = localData.size >= size
                )
            } else {
                val response = networkCall.invoke(page, size)
                if (response.isSuccess() && response.data != null) {
                    val data = response.data
                    saveCallResult(data)
                    PagedSource(
                        data = data,
                        currentPage = page,
                        totalPages = page + 1,
                        hasMore = data.size >= size
                    )
                } else {
                    throw Exception(response.message)
                }
            }
        }

        DataSource.NETWORK_FIRST -> {
            try {
                val response = networkCall.invoke(page, size)
                if (response.isSuccess() && response.data != null) {
                    val data = response.data
                    saveCallResult(data)
                    PagedSource(
                        data = data,
                        currentPage = page,
                        totalPages = page + 1,
                        hasMore = data.size >= size
                    )
                } else {
                    throw Exception(response.message)
                }
            } catch (e: Exception) {
                val localData = localQuery.invoke(page, size)
                PagedSource(
                    data = localData ?: emptyList(),
                    currentPage = page,
                    totalPages = page,
                    hasMore = false
                )
            }
        }

        DataSource.CACHE_THEN_NETWORK -> {
            val localData = localQuery.invoke(page, size)
            try {
                val response = networkCall.invoke(page, size)
                if (response.isSuccess() && response.data != null) {
                    val data = response.data
                    saveCallResult(data)
                    PagedSource(
                        data = data,
                        currentPage = page,
                        totalPages = page + 1,
                        hasMore = data.size >= size
                    )
                } else {
                    PagedSource(
                        data = localData ?: emptyList(),
                        currentPage = page,
                        totalPages = page,
                        hasMore = false
                    )
                }
            } catch (e: Exception) {
                PagedSource(
                    data = localData ?: emptyList(),
                    currentPage = page,
                    totalPages = page,
                    hasMore = false
                )
            }
        }
    }
}
