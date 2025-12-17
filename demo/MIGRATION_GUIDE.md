# Demo 模块迁移指南

## ✅ 新增示例

成功添加了 **MviUi 用户列表示例**,展示如何使用增强版的 `MviUiActivity` 和 `MviUiViewModel`。

### 新增文件

1. **[UserListIntent.kt](E:\soft\aFramework\demo\src\main\java\com\mvi\demo\ui\UserListIntent.kt)** - 用户列表的 Intent 定义
2. **[UserListViewModel.kt](E:\soft\aFramework\demo\src\main\java\com\mvi\demo\ui\UserListViewModel.kt)** - 使用 `MviUiViewModel` 的示例 ViewModel
3. **[UserListActivity.kt](E:\soft\aFramework\demo\src\main\java\com\mvi\demo\ui\UserListActivity.kt)** - 使用 `MviUiActivity` 的示例 Activity
4. **[activity_user_list.xml](E:\soft\aFramework\demo\src\main\res\layout\activity_user_list.xml)** - Activity 布局
5. **[item_user.xml](E:\soft\aFramework\demo\src\main\res\layout\item_user.xml)** - 列表项布局

### 修改文件

1. **[AndroidManifest.xml](E:\soft\aFramework\demo\src\main\AndroidManifest.xml)** - 注册新的 Activity
2. **[DemoActivity.kt](E:\soft\aFramework\demo\src\main\java\com\mvi\demo\ui\DemoActivity.kt)** - 添加入口按钮
3. **[activity_demo.xml](E:\soft\aFramework\demo\src\main\res\layout\activity_demo.xml)** - 添加"MviUi 用户列表示例"按钮

## ✅ 已完成的导入修复

所有 UI 相关类已从 `mvi-core` 移动到 `mvi-ui`，以下导入已自动修复：

### 已修复的文件

1. **ComprehensiveDemoActivity.kt** ✅
   - ✅ `com.mvi.core.widget.EmptyStateConfig` → `com.mvi.ui.widget.EmptyStateConfig`
   - ✅ `com.mvi.core.widget.RecyclerViewSkeletonManager` → `com.mvi.ui.widget.RecyclerViewSkeletonManager`
   - ✅ `com.mvi.core.widget.SkeletonConfig` → `com.mvi.ui.widget.SkeletonConfig`

2. **EmptyStateDemoActivity.kt** ✅
   - ✅ `com.mvi.core.widget.EmptyStateConfig` → `com.mvi.ui.widget.EmptyStateConfig`

3. **SkeletonDemoActivity.kt** ✅
   - ✅ `com.mvi.core.widget.RecyclerViewSkeletonManager` → `com.mvi.ui.widget.RecyclerViewSkeletonManager`
   - ✅ `com.mvi.core.widget.SkeletonConfig` → `com.mvi.ui.widget.SkeletonConfig`
   - ✅ `com.mvi.core.widget.SkeletonManager` → `com.mvi.ui.widget.SkeletonManager`

4. **DialogDemoActivity.kt** ✅
   - ✅ `com.mvi.core.base.MviDialog` → `com.mvi.ui.base.MviDialog`

5. **DemoDialogs.kt** ✅
   - ✅ `com.mvi.core.base.MviBottomDialog` → `com.mvi.ui.base.MviBottomDialog`
   - ✅ `com.mvi.core.base.MviCenterDialog` → `com.mvi.ui.base.MviCenterDialog`
   - ✅ `com.mvi.core.base.MviDialog` → `com.mvi.ui.base.MviDialog`

6. **UserInfoDialog.kt** ✅
   - ✅ `com.mvi.core.base.MviViewModelDialog` → `com.mvi.ui.base.MviViewModelDialog`

7. **UserListViewModel.kt** ✅
   - ✅ 修复 User 构造函数调用（添加 avatar 参数）

## ⚠️ 需要手动处理的架构变更

由于将 EmptyStateManager 和 SkeletonManager 从 `MviActivity` 移到了 `MviUiActivity`，以下旧的 Demo Activity 需要选择一种迁移方案：

### 受影响的文件：

1. **ComprehensiveDemoActivity.kt** - 使用了 `getEmptyStateManager()`, `getEmptyStateContainer()`, `getEmptyStateConfig()`, `getSkeletonConfig()`
2. **EmptyStateDemoActivity.kt** - 使用了 `getEmptyStateManager()`, `getEmptyStateContainer()`, `getEmptyStateConfig()`
3. **SkeletonDemoActivity.kt** - 使用了 `getSkeletonConfig()`, `createSkeletonManager()`

### 迁移方案（三选一）：

#### 方案一：迁移到 MviUiActivity（推荐）✨

将这些 Activity 改为继承 `MviUiActivity`，这样可以自动使用 EmptyStateManager 和 SkeletonManager：

```kotlin
// 旧代码
class ComprehensiveDemoActivity : MviActivity<...>() {

// 新代码
class ComprehensiveDemoActivity : MviUiActivity<...>() {
```

**优点**：
- 无需手动管理 EmptyStateManager
- 自动支持 ViewModel 的 UI 事件
- 代码最简洁

**需要修改**：
- 继承改为 `MviUiActivity`
- ViewModel 继承改为 `MviUiViewModel`（可选）

#### 方案二：手动管理 Manager

继续使用 `MviActivity`，但手动创建和管理 EmptyStateManager 和 SkeletonManager：

```kotlin
class ComprehensiveDemoActivity : MviActivity<...>() {
    private var emptyStateManager: EmptyStateManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        emptyStateManager = EmptyStateManager(
            container = binding.contentContainer,
            config = EmptyStateConfig(...)
        )
    }
}
```

**优点**：
- 保持使用 `MviActivity`
- 手动控制更灵活

**缺点**：
- 代码更冗长
- 需要手动管理生命周期

#### 方案三：移除不需要的功能

如果这些 Demo 只是为了演示基本功能，可以简化它们，移除对 EmptyStateManager 的依赖。

## 🔧 推荐的快速修复步骤

### 步骤 1: 迁移到 MviUiActivity（推荐）

修改三个文件的继承关系：

1. **ComprehensiveDemoActivity.kt**:
```kotlin
// 修改继承
class ComprehensiveDemoActivity : MviUiActivity<ActivityComprehensiveDemoBinding, ComprehensiveDemoViewModel, ComprehensiveDemoIntent>() {
    // getEmptyStateContainer(), getEmptyStateConfig(), getSkeletonConfig() 保持不变
    // 其他代码保持不变
}
```

2. **EmptyStateDemoActivity.kt**:
```kotlin
class EmptyStateDemoActivity : MviUiActivity<ActivityEmptyStateDemoBinding, EmptyStateDemoViewModel, EmptyStateDemoIntent>() {
    // getEmptyStateContainer(), getEmptyStateConfig() 保持不变
    // 其他代码保持不变
}
```

3. **SkeletonDemoActivity.kt**:
```kotlin
class SkeletonDemoActivity : MviUiActivity<ActivitySkeletonDemoBinding, SkeletonDemoViewModel, SkeletonDemoIntent>() {
    // getSkeletonConfig() 保持不变
    // createSkeletonManager() 需要替换为手动创建
}
```

### 步骤 2: 修复 SkeletonDemoActivity 中的 createSkeletonManager

在 `SkeletonDemoActivity.kt` 的 `showViewSkeleton()` 方法中：

```kotlin
// 旧代码
private fun showViewSkeleton() {
    if (viewSkeletonManager == null) {
        viewSkeletonManager = createSkeletonManager(binding.contentCard)
    }
    viewSkeletonManager?.show()
}

// 新代码
private fun showViewSkeleton() {
    if (viewSkeletonManager == null) {
        viewSkeletonManager = SkeletonManager(
            view = binding.contentCard,
            config = getSkeletonConfig()
        )
    }
    viewSkeletonManager?.show()
}
```

## 📚 新增示例的特性

UserListActivity 展示了以下 MviUi 特性：

1. **自动空状态管理** - ViewModel 通过 `showEmptyState()` 自动显示缺省页
2. **自动错误处理** - ViewModel 通过 `showErrorState()` 自动显示错误状态
3. **自动重试** - 空状态和错误状态支持重试回调
4. **状态循环演示** - 点击 FAB 按钮循环展示：成功→空数据→错误→成功
5. **下拉刷新** - SwipeRefreshLayout 集成
6. **列表展示** - RecyclerView + Adapter

## 🎯 运行示例

1. 应用上述推荐的修复步骤
2. 编译项目: `./gradlew :demo:assembleDebug`
3. 运行 Demo 应用
4. 点击"MviUi 用户列表示例 (NEW)"按钮
5. 点击右下角的 FAB 按钮切换不同的状态

---

**创建时间:** 2025-12-16
**作者:** Claude AI Assistant
**最后更新:** 2025-12-16 (添加架构变更说明)
