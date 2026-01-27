# Repository 层实现文档

## 📋 目录
1. [概述](#概述)
2. [核心功能](#核心功能)
3. [数据源类型](#数据源类型)
4. [使用方法](#使用方法)
5. [最佳实践](#最佳实践)

---

## 概述

Repository 层是数据层的重要组成部分，负责协调本地缓存和网络数据源，提供统一的数据访问接口。

### 设计目标

1. **统一数据访问**：隐藏数据来源细节（本地/网络）
2. **灵活的数据源策略**：支持全局配置和方法级别控制
3. **自动缓存管理**：智能缓存数据，减少网络请求
4. **简化代码**：减少重复的数据获取逻辑
5. **优先级清晰**：全局配置 > 方法级别配置

---

## 核心功能

### 1. 数据源类型

```kotlin
enum class DataSource {
    // 仅从网络获取数据
    NETWORK_ONLY,

    // 仅从本地缓存获取数据
    LOCAL_ONLY,

    // 先从本地获取，如果没有则从网络获取（推荐）
    LOCAL_FIRST,

    // 先从网络获取，如果失败则从本地获取
    NETWORK_FIRST,

    // 同时从本地和网络获取，优先使用网络数据
    CACHE_THEN_NETWORK
}
```

### 2. 全局配置管理

```kotlin
object DataSourceConfig {
    // 设置全局数据源
    fun setGlobalDataSource(dataSource: DataSource)

    // 禁用全局配置（使用方法级别配置）
    fun disableGlobal()

    // 启用全局配置
    fun enableGlobal()

    // 获取有效的数据源（考虑全局配置）
    fun getEffectiveDataSource(methodDataSource: DataSource?): DataSource
}
```

### 3. Repository 基类

```kotlin
abstract class BaseRepository {
    // 获取数据（suspend）
    protected suspend fun <T> fetchData(
        methodDataSource: DataSource? = null,
        localQuery: suspend () -> T?,
        networkCall: suspend () -> ApiResponse<T>,
        saveCallResult: suspend (T) -> Unit = {},
        forceRefresh: Boolean = false
    ): T

    // 获取数据流（Flow）
    protected fun <T> fetchDataFlow(
        methodDataSource: DataSource? = null,
        localQuery: suspend () -> T?,
        networkCall: suspend () -> ApiResponse<T>,
        saveCallResult: suspend (T) -> Unit = {}
    ): Flow<T>
}
```

### 4. 扩展函数

```kotlin
// 获取单个数据
suspend fun <T> fetchSingle(
    dataSource: DataSource? = null,
    localQuery: suspend () -> T?,
    networkCall: suspend () -> ApiResponse<T>,
    saveCallResult: suspend (T) -> Unit = {}
): T

// 获取数据流
fun <T> fetchFlow(
    dataSource: DataSource? = null,
    localQuery: suspend () -> T?,
    networkCall: suspend () -> ApiResponse<T>,
    saveCallResult: suspend (T) -> Unit = {}
): Flow<T>

// 获取分页数据
suspend fun <T> fetchPaged(
    dataSource: DataSource? = null,
    page: Int,
    size: Int,
    localQuery: suspend (page: Int, size: Int) -> List<T>?,
    networkCall: suspend (page: Int, size: Int) -> ApiResponse<List<T>>,
    saveCallResult: suspend (List<T>) -> Unit = {}
): PagedSource<T>
```

---

## 数据源类型详解

### 1. NETWORK_ONLY（仅网络）

```kotlin
suspend fun refreshUser(id: String): User {
    return fetchData(
        methodDataSource = DataSource.NETWORK_ONLY,
        localQuery = { cache.getUser(id) },
        networkCall = { apiService.getUser(id) },
        saveCallResult = { cache.saveUser(it) }
    )
}
```

**特点**：
- ✅ 总是从网络获取最新数据
- ✅ 自动保存到本地缓存
- ❌ 每次都请求网络，消耗流量
- 💡 适用场景：刷新数据、实时数据

---

### 2. LOCAL_ONLY（仅本地）

```kotlin
suspend fun getCachedUserList(): List<User> {
    return fetchData(
        methodDataSource = DataSource.LOCAL_ONLY,
        localQuery = { cache.getUserList() },
        networkCall = { apiService.getUserList() }
    )
}
```

**特点**：
- ✅ 快速响应，无网络请求
- ✅ 节省流量
- ❌ 数据可能过时
- ❌ 如果本地没有数据会抛异常
- 💡 适用场景：离线模式、历史数据

---

### 3. LOCAL_FIRST（本地优先，推荐）

```kotlin
suspend fun getUser(id: String): User {
    return fetchData(
        methodDataSource = DataSource.LOCAL_FIRST,
        localQuery = { cache.getUser(id) },
        networkCall = { apiService.getUser(id) },
        saveCallResult = { cache.saveUser(it) }
    )
}
```

**特点**：
- ✅ 优先使用本地数据（快速）
- ✅ 本地没有数据时自动请求网络
- ✅ 网络数据自动缓存
- 💡 适用场景：大多数场景（推荐）

**执行流程**：
```
1. 尝试从本地获取
   ├─ 有数据 → 立即返回
   └─ 无数据 → 继续下一步
2. 从网络获取
   ├─ 成功 → 保存到本地，返回数据
   └─ 失败 → 抛出异常
```

---

### 4. NETWORK_FIRST（网络优先）

```kotlin
suspend fun getUserListNetworkFirst(): List<User> {
    return fetchData(
        methodDataSource = DataSource.NETWORK_FIRST,
        localQuery = { cache.getUserList() },
        networkCall = { apiService.getUserList() },
        saveCallResult = { cache.saveUserList(it) }
    )
}
```

**特点**：
- ✅ 优先获取最新数据
- ✅ 网络失败时使用本地数据（降级）
- ❌ 首次可能较慢
- 💡 适用场景：需要新鲜数据但可容忍降级

**执行流程**：
```
1. 尝试从网络获取
   ├─ 成功 → 保存到本地，返回数据
   └─ 失败 → 继续下一步
2. 从本地获取
   ├─ 有数据 → 返回本地数据
   └─ 无数据 → 抛出异常
```

---

### 5. CACHE_THEN_NETWORK（缓存+网络）

```kotlin
fun getUserFlow(): Flow<User> = fetchDataFlow(
    methodDataSource = DataSource.CACHE_THEN_NETWORK,
    localQuery = { cache.getUser(id) },
    networkCall = { apiService.getUser(id) },
    saveCallResult = { cache.saveUser(it) }
)
```

**特点**：
- ✅ 先快速返回本地数据
- ✅ 然后获取网络数据并更新
- ✅ 用户体验最好（即时显示）
- 💡 适用场景：列表详情页

**执行流程**：
```
1. 从本地获取（同步）
   ├─ 有数据 → emit(本地数据)，继续
   └─ 无数据 → 跳过
2. 从网络获取
   ├─ 成功 → save() → emit(网络数据)
   └─ 失败 → 如果本地也没数据，抛异常
```

---

## 使用方法

### 步骤1：定义 Cache 接口

```kotlin
interface UserCache {
    suspend fun getUser(id: String): User?
    suspend fun saveUser(user: User)
    suspend fun getUserList(): List<User>?
    suspend fun saveUserList(users: List<User>)
}
```

### 步骤2：实现 Cache

```kotlin
class UserCacheImpl : UserCache {
    override suspend fun getUser(id: String): User? {
        // 从 MMKV、Room 等获取
        return MmkvStorage.getUser(id)
    }

    override suspend fun saveUser(user: User) {
        // 保存到 MMKV、Room 等
        MmkvStorage.saveUser(user)
    }
    // ...
}
```

### 步骤3：创建 Repository

```kotlin
class UserRepository(
    private val apiService: UserApiService,
    private val cache: UserCache
) : BaseRepository() {

    // 使用默认数据源（全局配置）
    suspend fun getUser(id: String): User {
        return fetchData(
            localQuery = { cache.getUser(id) },
            networkCall = { apiService.getUser(id) },
            saveCallResult = { cache.saveUser(it) }
        )
    }

    // 方法级别指定数据源
    @DataSource(DataSource.NETWORK_ONLY)
    suspend fun refreshUser(id: String): User {
        return fetchData(
            localQuery = { cache.getUser(id) },
            networkCall = { apiService.getUser(id) },
            saveCallResult = { cache.saveUser(it) }
        )
    }
}
```

### 步骤4：在 Application 中配置全局数据源

```kotlin
class MyApplication : MviApplication() {
    override fun onInit() {
        // 初始化 MMKV
        MmkvStorage.init(filesDir.absolutePath)

        // 初始化网络
        RetrofitClient.init {
            baseUrl = "https://api.example.com/"
        }

        // 配置全局数据源
        DataSourceConfig.setGlobalDataSource(DataSource.LOCAL_FIRST)
    }
}
```

### 步骤5：在 ViewModel 中使用

```kotlin
class UserViewModel(
    private val userRepository: UserRepository
) : MviViewModel<UserIntent>() {

    private val _userState = MutableStateFlow<UiState<User>>(UiState.Idle)
    val userState = _userState.asStateFlow()

    override fun handleIntent(intent: UserIntent) {
        when (intent) {
            is UserIntent.LoadUser -> loadUser(intent.userId)
            is UserIntent.RefreshUser -> refreshUser(intent.userId)
        }
    }

    private fun loadUser(userId: String) {
        viewModelScope.launch {
            try {
                _userState.value = UiState.Loading()
                // 使用全局数据源配置
                val user = userRepository.getUser(userId)
                _userState.value = UiState.Success(user)
            } catch (e: Exception) {
                _userState.value = UiState.Error(e.message ?: "加载失败")
            }
        }
    }

    private fun refreshUser(userId: String) {
        viewModelScope.launch {
            try {
                _userState.value = UiState.Loading()
                // 强制从网络刷新（方法级别配置）
                val user = userRepository.refreshUser(userId)
                _userState.value = UiState.Success(user)
            } catch (e: Exception) {
                _userState.value = UiState.Error(e.message ?: "刷新失败")
            }
        }
    }
}
```

---

## 优先级规则

### 优先级层级

```
全局配置 > 方法级别配置 > 默认值
```

### 判断流程

```
1. 检查方法是否指定了数据源？
   ├─ 是 → 检查全局配置是否启用？
   │   ├─ 是 → 使用方法级别的数据源 ✅
   │   └─ 否 → 使用方法级别的数据源 ✅
   └─ 否 → 使用全局数据源
       ├─ 如果全局启用 → 使用全局数据源 ✅
       └─ 如果全局禁用 → 使用默认值（LOCAL_FIRST）✅
```

### 特殊情况

**强制刷新**：即使全局配置是 `LOCAL_ONLY`，如果方法内部设置 `forceRefresh = true`，也会强制使用 `NETWORK_ONLY`。

---

## 最佳实践

### 1. 全局配置建议

```kotlin
// 正常模式（推荐）
DataSourceConfig.setGlobalDataSource(DataSource.LOCAL_FIRST)

// 离线模式
DataSourceConfig.setGlobalDataSource(DataSource.LOCAL_ONLY)

// 调试模式
DataSourceConfig.setGlobalDataSource(DataSource.NETWORK_FIRST)

// 实时数据模式
DataSourceConfig.setGlobalDataSource(DataSource.NETWORK_ONLY)
```

### 2. 方法命名规范

```kotlin
// 不指定数据源（使用全局配置）
suspend fun getUser(id: String): User

// 明确指定数据源
suspend fun refreshUser(id: String): User  // NETWORK_ONLY
suspend fun getCachedUser(id: String): User  // LOCAL_ONLY
suspend fun getUserListNetworkFirst(): List<User>  // NETWORK_FIRST
```

### 3. 缓存策略

```kotlin
// 对于不常变化的数据（如用户信息）
@CacheStrategy(cacheTime = 30 * 60 * 1000) // 30分钟
suspend fun getUserProfile(id: String): User

// 对于频繁变化的数据（如消息列表）
@CacheStrategy(cacheTime = 1 * 60 * 1000) // 1分钟
suspend fun getMessages(): List<Message>

// 对于从不缓存的数据
@CacheStrategy(enableCache = false)
suspend fun getRealtimeData(): RealtimeData
```

### 4. 错误处理

```kotlin
suspend fun getUser(id: String): User {
    return try {
        fetchData(...)
    } catch (e: Exception) {
        // 特殊错误处理
        when (e) {
            is NetworkException -> {
                // 网络错误，尝试返回本地数据
                cache.getUser(id) ?: throw e
            }
            else -> throw e
        }
    }
}
```

### 5. 测试建议

```kotlin
// 测试时禁用全局配置，使用方法级别配置
@Before
fun setup() {
    DataSourceConfig.disableGlobal()
}

@Test
fun testGetUserFromLocal() {
    // 测试仅从本地获取
    val user = userRepository.getUser(
        dataSource = DataSource.LOCAL_ONLY,
        ...
    )
}
```

---

## 完整示例

参考：`RepositoryExample.kt`

包含：
- ✅ Cache 接口定义和实现
- ✅ API 服务接口
- ✅ Repository 完整实现
- ✅ ViewModel 使用示例
- ✅ 数据源管理器
- ✅ 各种场景示例

---

## 总结

Repository 层提供了灵活、强大的数据源管理能力：

1. **全局配置优先**：统一管理整个应用的数据源策略
2. **方法级别控制**：特殊需求的方法可以覆盖全局配置
3. **智能缓存**：自动管理本地缓存，减少网络请求
4. **代码简化**：减少重复的数据获取逻辑
5. **易于测试**：可以通过依赖注入 Mock Repository

推荐使用 `LOCAL_FIRST` 作为全局配置，提供最佳的用户体验！
