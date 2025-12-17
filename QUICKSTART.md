# 🚀 aFramework 快速入门指南

> 5分钟上手 MVI 架构

---

## 📝 前置要求

- Android Studio Arctic Fox+
- Kotlin 1.8+
- Android SDK 23+

---

## 🎯 Step 1: 集成框架

### 1.1 添加模块依赖

在 `settings.gradle`:
```gradle
include ':mvi-core'
```

在 app 模块的 `build.gradle`:
```gradle
dependencies {
    implementation project(':mvi-core')
}
```

### 1.2 初始化框架

创建 `Application` 类:

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        // 初始化MMKV
        MmkvStorage.init(filesDir.absolutePath)

        // 初始化Retrofit
        RetrofitClient.init {
            baseUrl = "https://api.yourserver.com/"
            enableLogging = BuildConfig.DEBUG
        }
    }
}
```

在 `AndroidManifest.xml` 中声明:
```xml
<application
    android:name=".App"
    ...>
</application>
```

---

## 🏗️ Step 2: 创建第一个MVI页面

我们以一个**用户列表**功能为例:

### 2.1 定义数据模型

```kotlin
// User.kt
data class User(
    val id: String,
    val name: String,
    val email: String
)
```

### 2.2 定义 Intent

```kotlin
// UserIntent.kt
import com.mvi.core.base.MviIntent

sealed class UserIntent : MviIntent {
    data object LoadUsers : UserIntent()
    data object Refresh : UserIntent()
    data class DeleteUser(val id: String) : UserIntent()
}
```

> 💡 **Intent命名规范**: 使用动词开头,描述用户操作

### 2.3 创建 API 接口

```kotlin
// UserApi.kt
import com.mvi.core.network.ApiResponse
import retrofit2.http.GET
import retrofit2.http.DELETE
import retrofit2.http.Path

interface UserApi {
    @GET("/api/users")
    suspend fun getUserList(): ApiResponse<List<User>>

    @DELETE("/api/users/{id}")
    suspend fun deleteUser(@Path("id") id: String): ApiResponse<Boolean>
}
```

### 2.4 创建 ViewModel

```kotlin
// UserViewModel.kt
import com.mvi.core.base.MviViewModel
import com.mvi.core.base.UiState
import com.mvi.core.ext.launchRequest
import com.mvi.core.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserViewModel : MviViewModel<UserIntent>() {

    // 1. 创建API实例
    private val api = RetrofitClient.create(UserApi::class.java)

    // 2. 定义状态
    private val _userListState = MutableStateFlow<UiState<List<User>>>(UiState.Idle)
    val userListState = _userListState.asStateFlow()

    // 3. 处理Intent
    override fun handleIntent(intent: UserIntent) {
        when (intent) {
            is UserIntent.LoadUsers -> loadUsers()
            is UserIntent.Refresh -> refresh()
            is UserIntent.DeleteUser -> deleteUser(intent.id)
        }
    }

    // 4. 业务逻辑
    private fun loadUsers() {
        launchRequest(
            stateFlow = _userListState,
            showLoading = true
        ) {
            api.getUserList()
        }
    }

    private fun refresh() {
        launchRequest(
            stateFlow = _userListState,
            showLoading = false
        ) {
            api.getUserList()
        }
    }

    private fun deleteUser(id: String) {
        launchRequest(
            onSuccess = {
                showToast("删除成功")
                loadUsers() // 重新加载
            }
        ) {
            api.deleteUser(id)
        }
    }
}
```

> 💡 **launchRequest 两种用法**:
> - 传入 `stateFlow`: 自动更新状态
> - 传入 `onSuccess/onError`: 手动处理结果

### 2.5 创建布局文件

```xml
<!-- activity_user.xml -->
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="16dp">

    <Button
        android:id="@+id/btnLoad"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="加载用户" />

    <Button
        android:id="@+id/btnRefresh"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="刷新" />

    <ProgressBar
        android:id="@+id/progressBar"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:layout_gravity="center"
        android:visibility="gone" />

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerView"
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />

</LinearLayout>
```

### 2.6 创建 Activity

```kotlin
// UserActivity.kt
import android.view.LayoutInflater
import com.mvi.core.base.MviActivity
import com.mvi.core.base.UiState
import com.mvi.core.ext.collectOn
import com.mvi.core.ext.gone
import com.mvi.core.ext.visible

class UserActivity : MviActivity<ActivityUserBinding, UserViewModel, UserIntent>() {

    // 1. 创建ViewBinding
    override fun createBinding() =
        ActivityUserBinding.inflate(layoutInflater)

    // 2. 指定ViewModel类
    override fun getViewModelClass() =
        UserViewModel::class.java

    // 3. 初始化视图
    override fun initView() {
        // 设置点击事件
        binding.btnLoad.setOnClickListener {
            sendIntent(UserIntent.LoadUsers)
        }

        binding.btnRefresh.setOnClickListener {
            sendIntent(UserIntent.Refresh)
        }

        // 配置RecyclerView
        setupRecyclerView()
    }

    // 4. 观察数据
    override fun observeData() {
        viewModel.userListState.collectOn(this) { state ->
            when (state) {
                is UiState.Idle -> {
                    // 初始状态
                }

                is UiState.Loading -> {
                    // 加载中
                    binding.progressBar.visible()
                }

                is UiState.Success -> {
                    // 加载成功
                    binding.progressBar.gone()
                    updateUserList(state.data)
                }

                is UiState.Error -> {
                    // 加载失败
                    binding.progressBar.gone()
                    showError(state.message)
                }

                is UiState.Empty -> {
                    // 空数据
                    binding.progressBar.gone()
                    showEmpty()
                }
            }
        }
    }

    // 5. 处理Loading (可选)
    override fun handleLoading(show: Boolean) {
        binding.progressBar.visibleOrGone(show)
    }

    private fun setupRecyclerView() {
        // 配置RecyclerView Adapter
    }

    private fun updateUserList(users: List<User>) {
        // 更新RecyclerView数据
    }

    private fun showError(message: String) {
        // 显示错误UI
    }

    private fun showEmpty() {
        // 显示空状态UI
    }
}
```

---

## ✅ Step 3: 运行测试

构建并运行应用:
```bash
./gradlew assembleDebug
```

---

## 🎨 常用功能示例

### 1. 显示Toast

```kotlin
// 在ViewModel中
showToast("操作成功")
```

### 2. 显示Loading对话框

```kotlin
// 在ViewModel中
showLoading(true)  // 显示
showLoading(false) // 隐藏
```

### 3. 网络请求 (自动更新状态)

```kotlin
launchRequest(
    stateFlow = _dataState,
    showLoading = true
) {
    api.getData()
}
```

### 4. 网络请求 (回调方式)

```kotlin
launchRequest(
    onSuccess = { data ->
        // 处理成功
    },
    onError = { message, error ->
        // 处理错误
    }
) {
    api.getData()
}
```

### 5. MMKV存储

```kotlin
// 保存
MmkvStorage.putString("token", "xxx")
MmkvStorage.putInt("userId", 123)

// 读取
val token = MmkvStorage.getString("token")
val userId = MmkvStorage.getInt("userId")

// 委托属性
class UserSettings {
    var userName by mmkvDelegate("user_name", "")
    var age by mmkvDelegate("age", 0)
}
```

### 6. View扩展

```kotlin
// 显示/隐藏
view.visible()
view.gone()
view.visibleOrGone(condition)

// 防抖点击
button.setOnClickListener(interval = 500) {
    // 500ms内只触发一次
}
```

---

## 🐛 常见问题

### Q1: ViewModel状态不更新?

**A**: 确保使用 `collectOn` 扩展函数,它会自动处理生命周期:

```kotlin
// ❌ 错误
lifecycleScope.launch {
    viewModel.state.collect { }
}

// ✅ 正确
viewModel.state.collectOn(this) { }
```

### Q2: 如何处理一次性事件?

**A**: 使用 `sendEvent` 和 `uiEvent`:

```kotlin
// ViewModel中
sendEvent(UiEvent.ShowToast("登录成功"))
sendEvent(UiEvent.Navigate("home"))

// Activity/Fragment会自动处理
```

### Q3: 多个状态如何管理?

**A**: 每个独立功能使用独立的StateFlow:

```kotlin
class MyViewModel : MviViewModel<MyIntent>() {
    private val _userState = MutableStateFlow<UiState<User>>(UiState.Idle)
    val userState = _userState.asStateFlow()

    private val _orderState = MutableStateFlow<UiState<List<Order>>>(UiState.Idle)
    val orderState = _orderState.asStateFlow()
}
```

### Q4: Fragment如何使用?

**A**: 与Activity完全一致:

```kotlin
class UserFragment : MviFragment<FragmentUserBinding, UserViewModel, UserIntent>() {
    override fun createBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentUserBinding.inflate(inflater, container, false)

    override fun getViewModelClass() =
        UserViewModel::class.java

    // 其他方法与Activity一致
}
```

---

## 📚 下一步

1. 查看 [README.md](README.md) 了解完整功能
2. 查看 [ARCHITECTURE.md](ARCHITECTURE.md) 了解架构设计
3. 查看 demo 模块的完整示例代码

---

## 🎉 恭喜!

你已经完成了 aFramework 的快速入门!

现在你可以:
- ✅ 创建 MVI 架构的页面
- ✅ 管理 UI 状态
- ✅ 处理网络请求
- ✅ 使用本地存储

**Happy Coding! 🚀**
