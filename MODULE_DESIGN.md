# aFramework 模块化设计方案

## 问题背景

当前 `mvi-core` 是一个 library 模块,包含了核心架构、网络请求、存储等功能。现在需要引入更多**业务层和显示层框架**(如 Glide、AndroidAutoSize、ImmersionBar 等),但不想放在 app 模块中。

## 推荐方案:多模块分层架构

### 模块划分

```
aFramework/
├── mvi-core/          # 核心基础库(纯架构+基础设施)
├── mvi-ui/            # UI组件库(业务UI组件+显示层框架)
├── demo/              # 示例应用
└── [your-app]/        # 实际应用
```

---

## 模块一:mvi-core (核心基础库)

### 职责定位
**提供 MVI 架构核心能力和基础基础设施,不包含任何UI相关代码**

### 包含内容

#### 1. MVI 架构核心
```
com.mvi.core.base/
├── MviIntent.kt           # Intent 接口
├── MviViewModel.kt        # ViewModel 基类
├── MviActivity.kt         # Activity 基类
├── MviFragment.kt         # Fragment 基类
└── UiState.kt            # UI 状态封装
```

#### 2. 网络请求
```
com.mvi.core.network/
├── RetrofitClient.kt      # Retrofit 封装
├── ApiResponse.kt         # 统一响应封装
├── NetworkExt.kt          # 网络扩展函数
├── upload/                # 文件上传
└── download/              # 文件下载
```

#### 3. 本地存储
```
com.mvi.core.storage/
└── MmkvStorage.kt         # MMKV 封装
```

#### 4. 异常处理
```
com.mvi.core.exception/
├── ExceptionHandler.kt    # 异常处理器
└── ExceptionExt.kt        # 异常扩展
```

#### 5. 权限管理
```
com.mvi.core.permission/
├── FilePermissionHelper.kt    # 权限帮助类
└── FilePermissionExt.kt       # 权限扩展
```

#### 6. 基础扩展
```
com.mvi.core.ext/
├── CoroutineExt.kt        # 协程扩展
├── ContextExt.kt          # Context 扩展
└── CollectionExt.kt       # 集合扩展
```

### 依赖清单
```gradle
dependencies {
    // Kotlin & AndroidX 核心
    api "androidx.core:core-ktx:1.9.0"
    api "androidx.appcompat:appcompat:1.6.1"
    api "androidx.lifecycle:lifecycle-*:2.6.1"

    // 协程
    api "org.jetbrains.kotlinx:kotlinx-coroutines-*:1.7.3"

    // 网络
    api "com.squareup.retrofit2:retrofit:2.9.0"
    api "com.squareup.okhttp3:okhttp:4.11.0"

    // 存储
    api "com.tencent:mmkv-static:2.0.1"

    // 序列化
    api "com.google.code.gson:gson:2.10"

    // 权限
    api "com.github.getActivity:XXPermissions:16.2"
}
```

### 不包含的内容
- ❌ UI 组件库 (Glide、Coil等)
- ❌ 屏幕适配 (AndroidAutoSize)
- ❌ 沉浸式状态栏 (ImmersionBar)
- ❌ 对话框库 (Layer)
- ❌ 缺省页、骨架屏 (LoadingStateView)
- ❌ ViewBinding 扩展 (非核心)

---

## 模块二:mvi-ui (UI组件库)

### 职责定位
**提供通用UI组件、显示层框架和业务相关的UI扩展**

### 包含内容

#### 1. 图片加载
```
com.mvi.ui.image/
├── ImageLoader.kt         # 统一图片加载器
└── GlideExt.kt           # Glide 扩展
```

#### 2. 屏幕适配
```
com.mvi.ui.autosize/
└── AutoSizeExt.kt        # 屏幕适配扩展(可选)
```

#### 3. 沉浸式状态栏
```
com.mvi.ui.immersion/
└── ImmersionExt.kt       # 沉浸式扩展
```

#### 4. 对话框
```
com.mvi.ui.dialog/
├── CommonDialog.kt        # 通用对话框
├── LoadingDialog.kt       # 加载对话框
└── DialogExt.kt          # 对话框扩展
```

#### 5. 缺省页
```
com.mvi.ui.empty/
├── EmptyStateManager.kt   # 缺省页管理
└── EmptyStateExt.kt      # 缺省页扩展
```

#### 6. RecyclerView
```
com.mvi.ui.recyclerview/
├── BaseAdapter.kt         # 基础适配器
└── RecyclerViewExt.kt    # RecyclerView 扩展
```

#### 7. 自定义 View
```
com.mvi.ui.widget/
├── LoadingView.kt         # 加载视图
├── EmptyView.kt          # 空状态视图
└── ...                   # 其他自定义 View
```

#### 8. ViewBinding 扩展
```
com.mvi.ui.binding/
└── ViewBindingExt.kt     # ViewBinding 扩展
```

### 依赖清单
```gradle
dependencies {
    // 依赖核心库
    api project(':mvi-core')

    // 图片加载
    api "com.github.bumptech.glide:glide:4.15.1"
    kapt "com.github.bumptech.glide:compiler:4.15.1"

    // 屏幕适配
    api "com.github.JessYanCoding:AndroidAutoSize:v1.2.1"

    // 沉浸式状态栏
    api "com.geyifeng.immersionbar:immersionbar:3.2.2"
    api "com.geyifeng.immersionbar:immersionbar-ktx:3.2.2"

    // 对话框
    api "com.github.goweii:Layer:1.0.7"

    // 缺省页
    api "com.github.DylanCaiCoding.LoadingStateView:loadingstateview-ktx:5.0.0"

    // ViewBinding
    api "com.github.DylanCaiCoding.ViewBindingKTX:viewbinding-ktx:2.1.0"
    api "com.github.DylanCaiCoding.ViewBindingKTX:viewbinding-base:2.1.0"

    // Material Design
    api "com.google.android.material:material:1.9.0"

    // RecyclerView
    api "androidx.recyclerview:recyclerview:1.3.0"
}
```

---

## 创建 mvi-ui 模块的步骤

### 1. 在 Android Studio 中创建模块

1. **File → New → New Module**
2. 选择 **"Android Library"**
3. 模块名称: `mvi-ui`
4. Package name: `com.mvi.ui`
5. 点击 **Finish**

### 2. 配置 mvi-ui/build.gradle

创建文件: `E:\soft\aFramework\mvi-ui\build.gradle`

```gradle
plugins {
    id 'com.android.library'
    id 'org.jetbrains.kotlin.android'
    id 'kotlin-kapt'
    id 'kotlin-parcelize'
}

android {
    namespace 'com.mvi.ui'
    compileSdk rootProject.ext.versions.compileSdk
    buildToolsVersion rootProject.ext.versions.buildTools

    defaultConfig {
        minSdk rootProject.ext.versions.minSdk
        targetSdk rootProject.ext.versions.targetSdk
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }

    compileOptions {
        sourceCompatibility rootProject.ext.versions.javaVersion
        targetCompatibility rootProject.ext.versions.javaVersion
    }

    kotlinOptions {
        jvmTarget = rootProject.ext.versions.jvmTarget
    }

    viewBinding.enabled = true
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    // ===== 核心库 =====
    api project(':mvi-core')

    // ===== UI 框架 =====

    // 图片加载
    api rootProject.ext.deps["glide"]
    kapt rootProject.ext.deps["glide-compiler"]

    // ViewBinding 扩展
    api rootProject.ext.deps["viewbinding-ktx"]
    api rootProject.ext.deps["viewbinding-base"]

    // 缺省页/骨架屏
    api rootProject.ext.deps["loading-state-view"]

    // 沉浸式状态栏
    api rootProject.ext.deps["immersionbar"]
    api rootProject.ext.deps["immersionbar-ktx"]

    // 对话框
    api rootProject.ext.deps["layer"]

    // 屏幕适配
    api rootProject.ext.deps["autosize"]

    // Material Design
    api rootProject.ext.deps["material"]

    // RecyclerView
    api rootProject.ext.deps["recyclerview"]

    // ConstraintLayout
    api rootProject.ext.deps["constraintlayout"]
}
```

### 3. 更新 settings.gradle

编辑文件: `E:\soft\aFramework\settings.gradle`

```gradle
include ':mvi-core'
include ':mvi-ui'      // 新增
include ':demo'
```

### 4. 创建 AndroidManifest.xml

创建文件: `E:\soft\aFramework\mvi-ui\src\main\AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- mvi-ui 模块不需要声明任何权限和配置 -->
    <!-- 所有配置由 app 模块或 mvi-core 提供 -->
</manifest>
```

### 5. 从 mvi-core 移动代码

将以下内容从 `mvi-core` 移动到 `mvi-ui`:

#### 需要移动的文件:
- `EmptyStateManager.kt` → `mvi-ui/src/main/java/com/mvi/ui/empty/`
- `EmptyStateExt.kt` → `mvi-ui/src/main/java/com/mvi/ui/empty/`
- 其他UI相关的扩展函数

#### 需要移动的依赖:
从 `mvi-core/build.gradle` 中移除:
```gradle
// 这些移到 mvi-ui
api rootProject.ext.deps["glide"]
api rootProject.ext.deps["viewbinding-ktx"]
api rootProject.ext.deps["loading-state-view"]
api rootProject.ext.deps["immersionbar"]
api rootProject.ext.deps["layer"]
api rootProject.ext.deps["autosize"]
```

### 6. 更新 mvi-core/build.gradle

保持 `mvi-core` 纯净:

```gradle
dependencies {
    // Kotlin
    api rootProject.ext.deps["kotlin-stdlib"]
    api rootProject.ext.deps["core-ktx"]

    // AndroidX
    api rootProject.ext.deps["appcompat"]
    api rootProject.ext.deps["lifecycle-process"]
    api rootProject.ext.deps["lifecycle-viewmodel-ktx"]
    api rootProject.ext.deps["lifecycle-runtime-ktx"]

    // Coroutines
    api rootProject.ext.deps["coroutines-core"]
    api rootProject.ext.deps["coroutines-android"]

    // Network
    api rootProject.ext.deps["retrofit"]
    api rootProject.ext.deps["retrofit-gson"]
    api platform(rootProject.ext.deps["okhttp-bom"])
    api rootProject.ext.deps["okhttp"]
    api rootProject.ext.deps["okhttp-logging"]
    api rootProject.ext.deps["kotlinx-serialization"]

    // Storage
    api rootProject.ext.deps["mmkv"]
    api rootProject.ext.deps["mmkv-ktx"]

    // Utils
    api rootProject.ext.deps["gson"]
    api rootProject.ext.deps["utilcodex"]

    // Permissions
    api(rootProject.ext.deps["XXPermissions"]) {
        exclude group: 'com.android.support'
    }
}
```

### 7. 更新 demo/build.gradle

修改依赖:

```gradle
dependencies {
    // 旧的方式
    // implementation project(':mvi-core')

    // 新的方式 - 只需依赖 mvi-ui,会自动包含 mvi-core
    implementation project(':mvi-ui')
}
```

---

## 模块依赖关系

```
┌─────────┐
│  demo   │ (示例应用)
│  或 app  │
└────┬────┘
     │
     │ implementation
     │
┌────▼─────┐
│  mvi-ui  │ (UI组件库)
└────┬─────┘
     │
     │ api
     │
┌────▼─────┐
│ mvi-core │ (核心库)
└──────────┘
```

### 依赖说明

- **demo/app** 只需依赖 `mvi-ui`
- **mvi-ui** 通过 `api` 依赖 `mvi-core`
- **mvi-core** 不依赖任何其他模块

### 传递依赖

由于 `mvi-ui` 使用 `api project(':mvi-core')`,所以:
- demo 依赖 mvi-ui
- demo 自动获得 mvi-core 的所有类
- demo 可以直接使用 mvi-core 和 mvi-ui 的所有功能

---

## 使用示例

### 场景一:只需要核心功能(后台服务)

```gradle
// 后台服务应用,不需要 UI
dependencies {
    implementation project(':mvi-core')
}
```

可以使用:
- MviViewModel
- 网络请求
- 本地存储
- 权限管理

不包含:
- 图片加载
- 屏幕适配
- 对话框
- 缺省页

### 场景二:完整的 Android 应用

```gradle
// 普通 Android 应用
dependencies {
    implementation project(':mvi-ui')  // 自动包含 mvi-core
}
```

可以使用:
- mvi-core 的所有功能
- mvi-ui 的所有功能
- 所有 UI 组件和框架

---

## 迁移步骤建议

### 阶段一:准备阶段(推荐立即执行)

1. ✅ 创建 `mvi-ui` 模块
2. ✅ 配置 `build.gradle`
3. ✅ 更新 `settings.gradle`

### 阶段二:代码迁移(逐步进行)

1. 从 `mvi-core` 移动 UI 相关代码到 `mvi-ui`
2. 移动 UI 相关依赖
3. 更新包名和导入语句

### 阶段三:验证测试

1. 编译 `mvi-core` 确保没有错误
2. 编译 `mvi-ui` 确保依赖正确
3. 运行 `demo` 应用测试功能

---

## 方案对比

### 当前方案(单一模块)

```
mvi-core (大而全)
├── 核心架构 ✅
├── 网络请求 ✅
├── 本地存储 ✅
├── UI 组件 ⚠️
├── 图片加载 ⚠️
├── 屏幕适配 ⚠️
└── 对话框等 ⚠️
```

**问题:**
- ❌ 职责混乱(核心+UI混在一起)
- ❌ 依赖臃肿(后台服务也会引入 Glide等)
- ❌ 难以维护(一个模块包含太多内容)

### 推荐方案(双模块)

```
mvi-core (核心)        mvi-ui (UI)
├── MVI 架构           ├── 图片加载
├── 网络请求           ├── 屏幕适配
├── 本地存储           ├── 沉浸式
├── 异常处理           ├── 对话框
└── 权限管理           └── 缺省页
```

**优势:**
- ✅ 职责清晰(核心和UI分离)
- ✅ 按需依赖(后台服务只用core)
- ✅ 易于维护(模块职责单一)
- ✅ 便于扩展(可继续拆分)

---

## 常见问题

### Q1: 为什么不把所有代码都放在 mvi-core?

**A:**
- mvi-core 应该保持纯净,只包含核心架构
- UI 框架(Glide、AutoSize等)会增加包体积
- 后台服务不需要 UI 组件
- 分离后更易于维护和升级

### Q2: 是否需要创建更多模块?

**A:**
- 当前两个模块(mvi-core + mvi-ui)足够
- 如果将来需要,可以再拆分:
  - `mvi-network` - 网络请求
  - `mvi-storage` - 本地存储
  - `mvi-widget` - 自定义View

### Q3: demo 应该依赖哪个模块?

**A:**
- 依赖 `mvi-ui` 即可
- 会自动获得 `mvi-core` 的功能
- 这是最常见的使用方式

### Q4: 已有项目如何迁移?

**A:**
1. 先创建 `mvi-ui` 模块
2. 逐步移动 UI 相关代码
3. 更新 demo 依赖
4. 测试验证功能正常

### Q5: 性能会有影响吗?

**A:**
- ❌ 没有性能影响
- 模块化只是编译时的组织方式
- 运行时和单模块完全一样

---

## 总结

### 推荐做法

**立即执行:**
1. 创建 `mvi-ui` 模块
2. 将 UI 相关依赖移至 `mvi-ui`
3. 保持 `mvi-core` 纯净

**长期目标:**
- mvi-core: 核心架构 + 基础设施
- mvi-ui: UI 组件 + 显示层框架
- 根据需要继续拆分

### 核心原则

✅ **单一职责** - 每个模块职责清晰
✅ **按需依赖** - 只引入需要的功能
✅ **易于维护** - 代码组织清晰
✅ **便于扩展** - 可随时拆分新模块

---

**文档创建时间:** 2025-12-16
**当前版本:** v1.0
