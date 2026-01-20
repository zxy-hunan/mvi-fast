# aFramework

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-blue.svg)](https://kotlinlang.org)
[![Gradle](https://img.shields.io/badge/Gradle-8.6.1-green.svg)](https://gradle.org)
[![MinSdk](https://img.shields.io/badge/MinSdk-23-orange.svg)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

一个基于 **MVI (Model-View-Intent)** 架构模式的现代化 Android 开发框架，专注于简洁、高效和可维护性。

## 特性

### 核心特性

- **MVI 架构模式**：单向数据流，状态可预测，易于测试和调试
- **Kotlin 协程**：全面使用 Coroutines 和 Flow 进行异步编程
- **ViewBinding**：类型安全的视图绑定，告别 findViewById
- **模块化设计**：核心层(mvi-core) + UI层(mvi-ui) + 业务层分离
- **现代化技术栈**：Kotlin 2.0、Retrofit、OkHttp、MMKV 等主流框架

### 架构优势

- **简化的 Intent 流**：单一 Intent 流替代复杂的双 Intent 系统
- **StateFlow 状态管理**：响应式状态更新，自动 UI 刷新
- **Channel 事件处理**：一次性事件（如 Toast、导航）不会重复触发
- **DSL 风格 API**：优雅的网络请求封装，减少样板代码
- **扁平化 ViewModel**：精简的继承层次（1层而非4层）

## 快速开始

### 环境要求

- **Android Studio**: Meerkat | 2024.3.2 或更高版本 (推荐)
- **JDK**: 17 (必须)
- **Gradle**: 8.6.1 (必须)
- **Kotlin**: 2.0.0
- **Android Gradle Plugin (AGP)**: 8.6.1
- **Android SDK**: 最低 API 23 (Android 6.0)，推荐 API 26+ (Android 8.0+)

### 安装

#### 方式一：作为模块依赖（推荐）

1. 将 `aFramework` 项目复制到你的工作目录

2. 在 `settings.gradle` 中引入模块：

```gradle
include ':mvi-core'
project(':mvi-core').projectDir = new File('../aFramework/mvi-core')

include ':mvi-ui'
project(':mvi-ui').projectDir = new File('../aFramework/mvi-ui')
```

3. 在 app 的 `build.gradle` 中添加依赖：

```gradle
dependencies {
    // 只依赖 mvi-ui 即可（会自动包含 mvi-core）
    implementation project(':mvi-ui')
}
```

#### 方式二：直接集成源码

将 `mvi-core` 和 `mvi-ui` 模块直接复制到你的项目中，然后在 `settings.gradle` 中添加：

```gradle
include ':mvi-core'
include ':mvi-ui'
```

### 基础用法

#### 1. 定义 Intent（用户意图）

```kotlin
sealed class UserIntent : MviIntent {
    data object LoadUsers : UserIntent()
    data class SearchUser(val keyword: String) : UserIntent()
    data class DeleteUser(val userId: String) : UserIntent()
}
```

#### 2. 创建 ViewModel

```kotlin
class UserViewModel : MviViewModel<UserIntent>() {
    // 状态管理
    private val _userListState = MutableStateFlow<UiState<List<User>>>(UiState.Idle)
    val userListState = _userListState.asStateFlow()

    override fun handleIntent(intent: UserIntent) {
        when (intent) {
            is UserIntent.LoadUsers -> loadUsers()
            is UserIntent.SearchUser -> searchUser(intent.keyword)
            is UserIntent.DeleteUser -> deleteUser(intent.userId)
        }
    }

    private fun loadUsers() {
        launchRequest(_userListState) {
            apiService.getUserList()
        }
    }

    private fun deleteUser(userId: String) {
        launchRequest(_userListState, showLoading = false) {
            apiService.deleteUser(userId)
        }.invokeOnCompletion {
            sendEvent(UiEvent.ShowToast("删除成功"))
            sendIntent(UserIntent.LoadUsers) // 刷新列表
        }
    }
}
```

#### 3. Activity/Fragment 中使用

```kotlin
class UserListActivity : MviActivity<ActivityUserListBinding, UserIntent, UserViewModel>() {

    override fun createViewModel() = UserViewModel()

    override fun initView() {
        binding.btnLoad.setOnClickListener {
            sendIntent(UserIntent.LoadUsers)
        }

        binding.swipeRefresh.setOnRefreshListener {
            sendIntent(UserIntent.LoadUsers)
        }
    }

    override fun observeState() {
        // 监听状态变化
        viewModel.userListState.collectState { state ->
            when (state) {
                is UiState.Loading -> showLoading()
                is UiState.Success -> showUserList(state.data)
                is UiState.Error -> showError(state.message)
                else -> {}
            }
        }

        // 监听一次性事件
        viewModel.uiEvent.collectEvent { event ->
            when (event) {
                is UiEvent.ShowToast -> toast(event.message)
                is UiEvent.Navigate -> navigateTo(event.route)
            }
        }
    }
}
```

## 项目结构

```text
aFramework/
├── mvi-core/                    # 核心模块
│   ├── base/                    # 基础类
│   │   ├── MviActivity.kt       # Activity 基类
│   │   ├── MviFragment.kt       # Fragment 基类
│   │   ├── MviViewModel.kt      # ViewModel 基类
│   │   ├── MviIntent.kt         # Intent 标记接口
│   │   ├── UiState.kt           # 状态封装
│   │   └── UiEvent.kt           # 事件封装
│   ├── ext/                     # 扩展函数
│   │   ├── NetworkExt.kt        # 网络请求扩展
│   │   ├── FlowExt.kt           # Flow 扩展
│   │   └── ViewExt.kt           # View 扩展
│   ├── network/                 # 网络层
│   │   ├── ApiResponse.kt       # 统一响应格式
│   │   ├── RetrofitClient.kt    # Retrofit 配置
│   │   └── NetworkException.kt  # 网络异常处理
│   └── storage/                 # 存储层
│       └── MmkvStorage.kt       # MMKV 封装
│
├── mvi-ui/                      # UI 模块
│   ├── dialog/                  # 对话框
│   ├── widget/                  # 自定义控件
│   └── adapter/                 # 通用适配器
│
├── demo/                        # 示例应用
│   ├── ui/                      # 界面
│   │   ├── UserListActivity.kt  # 用户列表示例
│   │   └── DialogDemoActivity.kt # 对话框示例
│   └── data/                    # 数据层
│       └── api/                 # API 定义
│
└── docs/                        # 文档
    ├── QUICKSTART.md            # 快速入门指南
    ├── ARCHITECTURE.md          # 架构设计文档
    └── PROJECT_STRUCTURE.md     # 项目结构说明
```

## 核心概念

### MVI 数据流

```text
┌─────────┐
│  Intent │  用户意图（点击、输入等）
└────┬────┘
     │
     ▼
┌─────────┐
│  Model  │  处理业务逻辑，更新状态
└────┬────┘
     │
     ▼
┌─────────┐
│  View   │  根据状态渲染 UI
└─────────┘
```

### 状态管理

框架提供了标准的 `UiState` 封装，包含5种状态：

- **Idle**：空闲状态
- **Loading**：加载中
- **Success**：成功（携带数据）
- **Error**：错误（携带错误信息）
- **Empty**：空数据

### 事件处理

使用 `Channel` 处理一次性事件，避免配置更改时重复触发：

```kotlin
sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class Navigate(val route: String) : UiEvent()
    data class ShowDialog(val title: String, val message: String) : UiEvent()
}
```

## 技术栈

### 核心框架

- **Kotlin 2.0.0**：现代化编程语言
- **Coroutines 1.7.3**：异步编程
- **Flow**：响应式数据流

### AndroidX 组件

- **Lifecycle 2.6.1**：生命周期感知组件
- **ViewBinding**：视图绑定
- **AppCompat 1.6.1**：向下兼容

### 网络层

- **Retrofit 2.9.0**：RESTful API 客户端
- **OkHttp 4.11.0**：HTTP 客户端
- **Gson 2.10**：JSON 序列化

### 存储层

- **MMKV 2.0.1**：高性能键值存储
- **MMKV-KTX 1.2.14**：Kotlin 扩展

### UI 组件

- **Material 1.9.0**：Material Design 组件
- **Layer 1.0.7**：对话框框架
- **Banner 2.2.3**：轮播图组件
- **ShapeView 8.5**：形状视图
- **AndroidAutoSize v1.2.1**：屏幕适配

### 工具库

- **UtilCodeX 1.31.1**：Android 工具类
- **XXPermissions 16.2**：权限请求框架
- **Glide 4.15.1**：图片加载（可选）

## 示例应用

项目包含完整的示例应用 `demo`，展示了以下功能：

### 1. 用户列表（UserListActivity）

- 加载用户列表
- 下拉刷新
- 搜索用户
- 删除用户
- 状态管理演示

### 2. 对话框示例（DialogDemoActivity）

- 通用对话框
- 加载对话框
- 自定义对话框

### 运行示例

```bash
# 克隆项目
git clone <repository-url>

# 打开 Android Studio，导入项目

# 运行 demo 模块
./gradlew :demo:installDebug
```

## 最佳实践

### 1. Intent 命名规范

- 使用动词开头：`LoadData`、`UpdateUser`、`DeleteItem`
- 携带参数的使用 data class：`data class SearchUser(val keyword: String)`
- 简单操作使用 object：`object RefreshList`

### 2. 状态管理建议

- 每个页面维护独立的状态 StateFlow
- 使用 `UiState<T>` 封装加载状态
- 避免在 View 层直接修改状态

### 3. 网络请求优化

- 使用 `launchRequest` DSL 简化请求代码
- 统一异常处理，避免重复 try-catch
- 合理使用 `showLoading` 参数控制加载提示

### 4. 生命周期安全

- 在 `observeState()` 中使用 `collectState` 自动处理生命周期
- 一次性事件使用 `collectEvent` 避免重复触发
- ViewModel 中避免持有 Activity/Fragment 引用

## 对比原框架

| 特性 | aFramework | 原 mvi-fast |
|-----|-----------|------------|
| Intent 流 | 单一流 | 双 Intent 流（BaseIntent + BizIntent） |
| ViewModel 层级 | 1 层 | 4 层（Base → Mvi → Biz → Page） |
| 状态管理 | StateFlow | SharedFlow |
| 事件处理 | Channel | SharedFlow |
| 学习曲线 | 平缓 | 陡峭 |
| 样板代码 | 少 | 多 |
| 维护性 | 高 | 中等 |

## 更新日志

### v1.0.0 (2025-12-17)

- 初始版本发布
- 实现核心 MVI 架构
- 完成 mvi-core 和 mvi-ui 模块
- 提供完整示例应用

## 常见问题

### Q: 如何处理多个并发请求？

A: 使用 `viewModelScope.launch` 启动多个协程，每个请求维护独立的状态。

### Q: 如何实现列表分页加载？

A: 维护一个 `List<T>` 状态，在加载更多时追加数据而不是替换。

### Q: 如何处理复杂的表单验证？

A: 创建独立的 Validation Intent，在 ViewModel 中集中处理验证逻辑。

### Q: 支持哪些 Android 版本？

A: 最低支持 Android 6.0 (API 23)，推荐 Android 8.0+ (API 26+)。

## 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 许可证

本项目采用 Apache License 2.0 许可证 - 详见 [LICENSE](LICENSE) 文件

## 联系方式

- Issues: [GitHub Issues](<https://github.com/yourusername/aFramework/issues>)
- Email: <your.email@example.com>

## 致谢

感谢以下开源项目的启发和支持：

- [Kotlin Coroutines](https://github.com/Kotlin/kotlinx.coroutines)
- [Retrofit](https://github.com/square/retrofit)
- [OkHttp](https://github.com/square/okhttp)
- [MMKV](https://github.com/Tencent/MMKV)
- [ViewBindingKTX](https://github.com/DylanCaiCoding/ViewBindingKTX)

---

## Made with ❤️ by aFramework Team
