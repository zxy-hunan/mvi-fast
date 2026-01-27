package com.mvi.core.repository

import com.mvi.core.network.ApiResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Repository 基类
 *
 * 提供数据源选择、缓存管理等通用功能
 *
 * 功能特性:
 * 1. 全局数据源配置
 * 2. 方法级别数据源控制
 * 3. 自动缓存管理
 * 4. 统一错误处理
 * 5. 支持强制刷新
 *
 * 使用示例:
 * ```kotlin
 * class UserRepository(
 *     private val apiService: UserApiService,
 *     private val cache: UserCache
 * ) : BaseRepository() {
 *
 *     suspend fun getUser(id: String): User {
 *         return fetchData(
 *             methodDataSource = DataSource.LOCAL_FIRST,
 *             localQuery = { cache.getUser(id) },
 *             networkCall = { apiService.getUser(id) },
 *             saveCallResult = { cache.saveUser(it) }
 *         )
 *     }
 * }
 * ```
 */
abstract class BaseRepository {

    /**
     * 获取数据
     *
     * 根据数据源策略自动选择从本地还是网络获取数据
     *
     * @param methodDataSource 方法级别数据源（可选）
     * @param localQuery 本地查询
     * @param networkCall 网络请求
     * @param saveCallResult 保存网络结果到本地
     * @param forceRefresh 是否强制刷新（忽略本地数据）
     * @return 数据
     */
    protected suspend fun <T> fetchData(
        methodDataSource: DataSource? = null,
        localQuery: suspend () -> T?,
        networkCall: suspend () -> ApiResponse<T>,
        saveCallResult: suspend (T) -> Unit = {},
        forceRefresh: Boolean = false
    ): T {
        // 获取有效的数据源（全局 > 方法级别）
        val effectiveDataSource = if (forceRefresh) {
            DataSource.NETWORK_ONLY
        } else {
            DataSourceConfig.getEffectiveDataSource(methodDataSource)
        }

        return when (effectiveDataSource) {
            DataSource.LOCAL_ONLY -> {
                // 仅从本地获取
                localQuery.invoke() ?: throw Exception("Local data not found")
            }

            DataSource.NETWORK_ONLY -> {
                // 仅从网络获取
                fetchFromNetwork(networkCall, saveCallResult)
            }

            DataSource.LOCAL_FIRST -> {
                // 先本地，后网络
                val localData = localQuery.invoke()
                if (localData != null) {
                    localData
                } else {
                    fetchFromNetwork(networkCall, saveCallResult)
                }
            }

            DataSource.NETWORK_FIRST -> {
                // 先网络，失败则本地
                try {
                    fetchFromNetwork(networkCall, saveCallResult)
                } catch (e: Exception) {
                    val localData = localQuery.invoke()
                    localData ?: throw e
                }
            }

            DataSource.CACHE_THEN_NETWORK -> {
                // 同时获取本地和网络数据，优先使用网络
                val localData = localQuery.invoke()
                try {
                    val networkData = fetchFromNetwork(networkCall, saveCallResult)
                    networkData
                } catch (e: Exception) {
                    localData ?: throw e
                }
            }
        }
    }

    /**
     * 从网络获取数据并保存到本地
     */
    private suspend fun <T> fetchFromNetwork(
        networkCall: suspend () -> ApiResponse<T>,
        saveCallResult: suspend (T) -> Unit
    ): T {
        return withContext(Dispatchers.IO) {
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

    /**
     * 获取数据流（响应式）
     *
     * @param methodDataSource 方法级别数据源
     * @param localQuery 本地查询
     * @param networkCall 网络请求
     * @param saveCallResult 保存网络结果
     * @return Flow<T>
     */
    protected fun <T> fetchDataFlow(
        methodDataSource: DataSource? = null,
        localQuery: suspend () -> T?,
        networkCall: suspend () -> ApiResponse<T>,
        saveCallResult: suspend (T) -> Unit = {}
    ): Flow<T> = flow {
        val effectiveDataSource = DataSourceConfig.getEffectiveDataSource(methodDataSource)

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
                val data = fetchFromNetwork(networkCall, saveCallResult)
                emit(data)
            }

            DataSource.LOCAL_FIRST -> {
                // 先发射本地数据
                val localData = localQuery.invoke()
                if (localData != null) {
                    emit(localData)
                }
                // 然后从网络获取
                val networkData = fetchFromNetwork(networkCall, saveCallResult)
                emit(networkData)
            }

            DataSource.NETWORK_FIRST -> {
                try {
                    val data = fetchFromNetwork(networkCall, saveCallResult)
                    emit(data)
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
                val networkData = fetchFromNetwork(networkCall, saveCallResult)
                emit(networkData)
            }
        }
    }.flowOn(Dispatchers.IO).catch { e ->
        throw e
    }
}
