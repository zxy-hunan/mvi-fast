package com.mvi.core.example

import androidx.lifecycle.viewModelScope
import com.mvi.core.base.MviIntent
import com.mvi.core.base.MviViewModel
import com.mvi.core.base.UiState
import com.mvi.core.network.ApiResponse
import com.mvi.core.repository.BaseRepository
import com.mvi.core.repository.DataSource
import com.mvi.core.repository.DataSourceConfig
import com.mvi.core.repository.fetchPaged
import com.mvi.core.repository.fetchSingle
import com.mvi.core.storage.MmkvStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Repository 使用示例
 *
 * 展示完整的 Repository 层实现，包括：
 * 1. 全局数据源配置
 * 2. 方法级别数据源控制
 * 3. 缓存管理
 * 4. 分页数据加载
 */

// ==================== 1. 数据模型 ====================

// 注：User 数据类已在 PagingExample.kt 中定义，此处不再重复定义

// ==================== 2. Cache 接口和实现 ====================

/**
 * 用户缓存接口
 */
interface UserCache {
    /**
     * 获取用户
     */
    suspend fun getUser(id: String): User?

    /**
     * 保存用户
     */
    suspend fun saveUser(user: User)

    /**
     * 获取用户列表
     */
    suspend fun getUserList(): List<User>?

    /**
     * 保存用户列表
     */
    suspend fun saveUserList(users: List<User>)

    /**
     * 获取分页用户
     */
    suspend fun getPagedUsers(page: Int, size: Int): List<User>?

    /**
     * 清空缓存
     */
    suspend fun clear()
}

/**
 * 用户缓存实现（使用 MMKV）
 */
class UserCacheImpl : UserCache {

    companion object {
        private const val KEY_USER_PREFIX = "user_"
        private const val KEY_USER_LIST = "user_list"
        private const val KEY_USER_LIST_PAGE = "user_list_page_"
    }

    override suspend fun getUser(id: String): User? {
        val json = MmkvStorage.getString("$KEY_USER_PREFIX$id", "")
        return if (json.isNotEmpty()) {
            // 简化示例，实际应该使用 Gson 解析
            null // 实际项目中应该实现序列化/反序列化
        } else {
            null
        }
    }

    override suspend fun saveUser(user: User) {
        // 简化示例，实际应该序列化为 JSON
        MmkvStorage.putString("$KEY_USER_PREFIX${user.id}", "user_json")
    }

    override suspend fun getUserList(): List<User>? {
        val json = MmkvStorage.getString(KEY_USER_LIST, "")
        return if (json.isNotEmpty()) {
            emptyList() // 实际应该反序列化
        } else {
            null
        }
    }

    override suspend fun saveUserList(users: List<User>) {
        // 简化示例
        MmkvStorage.putString(KEY_USER_LIST, "users_json")
    }

    override suspend fun getPagedUsers(page: Int, size: Int): List<User>? {
        val json = MmkvStorage.getString("$KEY_USER_LIST_PAGE$page", "")
        return if (json.isNotEmpty()) {
            emptyList() // 实际应该反序列化
        } else {
            null
        }
    }

    override suspend fun clear() {
        // 清空所有用户缓存
        MmkvStorage.remove(KEY_USER_LIST)
    }
}

// ==================== 3. API 服务接口 ====================

/**
 * 用户 API 服务
 */
interface UserApiService {
    /**
     * 获取用户详情
     */
    suspend fun getUser(id: String): ApiResponse<User>

    /**
     * 获取用户列表
     */
    suspend fun getUserList(): ApiResponse<List<User>>

    /**
     * 获取分页用户列表
     */
    suspend fun getPagedUsers(page: Int, size: Int): ApiResponse<List<User>>

    /**
     * 刷新用户列表
     */
    suspend fun refreshUsers(): ApiResponse<List<User>>
}

// ==================== 4. Repository 实现 ====================

/**
 * 用户 Repository
 *
 * 展示如何使用数据源配置
 */
class UserRepository(
    private val apiService: UserApiService,
    private val cache: UserCache
) : BaseRepository() {

    // ==================== 示例1：使用默认数据源（本地优先） ====================

    /**
     * 获取用户详情
     *
     * 数据源策略：LOCAL_FIRST（默认）
     * - 先从本地缓存获取
     * - 如果没有，则从网络获取并缓存
     */
    suspend fun getUser(id: String): User {
        return fetchData(
            // 不指定数据源，使用全局配置
            localQuery = { cache.getUser(id) },
            networkCall = { apiService.getUser(id) },
            saveCallResult = { cache.saveUser(it) }
        )
    }

    // ==================== 示例2：强制从网络获取 ====================

    /**
     * 刷新用户详情
     *
     * 数据源策略：NETWORK_ONLY
     * 即使全局配置是本地优先，此方法也会强制从网络获取
     */
    suspend fun refreshUser(id: String): User {
        return fetchData(
            // 方法级别强制使用网络
            methodDataSource = DataSource.NETWORK_ONLY,
            localQuery = { cache.getUser(id) },
            networkCall = { apiService.getUser(id) },
            saveCallResult = { cache.saveUser(it) }
        )
    }

    // ==================== 示例3：仅从本地获取 ====================

    /**
     * 获取用户列表（仅本地）
     *
     * 数据源策略：LOCAL_ONLY
     * 只从本地缓存获取，即使全局配置是网络优先
     */
    suspend fun getUserListLocalOnly(): List<User> {
        return fetchData(
            methodDataSource = DataSource.LOCAL_ONLY,
            localQuery = { cache.getUserList() },
            networkCall = { apiService.getUserList() }
        )
    }

    // ==================== 示例4：网络优先 ====================

    /**
     * 获取用户列表（网络优先）
     *
     * 数据源策略：NETWORK_FIRST
     * - 先尝试从网络获取
     * - 如果失败，则从本地缓存获取
     */
    suspend fun getUserListNetworkFirst(): List<User> {
        return fetchData(
            methodDataSource = DataSource.NETWORK_FIRST,
            localQuery = { cache.getUserList() },
            networkCall = { apiService.getUserList() },
            saveCallResult = { cache.saveUserList(it) }
        )
    }

    // ==================== 示例5：使用扩展函数（推荐） ====================

    /**
     * 获取用户列表（使用扩展函数）
     *
     * 数据源策略：使用全局配置
     */
    suspend fun getUserListSimple(): List<User> {
        return fetchSingle(
            // 使用全局配置
            localQuery = { cache.getUserList() },
            networkCall = { apiService.getUserList() },
            saveCallResult = { cache.saveUserList(it) }
        )
    }

    // ==================== 示例6：分页数据加载 ====================

    /**
     * 获取分页用户列表
     *
     * 数据源策略：LOCAL_FIRST
     */
    suspend fun getPagedUsers(page: Int, size: Int): com.mvi.core.repository.PagedSource<User> {
        return fetchPaged(
            dataSource = DataSource.LOCAL_FIRST,
            page = page,
            size = size,
            localQuery = { p, s -> cache.getPagedUsers(p, s) },
            networkCall = { p, s -> apiService.getPagedUsers(p, s) },
            saveCallResult = { users ->
                // 保存到缓存
                MmkvStorage.putString("user_list_page_$page", "users_json")
            }
        )
    }

    // ==================== 示例7：清空缓存 ====================

    /**
     * 清空所有缓存
     */
    suspend fun clearCache() {
        cache.clear()
    }
}

// ==================== 5. ViewModel 使用示例 ====================

/**
 * 用户 ViewModel
 *
 * 展示如何在 ViewModel 中使用 Repository
 */
class UserViewModelExample(
    private val userRepository: UserRepository
) : MviViewModel<UserIntentExample>() {

    private val _userState = MutableStateFlow<UiState<User>>(UiState.Idle)
    val userState = _userState.asStateFlow()

    private val _userListState = MutableStateFlow<UiState<List<User>>>(UiState.Idle)
    val userListState = _userListState.asStateFlow()

    override fun handleIntent(intent: UserIntentExample) {
        when (intent) {
            is UserIntentExample.LoadUser -> loadUser(intent.userId)
            is UserIntentExample.RefreshUser -> refreshUser(intent.userId)
            is UserIntentExample.LoadUserList -> loadUserList()
            is UserIntentExample.LoadLocalUserList -> loadLocalUserList()
            is UserIntentExample.ClearCache -> clearCache()
        }
    }

    /**
     * 加载用户（使用全局数据源配置）
     *
     * 数据源由全局配置决定，无需在方法中指定
     */
    private fun loadUser(userId: String) {
        // 假设全局配置是 LOCAL_FIRST
        // 此方法会先从本地获取，如果没有则从网络获取
        viewModelScope.launch {
            try {
                _userState.value = UiState.Loading()
                val user = userRepository.getUser(userId)
                _userState.value = UiState.Success(user)
            } catch (e: Exception) {
                _userState.value = UiState.Error(e.message ?: "加载失败")
            }
        }
    }

    /**
     * 刷新用户（强制从网络）
     *
     * 即使全局配置是 LOCAL_ONLY，此方法也会从网络获取
     */
    private fun refreshUser(userId: String) {
        viewModelScope.launch {
            try {
                _userState.value = UiState.Loading()
                val user = userRepository.refreshUser(userId)
                _userState.value = UiState.Success(user)
            } catch (e: Exception) {
                _userState.value = UiState.Error(e.message ?: "刷新失败")
            }
        }
    }

    /**
     * 加载用户列表（使用全局配置）
     */
    private fun loadUserList() {
        viewModelScope.launch {
            try {
                _userListState.value = UiState.Loading()
                val users = userRepository.getUserListSimple()
                _userListState.value = UiState.Success(users)
            } catch (e: Exception) {
                _userListState.value = UiState.Error(e.message ?: "加载失败")
            }
        }
    }

    /**
     * 仅加载本地数据
     *
     * 方法级别指定 LOCAL_ONLY
     * 即使全局配置是 NETWORK_ONLY，此方法也只会从本地获取
     */
    private fun loadLocalUserList() {
        viewModelScope.launch {
            try {
                _userListState.value = UiState.Loading()
                val users = userRepository.getUserListLocalOnly()
                _userListState.value = UiState.Success(users)
            } catch (e: Exception) {
                _userListState.value = UiState.Error(e.message ?: "本地数据为空")
            }
        }
    }

    /**
     * 清空缓存
     */
    private fun clearCache() {
        viewModelScope.launch {
            try {
                userRepository.clearCache()
                showToast("缓存已清空")
            } catch (e: Exception) {
                showToast("清空失败")
            }
        }
    }
}

// ==================== 6. Intent 定义 ====================

/**
 * 用户 Intent
 */
sealed class UserIntentExample : MviIntent {
    data class LoadUser(val userId: String) : UserIntentExample()
    data class RefreshUser(val userId: String) : UserIntentExample()
    data object LoadUserList : UserIntentExample()
    data object LoadLocalUserList : UserIntentExample()
    data object ClearCache : UserIntentExample()
}

// ==================== 7. Application 中配置全局数据源 ====================

/**
 * Application 配置示例
 *
 * 展示如何配置全局数据源
 */
class RepositoryApplicationExample /* : MviApplication() */ {

    /*
    override fun onInit() {
        // 初始化 MMKV
        MmkvStorage.init(filesDir.absolutePath)

        // ==================== 场景1：离线模式 ====================
        // 全局配置为仅使用本地数据
        // 适用于：离线模式、节省流量、演示模式
        DataSourceConfig.setGlobalDataSource(DataSource.LOCAL_ONLY)

        // ==================== 场景2：在线模式（推荐） ====================
        // 全局配置为本地优先（默认）
        // 适用于：正常使用，提供最佳用户体验
        DataSourceConfig.setGlobalDataSource(DataSource.LOCAL_FIRST)

        // ==================== 场景3：调试模式 ====================
        // 全局配置为网络优先
        // 适用于：开发调试、实时数据
        DataSourceConfig.setGlobalDataSource(DataSource.NETWORK_FIRST)

        // ==================== 场景4：实时数据模式 ====================
        // 全局配置为仅使用网络
        // 适用于：需要最新数据的场景
        DataSourceConfig.setGlobalDataSource(DataSource.NETWORK_ONLY)
    }
    */
}

// ==================== 8. 动态切换数据源示例 ====================

/**
 * 数据源管理器
 *
 * 用于在不同场景下动态切换数据源
 */
object DataSourceManager {

    /**
     * 切换到离线模式
     */
    fun switchToOfflineMode() {
        DataSourceConfig.setGlobalDataSource(DataSource.LOCAL_ONLY)
    }

    /**
     * 切换到在线模式
     */
    fun switchToOnlineMode() {
        DataSourceConfig.setGlobalDataSource(DataSource.LOCAL_FIRST)
    }

    /**
     * 切换到调试模式
     */
    fun switchToDebugMode() {
        DataSourceConfig.setGlobalDataSource(DataSource.NETWORK_FIRST)
    }

    /**
     * 切换到实时数据模式
     */
    fun switchToRealtimeMode() {
        DataSourceConfig.setGlobalDataSource(DataSource.NETWORK_ONLY)
    }

    /**
     * 禁用全局配置
     *
     * 禁用后，方法级别的数据源配置将生效
     */
    fun disableGlobalConfig() {
        DataSourceConfig.disableGlobal()
    }

    /**
     * 启用全局配置
     */
    fun enableGlobalConfig() {
        DataSourceConfig.enableGlobal()
    }
}

// ==================== 使用示例总结 ====================

/**
 * 优先级规则：
 *
 * 1. 全局配置 > 方法级别配置
 * 2. NETWORK_ONLY 优先级最高，即使方法指定 LOCAL_ONLY
 *
 * 场景示例：
 *
 * 场景1：全局 LOCAL_ONLY，方法无指定
 * → 结果：LOCAL_ONLY
 *
 * 场景2：全局 NETWORK_ONLY，方法指定 LOCAL_ONLY
 * → 结果：NETWORK_ONLY（全局优先）
 *
 * 场景3：全局 LOCAL_FIRST，方法指定 NETWORK_ONLY
 * → 结果：NETWORK_ONLY（方法优先）
 *
 * 场景4：全局禁用，方法指定 LOCAL_ONLY
 * → 结果：LOCAL_ONLY（方法优先）
 *
 * 推荐实践：
 * 1. 应用启动时设置全局配置为 LOCAL_FIRST
 * 2. 特殊需求的方法使用 @DataSource 注解指定
 * 3. 需要"实时"数据的方法使用 NETWORK_ONLY
 * 4. 离线功能的方法使用 LOCAL_ONLY
 * 5. 大部分方法不指定，使用全局配置
 */
