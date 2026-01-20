# 架构说明 - MVI 设计

## 📐 架构概览

本项目采用 **MVI (Model-View-Intent)** 架构模式，实现单向数据流，确保状态可预测、易于测试和调试。

## 🔄 数据流

```
┌─────────────┐
│    View     │  用户交互（点击、输入等）
│  (Activity) │
└──────┬──────┘
       │ sendIntent()
       ▼
┌─────────────┐
│   Intent    │  用户意图（LoadUsers, DeleteUser 等）
│  (Sealed)   │
└──────┬──────┘
       │ handleIntent()
       ▼
┌─────────────┐
│  ViewModel  │  处理业务逻辑，更新状态
│  (Model)    │
└──────┬──────┘
       │ updateState()
       ▼
┌─────────────┐
│    State    │  UI 状态（Loading, Success, Error 等）
│ (StateFlow) │
└──────┬──────┘
       │ collectState()
       ▼
┌─────────────┐
│    View     │  根据状态渲染 UI
│  (Activity) │
└─────────────┘
```

## 🎯 核心组件

### 1. Intent（用户意图）

#### 定义规范
```kotlin
/**
 * Intent 必须实现 MviIntent 接口
 * 使用 sealed class 定义所有可能的用户操作
 */
sealed class UserIntent : MviIntent {
    // 简单操作使用 object
    data object LoadUsers : UserIntent()
    data object RefreshUsers : UserIntent()
    
    // 带参数的操作使用 data class
    data class DeleteUser(val userId: String) : UserIntent()
    data class SearchUser(val keyword: String) : UserIntent()
    data class UpdateUser(val user: User) : UserIntent()
}
```

#### Intent 命名规范
- ✅ 使用**动词开头**：`LoadUsers`、`DeleteUser`、`SearchUser`
- ✅ 简单操作使用 `object`：`data object LoadUsers`
- ✅ 带参数使用 `data class`：`data class DeleteUser(val userId: String)`
- ❌ 避免使用 `Action` 后缀：`LoadUsersAction`（冗余）

#### Intent 边界
- **Intent 只表示用户意图**，不包含业务逻辑
- **Intent 不可变**，使用 `data object` 或 `data class`
- **Intent 不包含状态信息**，状态由 StateFlow 管理

### 2. State（UI 状态）

#### UiState 定义
```kotlin
/**
 * UI 状态封装
 * 使用 sealed class 表示有限的状态集合
 */
sealed class UiState<out T> {
    /** 初始状态 */
    data object Idle : UiState<Nothing>()
    
    /** 加载中 */
    data class Loading(val message: String = "Loading...") : UiState<Nothing>()
    
    /** 成功（携带数据） */
    data class Success<T>(val data: T) : UiState<T>()
    
    /** 错误 */
    data class Error(
        val message: String,
        val throwable: Throwable? = null,
        val canRetry: Boolean = true
    ) : UiState<Nothing>()
    
    /** 空数据 */
    data class Empty(val message: String = "No data") : UiState<Nothing>()
    
    /** 网络错误 */
    data class NetworkError(val message: String = "Network error") : UiState<Nothing>()
}
```

#### State 管理规范
```kotlin
class UserViewModel : MviViewModel<UserIntent>() {
    // ✅ 正确：私有可变，公开只读
    private val _userState = MutableStateFlow<UiState<List<User>>>(UiState.Idle)
    val userState: StateFlow<UiState<List<User>>> = _userState.asStateFlow()
    
    // ✅ 正确：在 ViewModel 中更新状态
    private fun loadUsers() {
        _userState.value = UiState.Loading()
        // ... 网络请求
        _userState.value = UiState.Success(users)
    }
}
```

#### State 边界
- **State 是只读的**，View 层只能观察，不能修改
- **State 是单向的**，只能从 ViewModel 流向 View
- **State 是不可变的**，使用 sealed class 确保类型安全

### 3. Effect（一次性事件）

#### UiEvent 定义
```kotlin
/**
 * UI 事件（一次性事件，如 Toast、导航等）
 * 使用 Channel 避免配置更改时重复触发
 */
sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class ShowLoading(val show: Boolean) : UiEvent()
    data class Navigate(val route: String) : UiEvent()
}
```

#### Event 使用规范
```kotlin
class UserViewModel : MviViewModel<UserIntent>() {
    private fun deleteUser(userId: String) {
        launchRequest(_userState, showLoading = false) {
            apiService.deleteUser(userId)
        }.invokeOnCompletion {
            // ✅ 正确：使用 sendEvent 发送一次性事件
            sendEvent(UiEvent.ShowToast("删除成功"))
            sendIntent(UserIntent.LoadUsers) // 刷新列表
        }
    }
}
```

#### Event 边界
- **Event 用于一次性操作**：Toast、导航、对话框等
- **Event 不保存状态**，只触发一次
- **Event 使用 Channel**，避免配置更改时重复触发

## 🏗️ 架构层次

### 1. View 层（Activity/Fragment）

#### 职责
- 接收用户输入，发送 Intent
- 观察 State，更新 UI
- 处理生命周期

#### 实现示例
```kotlin
class UserListActivity : MviActivity<ActivityUserListBinding, UserViewModel, UserIntent>() {
    
    override fun initView() {
        // ✅ 正确：用户操作转换为 Intent
        binding.btnLoad.setOnClickListener {
            sendIntent(UserIntent.LoadUsers)
        }
    }
    
    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userState.collect { state ->
                    // ✅ 正确：根据状态更新 UI
                    when (state) {
                        is UiState.Loading -> showLoading()
                        is UiState.Success -> showData(state.data)
                        is UiState.Error -> showError(state.message)
                        else -> {}
                    }
                }
            }
        }
    }
}
```

### 2. ViewModel 层

#### 职责
- 处理 Intent，执行业务逻辑
- 管理 State，更新 StateFlow
- 发送 Event，触发一次性操作

#### 实现示例
```kotlin
class UserViewModel : MviViewModel<UserIntent>() {
    
    private val _userState = MutableStateFlow<UiState<List<User>>>(UiState.Idle)
    val userState: StateFlow<UiState<List<User>>> = _userState.asStateFlow()
    
    override fun handleIntent(intent: UserIntent) {
        when (intent) {
            is UserIntent.LoadUsers -> loadUsers()
            is UserIntent.DeleteUser -> deleteUser(intent.userId)
        }
    }
    
    private fun loadUsers() {
        // ✅ 正确：使用 launchRequest 自动处理状态
        launchRequest(_userState) {
            apiService.getUserList()
        }
    }
}
```

### 3. Model 层（Repository/API）

#### 职责
- 数据获取（网络、本地存储）
- 数据转换
- 错误处理

#### 实现示例
```kotlin
interface UserApi {
    @GET("/api/users")
    suspend fun getUserList(): ApiResponse<List<User>>
}

// ✅ 正确：API 返回统一的 ApiResponse
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
) {
    fun isSuccess(): Boolean = code == 200 || code == 0
}
```

## 🔀 数据流边界

### Intent 流边界
```
View → Intent → ViewModel
```
- **单向流动**：View 只能发送 Intent，不能接收
- **不可逆**：Intent 一旦发送，不能撤销
- **异步处理**：Intent 在 ViewModel 中异步处理

### State 流边界
```
ViewModel → State → View
```
- **单向流动**：State 只能从 ViewModel 流向 View
- **只读观察**：View 只能观察 State，不能修改
- **生命周期感知**：使用 `repeatOnLifecycle` 确保生命周期安全

### Event 流边界
```
ViewModel → Event → View
```
- **一次性消费**：Event 使用 Channel，消费后即销毁
- **不保存状态**：Event 不参与状态管理
- **自动处理**：在 `MviActivity` 中自动处理常见 Event

## 📊 状态转换图

```
Idle
  │
  ├─→ Loading ──→ Success
  │                │
  │                └─→ Empty (data == null)
  │
  └─→ Error ──→ (retry) ──→ Loading
       │
       └─→ NetworkError
```

## 🎨 最佳实践

### 1. Intent 设计
```kotlin
// ✅ 正确：Intent 粒度适中
sealed class UserIntent : MviIntent {
    data object LoadUsers : UserIntent()           // 加载列表
    data object RefreshUsers : UserIntent()        // 刷新列表
    data class DeleteUser(val userId: String) : UserIntent()  // 删除用户
}

// ❌ 错误：Intent 过于细化
sealed class UserIntent : MviIntent {
    data object ClickLoadButton : UserIntent()     // ❌ 过于细化
    data object StartLoading : UserIntent()        // ❌ 这是状态，不是意图
}
```

### 2. State 设计
```kotlin
// ✅ 正确：每个页面维护独立的状态
class UserViewModel : MviViewModel<UserIntent>() {
    private val _userState = MutableStateFlow<UiState<List<User>>>(UiState.Idle)
    val userState: StateFlow<UiState<List<User>>> = _userState.asStateFlow()
}

// ❌ 错误：不要在 ViewModel 中混合多个状态
class UserViewModel : MviViewModel<UserIntent>() {
    private val _loading = MutableStateFlow(false)  // ❌ 应该使用 UiState.Loading
    private val _users = MutableStateFlow<List<User>>(emptyList())  // ❌ 应该使用 UiState.Success
}
```

### 3. Event 设计
```kotlin
// ✅ 正确：Event 用于一次性操作
sendEvent(UiEvent.ShowToast("删除成功"))
sendEvent(UiEvent.Navigate("/user/detail"))

// ❌ 错误：不要用 Event 传递状态
sendEvent(UiEvent.UpdateUserList(users))  // ❌ 应该使用 State
```

## 🔍 常见问题

### Q1: Intent 和 Event 的区别？
**A**: Intent 是用户意图，从 View 流向 ViewModel；Event 是一次性事件，从 ViewModel 流向 View。

### Q2: 什么时候使用 State，什么时候使用 Event？
**A**: 
- **State**：需要持久化的状态（列表数据、表单数据等）
- **Event**：一次性操作（Toast、导航、对话框等）

### Q3: 如何处理多个并发请求？
**A**: 每个请求维护独立的 StateFlow：
```kotlin
private val _userState = MutableStateFlow<UiState<List<User>>>(UiState.Idle)
private val _detailState = MutableStateFlow<UiState<User>>(UiState.Idle)
```

### Q4: 如何实现列表分页？
**A**: 在 State 中维护列表数据：
```kotlin
data class UserListState(
    val users: List<User> = emptyList(),
    val hasMore: Boolean = true,
    val isLoading: Boolean = false
)
```

---

**最后更新**: 2024-12-17  
**维护者**: aFramework Team
