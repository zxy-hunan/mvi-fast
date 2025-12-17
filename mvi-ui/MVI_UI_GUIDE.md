# MVI-UI 增强版基类使用指南

## 概述

`mvi-ui` 模块提供了增强版的 Activity 和 Fragment 基类，在 `mvi-core` 的基础上添加了 UI 管理器功能。

## 架构层次

```
┌─────────────────────────────────────────┐
│  mvi-ui (UI增强层)                      │
│  ├── MviUiViewModel                     │
│  ├── MviUiActivity                      │
│  ├── MviUiFragment                      │
│  ├── EmptyStateManager (缺省页)          │
│  ├── SkeletonManager (骨架屏)            │
│  └── MviDialog (对话框)                  │
└─────────────────────────────────────────┘
             ▼ extends
┌─────────────────────────────────────────┐
│  mvi-core (核心架构层)                   │
│  ├── MviViewModel                       │
│  ├── MviActivity                        │
│  ├── MviFragment                        │
│  ├── UiEvent (基础事件)                  │
│  └── UiState (状态封装)                  │
└─────────────────────────────────────────┘
```

## 新增组件

### 1. MviUiViewModel

继承自 `MviViewModel`，添加了 UI 扩展事件支持。

**特性:**
- 支持 `MviUiEvent` (空状态、错误状态事件)
- 提供便捷方法: `showEmptyState()`, `showErrorState()`, `hideEmptyState()`
- 完全兼容 `MviViewModel` 的所有功能

**使用示例:**

```kotlin
class UserViewModel : MviUiViewModel<UserIntent>() {

    private val _userState = MutableStateFlow<UiState<List<User>>>(UiState.Idle)
    val userState: StateFlow<UiState<List<User>>> = _userState.asStateFlow()

    override fun handleIntent(intent: UserIntent) {
        when (intent) {
            is UserIntent.LoadUsers -> loadUsers()
            is UserIntent.RefreshUsers -> refreshUsers()
        }
    }

    private fun loadUsers() {
        viewModelScope.launch {
            _userState.value = UiState.Loading()

            try {
                val users = userRepository.getUsers()
                if (users.isEmpty()) {
                    // 显示空状态
                    showEmptyState("暂无用户数据")
                } else {
                    hideEmptyState()
                    _userState.value = UiState.Success(users)
                }
            } catch (e: Exception) {
                // 显示错误状态
                showErrorState("加载失败: ${e.message}")
                _userState.value = UiState.Error(e.message ?: "未知错误")
            }
        }
    }
}
```

### 2. MviUiActivity

继承自 `MviActivity`，添加了缺省页和骨架屏管理。

**特性:**
- 自动初始化 `EmptyStateManager`
- 自动观察 `MviUiEvent` 并处理空状态/错误状态
- 提供便捷方法: `showEmpty()`, `showError()`, `showNetworkError()`, `hideEmptyState()`
- 支持创建 `SkeletonManager`

**使用示例:**

```kotlin
class UserActivity : MviUiActivity<ActivityUserBinding, UserViewModel, UserIntent>() {

    override fun createBinding() = ActivityUserBinding.inflate(layoutInflater)

    override fun getViewModelClass() = UserViewModel::class.java

    override fun initView() {
        // 初始化 RecyclerView
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@UserActivity)
            adapter = userAdapter
        }

        // 加载数据
        sendIntent(UserIntent.LoadUsers)
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 观察用户列表状态
                viewModel.userState.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            // 显示加载中
                        }
                        is UiState.Success -> {
                            // 显示数据
                            userAdapter.submitList(state.data)
                        }
                        is UiState.Error -> {
                            // ViewModel 会自动通过 MviUiEvent 显示错误
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    // 可选: 自定义缺省页配置
    override fun getEmptyStateConfig(): EmptyStateConfig {
        return EmptyStateConfig(
            emptyMessage = "还没有用户哦~",
            errorMessage = "加载失败了",
            emptyIconResId = R.drawable.ic_empty,
            errorIconResId = R.drawable.ic_error
        )
    }
}
```

### 3. MviUiFragment

继承自 `MviFragment`，功能与 `MviUiActivity` 相同。

**使用示例:**

```kotlin
class UserListFragment : MviUiFragment<FragmentUserListBinding, UserViewModel, UserIntent>() {

    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentUserListBinding.inflate(inflater, container, false)

    override fun getViewModelClass() = UserViewModel::class.java

    override fun initView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userAdapter
        }

        // 下拉刷新
        binding.swipeRefresh.setOnRefreshListener {
            sendIntent(UserIntent.RefreshUsers)
        }

        // 加载数据
        sendIntent(UserIntent.LoadUsers)
    }

    override fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userState.collect { state ->
                    binding.swipeRefresh.isRefreshing = state is UiState.Loading

                    when (state) {
                        is UiState.Success -> {
                            userAdapter.submitList(state.data)
                        }
                        is UiState.Empty -> {
                            // ViewModel 会自动显示空状态
                        }
                        is UiState.Error -> {
                            // ViewModel 会自动显示错误状态
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
```

### 4. MviUiEvent

独立的 UI 事件类型，用于处理空状态和错误状态。

**事件类型:**

```kotlin
sealed interface MviUiEvent {
    // 显示空状态
    data class ShowEmptyState(
        val message: String = "",
        val iconResId: Int = 0,
        val onRetry: (() -> Unit)? = null
    ) : MviUiEvent

    // 显示错误状态
    data class ShowErrorState(
        val message: String = "",
        val iconResId: Int = 0,
        val onRetry: (() -> Unit)? = null
    ) : MviUiEvent

    // 隐藏空状态/错误状态
    data object HideEmptyState : MviUiEvent
}
```

**在 ViewModel 中使用:**

```kotlin
class ProductViewModel : MviUiViewModel<ProductIntent>() {

    private fun loadProducts() {
        viewModelScope.launch {
            try {
                val products = productRepository.getProducts()

                if (products.isEmpty()) {
                    // 方式 1: 使用便捷方法
                    showEmptyState("暂无商品")

                    // 方式 2: 直接发送事件
                    sendUiEvent(MviUiEvent.ShowEmptyState(
                        message = "暂无商品",
                        iconResId = R.drawable.ic_empty,
                        onRetry = { handleIntent(ProductIntent.LoadProducts) }
                    ))
                } else {
                    hideEmptyState()
                    _productState.value = UiState.Success(products)
                }
            } catch (e: Exception) {
                // 带重试功能的错误提示
                showErrorState(
                    message = "加载失败: ${e.message}",
                    onRetry = { handleIntent(ProductIntent.LoadProducts) }
                )
            }
        }
    }
}
```

## 完整示例

### ViewModel

```kotlin
sealed class UserIntent : MviIntent {
    data object LoadUsers : UserIntent()
    data class DeleteUser(val userId: String) : UserIntent()
}

class UserViewModel : MviUiViewModel<UserIntent>() {

    private val _users = MutableStateFlow<UiState<List<User>>>(UiState.Idle)
    val users: StateFlow<UiState<List<User>>> = _users.asStateFlow()

    override fun handleIntent(intent: UserIntent) {
        when (intent) {
            is UserIntent.LoadUsers -> loadUsers()
            is UserIntent.DeleteUser -> deleteUser(intent.userId)
        }
    }

    private fun loadUsers() {
        viewModelScope.launch {
            _users.value = UiState.Loading()
            showLoading(true)

            try {
                delay(1000) // 模拟网络请求
                val result = userRepository.getUsers()

                showLoading(false)

                if (result.isEmpty()) {
                    showEmptyState(
                        message = "还没有用户数据",
                        onRetry = { handleIntent(UserIntent.LoadUsers) }
                    )
                } else {
                    hideEmptyState()
                    _users.value = UiState.Success(result)
                }
            } catch (e: Exception) {
                showLoading(false)
                showErrorState(
                    message = "加载失败: ${e.message}",
                    onRetry = { handleIntent(UserIntent.LoadUsers) }
                )
            }
        }
    }

    private fun deleteUser(userId: String) {
        viewModelScope.launch {
            try {
                userRepository.deleteUser(userId)
                showToast("删除成功")
                handleIntent(UserIntent.LoadUsers)
            } catch (e: Exception) {
                showToast("删除失败: ${e.message}")
            }
        }
    }
}
```

### Activity

```kotlin
class UserActivity : MviUiActivity<ActivityUserBinding, UserViewModel, UserIntent>() {

    private val userAdapter by lazy {
        UserAdapter(
            onItemClick = { user -> navigateToDetail(user) },
            onDeleteClick = { user -> sendIntent(UserIntent.DeleteUser(user.id)) }
        )
    }

    override fun createBinding() = ActivityUserBinding.inflate(layoutInflater)

    override fun getViewModelClass() = UserViewModel::class.java

    override fun initView() {
        setupToolbar()
        setupRecyclerView()
        setupRefresh()

        // 首次加载
        sendIntent(UserIntent.LoadUsers)
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@UserActivity)
            adapter = userAdapter
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    private fun setupRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            sendIntent(UserIntent.LoadUsers)
        }
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.users.collect { state ->
                    binding.swipeRefresh.isRefreshing = state is UiState.Loading

                    when (state) {
                        is UiState.Success -> {
                            userAdapter.submitList(state.data)
                        }
                        else -> {
                            // 空状态和错误状态由 MviUiActivity 自动处理
                        }
                    }
                }
            }
        }
    }

    override fun getEmptyStateConfig() = EmptyStateConfig(
        emptyMessage = "还没有用户哦~",
        errorMessage = "加载失败了,请重试",
        emptyIconResId = R.drawable.ic_empty_user,
        errorIconResId = R.drawable.ic_error
    )
}
```

## 对比

### 使用 MviActivity (核心版)

```kotlin
class SimpleActivity : MviActivity<ActivitySimpleBinding, SimpleViewModel, SimpleIntent>() {
    // ✅ 核心 MVI 功能
    // ❌ 没有缺省页管理
    // ❌ 需要手动处理空数据/错误状态
}
```

### 使用 MviUiActivity (增强版)

```kotlin
class EnhancedActivity : MviUiActivity<ActivityEnhancedBinding, EnhancedViewModel, EnhancedIntent>() {
    // ✅ 核心 MVI 功能
    // ✅ 自动缺省页管理
    // ✅ 自动处理空数据/错误状态
    // ✅ 支持骨架屏
}
```

## 最佳实践

1. **选择合适的基类**
   - 需要缺省页功能 → 使用 `MviUiActivity/Fragment`
   - 仅需核心功能 → 使用 `MviActivity/Fragment`

2. **ViewModel 中的事件发送**
   - 使用便捷方法: `showEmptyState()`, `showErrorState()`
   - 需要自定义图标时使用完整事件

3. **自定义缺省页样式**
   - 重写 `getEmptyStateConfig()` 方法
   - 配置自定义图标、文案、样式

4. **重试机制**
   - 在 `showEmptyState/showErrorState` 中传入 `onRetry` 回调
   - 回调中重新发送 Intent

## 注意事项

1. **跨模块 sealed class 限制**
   - `MviUiEvent` 不继承 `UiEvent`（Kotlin 限制）
   - 通过独立的事件流 `uiUiEvent` 传递

2. **ViewModel 类型**
   - `MviUiActivity` 要求 ViewModel 类型为 `MviUiViewModel`
   - 确保 ViewModel 继承自 `MviUiViewModel` 而非 `MviViewModel`

3. **事件观察**
   - 基础事件 (`UiEvent`) 由父类自动观察
   - UI 扩展事件 (`MviUiEvent`) 由 `MviUiActivity/Fragment` 观察

---

**创建时间:** 2025-12-16
**模块版本:** 1.0.0
**作者:** Claude AI Assistant
