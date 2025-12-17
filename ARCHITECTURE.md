# aFramework 架构设计文档

## 📋 目录
1. [优化设计理念](#优化设计理念)
2. [核心架构对比](#核心架构对比)
3. [技术选型对比](#技术选型对比)
4. [代码量对比](#代码量对比)
5. [性能优化](#性能优化)

---

## 🎯 优化设计理念

### 原框架问题分析

| 问题 | 原因 | 影响 |
|------|------|------|
| 双层Intent设计复杂 | BaseIntent + BizIntent 分离 | 学习成本高、代码冗余 |
| SharedFlow语义不清 | 用于状态管理不合适 | 容易误用、状态混乱 |
| ViewModel继承层次深 | 4层继承关系 | 代码难以维护、职责不清 |
| 缺少生命周期自动管理 | 手动订阅Flow | 容易内存泄漏 |
| 网络请求回调复杂 | 多层回调嵌套 | 代码可读性差 |

### aFramework 解决方案

✅ **单一状态流** - StateFlow管理状态 + Channel处理事件
✅ **扁平化设计** - 最多1层继承,职责清晰
✅ **自动生命周期** - repeatOnLifecycle自动管理
✅ **DSL风格API** - 简洁优雅的扩展函数
✅ **类型安全** - Sealed Class编译期检查

---

## 🏗️ 核心架构对比

### 1. Intent管理

#### 原框架 (双层设计)
```kotlin
// 框架层 - BaseIntent
sealed class BaseIntent {
    class ShowLoading : BaseIntent()
    class ShowContent : BaseIntent()
    class ShowError : BaseIntent()
    class ShowToast : BaseIntent()
}

// 业务层 - BizIntent
interface BizIntent {}

sealed class AssetIntent : BizIntent {
    data class Symbols(val data: List<Symbol>) : AssetIntent()
    data class AssetByCoin(val data: List<Asset>) : AssetIntent()
}

// ViewModel中管理两个流
class BaseViewModel<I> {
    protected val _baseIntent = MutableSharedFlow<BaseIntent>()
    protected val _intent = MutableSharedFlow<I>()
}
```

**问题**:
- 需要同时管理两个Intent流
- BaseIntent和BizIntent职责划分不清晰
- SharedFlow用于状态管理语义不正确

#### aFramework (统一设计)
```kotlin
// 统一的Intent接口
interface MviIntent

sealed class UserIntent : MviIntent {
    data object LoadUsers : UserIntent()
    data class DeleteUser(val id: String) : UserIntent()
}

// UI状态封装
sealed class UiState<out T> {
    data object Idle : UiState<Nothing>()
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

// UI事件 (一次性)
sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class ShowLoading(val show: Boolean) : UiEvent()
}

// ViewModel中清晰分离
class MviViewModel<I : MviIntent> {
    private val _intent = MutableSharedFlow<I>()          // Intent流
    private val _uiEvent = Channel<UiEvent>()             // 一次性事件
    // StateFlow在具体ViewModel中管理状态
}
```

**优势**:
- Intent、State、Event职责清晰
- StateFlow管理状态,Channel处理事件
- 语义更准确,不易误用

---

### 2. ViewModel 层次

#### 原框架 (4层继承)
```kotlin
BaseViewModel<I>                    // 1. 框架基类
    ↓
UserBaseViewModel<I>                // 2. 用户数据层
    ↓
MiningBaseViewModel<I>              // 3. 业务中间层
    ↓
AssetViewModel                      // 4. 具体ViewModel

// 继承关系复杂,职责混乱
class AssetViewModel : MiningBaseViewModel<AssetIntent>() {
    // 需要理解4层继承关系
}
```

**问题**:
- 继承层次过深
- 职责划分不清晰
- 维护成本高

#### aFramework (1层继承)
```kotlin
MviViewModel<I>                     // 唯一基类
    ↓
UserViewModel                       // 直接继承

// 简洁清晰
class UserViewModel : MviViewModel<UserIntent>() {
    // 只需要理解1层继承
    override fun handleIntent(intent: UserIntent) {
        when (intent) {
            is UserIntent.LoadUsers -> loadUsers()
        }
    }
}
```

**优势**:
- 扁平化设计
- 职责单一
- 易于理解和维护

---

### 3. 网络请求

#### 原框架
```kotlin
fun assetBycoin(coin: String) {
    assetWalletService.assetList(...)
        .zhiuHttpCoroutine(
            onSuccess = { response ->
                matchAssetcoinIcons(response.data)
                _intent.emitCoroutine(AssetIntent.assetBycoin(response.data))
            },
            onError = {
                _intent.emitCoroutine(AssetIntent.assetBycoin(mutableListOf()))
            }
        )
}
```

**问题**:
- 需要手动emit Intent
- 错误处理不统一
- 缺少Loading状态管理

#### aFramework
```kotlin
// 方式一: 自动更新StateFlow
private fun loadUsers() {
    launchRequest(
        stateFlow = _userListState,
        showLoading = true
    ) {
        api.getUserList()
    }
    // 自动处理Loading、Success、Error状态
    // 自动更新StateFlow
}

// 方式二: 回调方式
private fun deleteUser(id: String) {
    launchRequest(
        onSuccess = { success ->
            showToast("删除成功")
            loadUsers()
        },
        onError = { message, error ->
            showToast("删除失败: $message")
        }
    ) {
        api.deleteUser(id)
    }
}
```

**优势**:
- 自动处理Loading、Error状态
- 统一的错误处理
- DSL风格,代码简洁

---

### 4. Activity/Fragment

#### 原框架
```kotlin
class AssetAcy : MviAct<ActivityAssetBinding, AssetViewModel, AssetIntent>(
    vMCls = AssetViewModel::class.java,
    titleRes = R.string.main_tab3
) {
    override fun ob() {
        // 手动订阅
        intentCallback { intent ->
            when (intent) {
                is AssetIntent.Symbols -> handleSymbols(intent)
                is AssetIntent.assetBycoin -> handleAssetByCoin(intent)
            }
        }
    }

    override fun initView() { }
    override fun onListener() { }
}
```

**问题**:
- 需要手动管理订阅生命周期
- 方法名不够语义化 (ob, onListener)

#### aFramework
```kotlin
class UserActivity : MviActivity<ActivityUserBinding, UserViewModel, UserIntent>() {

    override fun createBinding() =
        ActivityUserBinding.inflate(layoutInflater)

    override fun getViewModelClass() =
        UserViewModel::class.java

    override fun initView() {
        binding.btnLoad.setOnClickListener {
            sendIntent(UserIntent.LoadUsers)
        }
    }

    override fun observeData() {
        // 自动管理生命周期
        viewModel.userListState.collectOn(this) { state ->
            when (state) {
                is UiState.Loading -> showLoading()
                is UiState.Success -> showUsers(state.data)
                is UiState.Error -> showError(state.message)
            }
        }
    }
}
```

**优势**:
- 自动管理生命周期 (collectOn扩展函数)
- 方法命名更语义化
- repeatOnLifecycle避免内存泄漏

---

## 🔧 技术选型对比

| 技术点 | 原框架 | aFramework | 理由 |
|--------|--------|------------|------|
| 状态管理 | SharedFlow | StateFlow | StateFlow语义更准确 |
| 事件管理 | SharedFlow | Channel | Channel保证一次性消费 |
| 生命周期 | 手动管理 | repeatOnLifecycle | 自动处理,避免泄漏 |
| 网络封装 | Flow扩展 | DSL扩展函数 | 更简洁易用 |
| 存储 | MMKV-KTX | MmkvStorage单例 | 统一管理,使用简单 |
| ViewBinding | 反射 | 泛型 | 性能更好 |

---

## 📊 代码量对比

### 原框架
```
system/biz/base/
├── BaseIntent.kt              ~50 lines
├── BizIntent.kt               ~5 lines
├── BaseViewModel.kt           ~150 lines
├── MviAct.kt                  ~120 lines
├── MviFrg.kt                  ~100 lines
└── NoDataViewModel.kt         ~20 lines
总计: ~445 lines
```

### aFramework
```
mvi-core/base/
├── MviIntent.kt               ~5 lines
├── UiState.kt                 ~25 lines
├── MviViewModel.kt            ~60 lines
├── MviActivity.kt             ~80 lines
└── MviFragment.kt             ~75 lines
总计: ~245 lines
```

**减少代码量: ~45%**

---

## 🚀 性能优化

### 1. StateFlow vs SharedFlow

```kotlin
// SharedFlow (原框架)
val sharedFlow = MutableSharedFlow<Data>(replay = 1)
// - 每次订阅都会创建新的收集器
// - replay机制占用额外内存

// StateFlow (aFramework)
val stateFlow = MutableStateFlow<Data>(initialValue)
// - 永远只有当前值
// - 冲突合并策略,性能更好
```

### 2. Channel vs Flow for Events

```kotlin
// SharedFlow处理一次性事件 (原框架)
val eventFlow = MutableSharedFlow<Event>()
// 问题: 可能重复消费,需要手动处理

// Channel处理一次性事件 (aFramework)
val eventChannel = Channel<UiEvent>()
val uiEvent = eventChannel.receiveAsFlow()
// 保证只消费一次,语义正确
```

### 3. 生命周期优化

```kotlin
// 原框架 - 手动管理
lifecycleScope.launch {
    viewModel.intent.collect { }
}
// 问题: 在STOPPED状态仍然收集,浪费资源

// aFramework - 自动管理
viewModel.state.collectOn(this) { }
// 内部使用 repeatOnLifecycle(STARTED)
// 在STOPPED时自动暂停,STARTED时恢复
```

---

## 📈 使用体验对比

### 场景: 实现一个列表加载功能

#### 原框架 (需要7个步骤)

1. 定义BizIntent
```kotlin
sealed class AssetIntent : BizIntent {
    data class AssetList(val data: List<Asset>) : AssetIntent()
}
```

2. 创建ViewModel (继承4层)
```kotlin
class AssetViewModel : MiningBaseViewModel<AssetIntent>() {
    fun loadAssets() {
        assetService.getList()
            .zhiuHttpCoroutine(
                onSuccess = {
                    _intent.emitCoroutine(AssetIntent.AssetList(it.data))
                }
            )
    }
}
```

3. 在Activity中订阅
```kotlin
override fun ob() {
    intentCallback { intent ->
        when (intent) {
            is AssetIntent.AssetList -> updateList(intent.data)
        }
    }
}
```

#### aFramework (需要3个步骤)

1. 定义Intent
```kotlin
sealed class AssetIntent : MviIntent {
    data object LoadAssets : AssetIntent()
}
```

2. 创建ViewModel
```kotlin
class AssetViewModel : MviViewModel<AssetIntent>() {
    private val _assets = MutableStateFlow<UiState<List<Asset>>>(UiState.Idle)
    val assets = _assets.asStateFlow()

    override fun handleIntent(intent: AssetIntent) {
        when (intent) {
            is AssetIntent.LoadAssets -> launchRequest(_assets) {
                api.getAssets()
            }
        }
    }
}
```

3. 在Activity中观察
```kotlin
override fun observeData() {
    viewModel.assets.collectOn(this) { state ->
        when (state) {
            is UiState.Success -> updateList(state.data)
            is UiState.Error -> showError(state.message)
        }
    }
}
```

**代码量减少: ~40%**
**概念简化: 移除了BaseIntent、多层继承、手动emit等概念**

---

## 🎯 总结

### aFramework 核心优势

1. **更简单** - 代码量减少45%,概念更少
2. **更安全** - 生命周期自动管理,避免泄漏
3. **更清晰** - StateFlow、Channel语义明确
4. **更现代** - Kotlin DSL风格,符合现代开发习惯
5. **更高效** - 扁平化设计,性能更好

### 适用场景

✅ **新项目** - 直接使用aFramework
✅ **中小型项目** - 架构简洁,快速开发
✅ **学习MVI** - 概念清晰,易于理解
⚠️ **大型项目** - 可根据需求扩展

---

**设计者: AI Assistant**
**创建时间: 2024-12-12**
