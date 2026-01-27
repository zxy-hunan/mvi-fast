# P2 级别问题修复报告

修复日期：2025-01-27
修复内容：所有P2级别的优化问题

---

## 📋 修复清单

### ✅ 1. 添加统一的日志管理工具

**问题描述**：
- 框架中没有统一的日志工具
- 开发者直接使用 `android.util.Log`
- Release 版本可能打印日志（性能问题）
- 无法控制日志级别和格式

**修复方案**：

#### 1.1 创建 MviLog 工具类

**文件**：`MviLog.kt`

**核心功能**：
- ✅ 统一的日志接口（v/d/i/w/e）
- ✅ 可配置的日志级别（VERBOSE/DEBUG/INFO/WARN/ERROR/NONE）
- ✅ Release 版本自动关闭
- ✅ 线程信息显示
- ✅ 堆栈跟踪
- ✅ 网络请求日志
- ✅ 异常日志
- ✅ 文件日志（可选）
- ✅ 扩展函数支持

**使用示例**：
```kotlin
// 1. 在 Application 中初始化
MviLog.init(
    level = MviLog.Level.DEBUG,
    enabled = BuildConfig.DEBUG
)

// 2. 使用扩展函数
viewModel.logD("Loading users...")
viewModel.logE("Failed to load users", exception)

// 3. 使用静态方法
MviLog.d("Tag", "Debug message")
MviLog.networkRequest(url, method, params)
MviLog.exception(exception, "Operation failed")

// 4. 可选：添加文件日志（Release版本也记录错误）
if (!BuildConfig.DEBUG) {
    val fileWriter = MviLog.FileLogWriter(filesDir.absolutePath + "/logs")
    MviLog.addLogWriter(fileWriter)
}
```

**配置选项**：
```kotlin
// 日志级别
MviLog.logLevel = MviLog.Level.DEBUG

// 是否启用日志
MviLog.isLogEnabled = true

// 是否显示线程信息
MviLog.showThreadInfo = true

// 是否显示堆栈跟踪
MviLog.showStackTrace = false
```

**文件日志功能**：
- 自动创建日志文件
- 文件大小超过限制自动压缩
- 自动清理旧日志文件
- 可配置最大文件数量

---

### ✅ 2. 添加分页支持功能

**问题描述**：
- 没有分页加载扩展函数
- 无法处理大数据列表
- 每次都要手动实现分页逻辑

**修复方案**：

#### 2.1 创建分页数据类

**文件**：`PagingState.kt`

**核心类**：
```kotlin
// 分页数据封装
data class PagedList<T>(
    val data: List<T>,
    val currentPage: Int,
    val totalPages: Int,
    val pageSize: Int,
    val total: Int,
    val hasMore: Boolean
)

// 分页状态封装
sealed class PagingState<out T> {
    data object Idle : PagingState<Nothing>()
    data object Loading : PagingState<Nothing>()
    data class LoadingMore<T>(val currentData: List<T>) : PagingState<T>()
    data class Success<T>(val data: PagedList<T>) : PagingState<T>()
    data class Error(val message: String, val currentData: List<Any>?) : PagingState<Nothing>()
    data object Empty : PagingState<Nothing>()
    data class NoMoreData<T>(val currentData: List<T>) : PagingState<T>()
}
```

#### 2.2 创建分页请求扩展函数

**文件**：`PagingExt.kt`

**核心方法**：
```kotlin
// 方式1：自动更新 StateFlow
fun <T, I: MviIntent> MviViewModel<I>.launchPagingRequest(
    stateFlow: MutableStateFlow<PagingState<T>>,
    currentPage: Int,
    pageSize: Int = 20,
    currentData: List<T> = emptyList(),
    request: suspend (page: Int, size: Int) -> ApiResponse<PagedList<T>>,
    onSuccess: ((PagedList<T>) -> Unit)?
)

// 方式2：带回调方式
fun <T, I: MviIntent> MviViewModel<I>.launchPagingRequest(
    currentPage: Int,
    pageSize: Int = 20,
    currentData: List<T> = emptyList(),
    showLoading: Boolean = true,
    onSuccess: (PagedList<T>) -> Unit,
    onError: (String, Throwable?) -> Unit,
    request: suspend (page: Int, size: Int) -> ApiResponse<PagedList<T>>
)

// 方式3：带重试机制
fun <T, I: MviIntent> MviViewModel<I>.launchPagingRequestWithRetry(...)
```

**使用示例**：
```kotlin
class UserViewModel : MviViewModel<UserIntent>() {
    private var currentPage = 1
    private val pageSize = 20
    private val allUsers = mutableListOf<User>()

    private val _pagingState = MutableStateFlow<PagingState<User>>(PagingState.Idle)
    val pagingState = _pagingState.asStateFlow()

    private fun loadUsers(refresh: Boolean = false) {
        if (refresh) {
            currentPage = 1
            allUsers.clear()
        }

        launchPagingRequest(
            stateFlow = _pagingState,
            currentPage = currentPage,
            pageSize = pageSize,
            currentData = allUsers.toList(),
            request = { page, size -> apiService.getUsers(page, size) }
        ) { pagedData ->
            // 成功回调：更新数据
            if (page == 1) allUsers.clear()
            allUsers.addAll(pagedData.data)
            currentPage++
        }
    }

    private fun loadMore() {
        if (!_pagingState.canLoadMore()) return

        launchPagingRequest(
            stateFlow = _pagingState,
            currentPage = currentPage,
            pageSize = pageSize,
            currentData = allUsers.toList(),
            request = { page, size -> apiService.getUsers(page, size) }
        ) { pagedData ->
            allUsers.addAll(pagedData.data)
            currentPage++
        }
    }
}
```

**Activity 中使用**：
```kotlin
override fun observeData() {
    viewModel.pagingState.collectOn(this) { state ->
        when (state) {
            is PagingState.Loading -> showLoading()
            is PagingState.LoadingMore -> showLoadingMore()
            is PagingState.Success -> {
                hideLoading()
                adapter.setData(viewModel.getCurrentData())
            }
            is PagingState.NoMoreData -> {
                hideLoading()
                showNoMoreData()
            }
            is PagingState.Error -> {
                if (state.currentData.isNullOrEmpty()) {
                    showError(state.message) { sendIntent(UserIntent.Retry) }
                } else {
                    showToast(state.message)
                }
            }
            is PagingState.Empty -> showEmpty()
            else -> {}
        }
    }
}
```

#### 2.3 完整示例

**文件**：`PagingExample.kt`

包含：
- Intent 定义
- API 响应格式
- ViewModel 实现
- Activity 使用示例
- Adapter 示例

---

### ✅ 3. 完善错误处理国际化支持

**问题描述**：
- 默认错误消息只有英文
- 多语言应用需要自己处理国际化
- 中文用户体验不好

**修复方案**：

#### 3.1 添加多语言字符串资源

**已添加语言**：
- 🇺🇸 English（已有）
- 🇨🇳 简体中文（已有，values-zh）
- 🇯🇵 日本語（新增，values-ja）
- 🇰🇷 한국어（新增，values-ko）

**文件位置**：
```
mvi-core/src/main/res/
├── values/strings.xml              # 英文
├── values-zh/strings.xml           # 中文
├── values-ja/strings.xml           # 日文
└── values-ko/strings.xml           # 韩文
```

**支持的消息类型**：
- 网络异常（连接失败、超时、Socket异常）
- HTTP状态码（401/403/404/500/502/503/504等）
- 数据解析异常
- IO异常
- 其他常见异常（空指针、越界、类型转换等）

#### 3.2 改进 ExceptionHandle

**新增功能**：
```kotlin
// 1. 设置自定义错误消息（根据错误码）
ExceptionHandle.setCustomMessages(
    mapOf(
        401 to "请先登录",
        403 to "无权限访问",
        500 to "服务器开小差了"
    )
)

// 2. 设置自定义错误消息（根据异常类型）
ExceptionHandle.setCustomClassMessages(
    mapOf(
        SocketTimeoutException::class.java to "网络超时",
        IllegalArgumentException::class.java to "参数错误"
    )
)

// 3. 清除自定义消息
ExceptionHandle.clearCustomMessages()
```

**消息优先级**：
1. 自定义异常类型消息（最高）
2. 自定义错误码消息
3. 国际化消息（根据系统语言）
4. 默认英文消息（最低）

**使用示例**：
```kotlin
// 在 Application 中初始化（自动国际化）
class MyApplication : MviApplication() {
    override fun onInit() {
        ExceptionHandle.init(this)
        // 框架会根据系统语言自动选择对应的错误消息
    }
}

// 在特定页面使用自定义消息
class LoginActivity : MviActivity<...>() {
    override fun initView() {
        // 登录页面的特殊错误消息
        ExceptionHandle.setCustomMessages(
            mapOf(
                401 to "用户名或密码错误",
                429 to "登录尝试次数过多，请稍后再试"
            )
        )
    }

    override fun onDestroy() {
        // 清除自定义消息
        ExceptionHandle.clearCustomMessages()
        super.onDestroy()
    }
}
```

#### 3.3 完整示例

**文件**：`ErrorMessageExample.kt`

包含：
- 根据错误码自定义消息
- 根据异常类型自定义消息
- 同时使用两种方式
- 清除自定义消息
- 动态更新消息
- 特定场景使用不同消息
- 多语言自定义消息

---

## 🎯 修复效果

### 代码质量提升

| 评估项 | 修复前 | 修复后 |
|--------|--------|--------|
| 日志管理 | 无统一工具 | 完整的 MviLog 工具 |
| 分页支持 | 无 | 完整的分页框架 |
| 国际化 | 仅英文 | 支持中英日韩 |
| 自定义错误 | 不支持 | 支持错误码和异常类型 |
| 开发体验 | 一般 | 显著提升 |

### 开发效率提升

| 功能 | 修复前 | 修复后 |
|------|--------|--------|
| 日志记录 | 手动使用 Log | 统一工具 + 自动管理 |
| 分页列表 | 每次手动实现 | 调用扩展函数即可 |
| 错误消息 | 英文或不友好 | 自动国际化 + 可自定义 |

---

## 📖 使用指南

### 1. 日志管理

```kotlin
// Application 中初始化
class MyApplication : MviApplication() {
    override fun onInit() {
        // Debug 模式启用日志
        MviLog.init(level = MviLog.Level.DEBUG)

        // Release 模式只记录错误
        if (!BuildConfig.DEBUG) {
            MviLog.init(level = MviLog.Level.ERROR)

            // 可选：添加文件日志
            val fileWriter = MviLog.FileLogWriter(
                logDir = filesDir.absolutePath + "/logs",
                maxFileSize = 5 * 1024 * 1024  // 5MB
            )
            MviLog.addLogWriter(fileWriter)
        }
    }
}

// 在代码中使用
class MyViewModel : MviViewModel<MyIntent>() {
    fun loadData() {
        logD("loadData: 开始加载数据")

        try {
            val data = apiService.getData()
            logI("loadData: 加载成功，数据量 = ${data.size}")
        } catch (e: Exception) {
            logE("loadData: 加载失败", e)
        }
    }
}
```

### 2. 分页功能

```kotlin
// 1. 定义返回分页数据的 API
interface ApiService {
    @GET("users")
    suspend fun getUsers(
        @Query("page") page: Int,
        @Query("size") size: Int
    ): PagedList<User>
}

// 2. ViewModel 中使用
class UserViewModel(
    private val apiService: ApiService
) : MviViewModel<UserIntent>() {

    private var currentPage = 1
    private val allUsers = mutableListOf<User>()

    private val _pagingState = MutableStateFlow<PagingState<User>>(PagingState.Idle)
    val pagingState = _pagingState.asStateFlow()

    override fun handleIntent(intent: UserIntent) {
        when (intent) {
            is UserIntent.LoadFirst -> {
                currentPage = 1
                allUsers.clear()
                loadPage()
            }
            is UserIntent.LoadMore -> loadPage()
        }
    }

    private fun loadPage() {
        launchPagingRequest(
            stateFlow = _pagingState,
            currentPage = currentPage,
            pageSize = 20,
            currentData = allUsers.toList(),
            request = { page, size -> apiService.getUsers(page, size) }
        ) { pagedData ->
            if (pagedData.currentPage == 1) {
                allUsers.clear()
            }
            allUsers.addAll(pagedData.data)
            currentPage++
        }
    }
}

// 3. Activity 中监听状态
override fun observeData() {
    viewModel.pagingState.collectOn(this) { state ->
        when (state) {
            is PagingState.Loading -> showLoading()
            is PagingState.LoadingMore -> showLoadingMore()
            is PagingState.Success -> {
                hideLoading()
                adapter.submitList(state.data.data)
            }
            is PagingState.NoMoreData -> {
                hideLoading()
                showNoMore()
            }
            is PagingState.Error -> {
                hideLoading()
                showError(state.message)
            }
            else -> {}
        }
    }
}
```

### 3. 错误消息国际化

```kotlin
// Application 中初始化
class MyApplication : MviApplication() {
    override fun onInit() {
        // 初始化国际化支持
        ExceptionHandle.init(this)
        // 框架会根据系统语言自动选择对应的错误消息
    }
}

// 特定页面使用自定义消息
class PaymentActivity : MviActivity<...>() {
    override fun initView() {
        // 支付页面的特殊错误消息
        ExceptionHandle.setCustomMessages(
            mapOf(
                402 to "支付失败，余额不足",
                403 to "支付权限被限制",
                503 to "支付服务维护中"
            )
        )
    }

    override fun onDestroy() {
        // 恢复默认消息
        ExceptionHandle.clearCustomMessages()
        super.onDestroy()
    }
}
```

---

## ✅ 验证清单

- [x] 创建 MviLog 日志工具
- [x] 支持6种日志级别
- [x] 支持线程信息和堆栈跟踪
- [x] 支持网络请求日志
- [x] 支持文件日志
- [x] MviApplication 集成日志
- [x] 创建 PagingState 和 PagedList
- [x] 创建分页请求扩展函数
- [x] 创建分页使用示例
- [x] 添加中文错误消息
- [x] 添加日文错误消息
- [x] 添加韩文错误消息
- [x] 支持自定义错误消息
- [x] 创建自定义消息示例

---

## 📄 文件清单

### 新增文件

1. **日志管理**
   - `mvi-core/src/main/java/com/mvi/core/util/MviLog.kt`

2. **分页支持**
   - `mvi-core/src/main/java/com/mvi/core/base/PagingState.kt`
   - `mvi-core/src/main/java/com/mvi/core/ext/PagingExt.kt`
   - `mvi-core/src/main/java/com/mvi/core/example/PagingExample.kt`

3. **国际化支持**
   - `mvi-core/src/main/res/values-ja/strings.xml`（日文）
   - `mvi-core/src/main/res/values-ko/strings.xml`（韩文）
   - `mvi-core/src/main/java/com/mvi/core/example/ErrorMessageExample.kt`

### 修改文件

1. **日志集成**
   - `mvi-core/src/main/java/com/mvi/core/base/MviApplication.kt`

2. **错误处理增强**
   - `mvi-core/src/main/java/com/mvi/core/network/ExceptionHandle.kt`
   - `mvi-core/src/main/java/com/mvi/core/network/ErrorStatus.kt`

---

## 🔄 后续建议

### 可选优化（P3级别）
1. 添加日志远程上报功能
2. 添加分页缓存机制
3. 支持更多语言的错误消息
4. 添加日志分析工具
5. 添加分页预加载功能

---

**所有 P2 级别问题已修复！框架现在功能更加完善，开发体验显著提升。** ✅
