# 📁 aFramework 项目结构

```
E:\soft\aFramework/
│
├── 📄 build.gradle                    # 项目根配置
├── 📄 settings.gradle                 # 模块配置
├── 📄 config.gradle                   # 依赖版本管理
├── 📄 README.md                       # 项目说明文档
├── 📄 ARCHITECTURE.md                 # 架构设计文档
├── 📄 QUICKSTART.md                   # 快速入门指南
│
├── 📂 mvi-core/                       # 核心框架模块 ⭐
│   ├── 📄 build.gradle
│   ├── 📄 proguard-rules.pro
│   └── src/main/
│       └── java/com/mvi/core/
│           │
│           ├── 📂 base/               # MVI基础组件
│           │   ├── MviIntent.kt       # Intent标记接口
│           │   ├── UiState.kt         # UI状态封装
│           │   ├── MviViewModel.kt    # ViewModel基类
│           │   ├── MviActivity.kt     # Activity基类
│           │   └── MviFragment.kt     # Fragment基类
│           │
│           ├── 📂 network/            # 网络层
│           │   ├── ApiResponse.kt     # API响应封装
│           │   └── RetrofitClient.kt  # Retrofit客户端
│           │
│           ├── 📂 storage/            # 存储层
│           │   └── MmkvStorage.kt     # MMKV封装
│           │
│           └── 📂 ext/                # 扩展函数
│               ├── NetworkExt.kt      # 网络请求扩展
│               ├── FlowExt.kt         # Flow扩展
│               └── ViewExt.kt         # View扩展
│
└── 📂 demo/                           # 示例应用模块 📱
    ├── 📄 build.gradle
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/mvi/demo/
        │   │
        │   ├── 📂 api/                # API接口层
        │   │   └── DemoApi.kt
        │   │
        │   └── 📂 ui/                 # UI层
        │       ├── DemoIntent.kt      # Intent定义
        │       ├── DemoModel.kt       # 数据模型
        │       ├── DemoViewModel.kt   # ViewModel
        │       └── DemoActivity.kt    # Activity
        │
        └── res/
            └── layout/
                └── activity_demo.xml  # 布局文件
```

---

## 📊 文件统计

### 代码文件
- **Kotlin文件**: 18个
- **XML文件**: 2个
- **Gradle文件**: 4个
- **文档文件**: 3个

### 核心模块 (mvi-core)
```
base/        5个文件  ~300行代码
network/     2个文件  ~150行代码
storage/     1个文件  ~180行代码
ext/         3个文件  ~120行代码
────────────────────────────────
总计:       11个文件  ~750行代码
```

### 示例模块 (demo)
```
ui/          4个文件  ~200行代码
api/         1个文件  ~50行代码
res/         1个文件  ~50行XML
────────────────────────────────
总计:        6个文件  ~300行代码
```

---

## 🎯 核心文件说明

### 1. 框架基础 (mvi-core/base/)

| 文件 | 行数 | 职责 |
|------|------|------|
| MviIntent.kt | ~5 | Intent标记接口 |
| UiState.kt | ~25 | 状态和事件封装 |
| MviViewModel.kt | ~60 | ViewModel基类,处理Intent |
| MviActivity.kt | ~80 | Activity基类,生命周期管理 |
| MviFragment.kt | ~75 | Fragment基类 |

### 2. 网络层 (mvi-core/network/)

| 文件 | 行数 | 职责 |
|------|------|------|
| ApiResponse.kt | ~30 | API响应模型 |
| RetrofitClient.kt | ~120 | Retrofit配置和初始化 |

### 3. 存储层 (mvi-core/storage/)

| 文件 | 行数 | 职责 |
|------|------|------|
| MmkvStorage.kt | ~180 | MMKV封装,支持委托属性 |

### 4. 扩展函数 (mvi-core/ext/)

| 文件 | 行数 | 职责 |
|------|------|------|
| NetworkExt.kt | ~90 | 网络请求DSL扩展 |
| FlowExt.kt | ~30 | Flow生命周期扩展 |
| ViewExt.kt | ~40 | View便捷扩展 |

### 5. Demo示例 (demo/)

| 文件 | 行数 | 职责 |
|------|------|------|
| DemoIntent.kt | ~12 | 示例Intent定义 |
| DemoModel.kt | ~15 | 示例数据模型 |
| DemoViewModel.kt | ~80 | 示例ViewModel实现 |
| DemoActivity.kt | ~90 | 示例Activity实现 |
| DemoApi.kt | ~50 | 示例API接口 |

---

## 🔑 关键设计决策

### 1. 模块划分

- **mvi-core**: 独立的框架模块,可单独发布
- **demo**: 完整的使用示例,可作为模板

### 2. 包结构

```
com.mvi.core
├── base      # 基础抽象,所有项目必用
├── network   # 网络层,可选
├── storage   # 存储层,可选
└── ext       # 扩展函数,按需使用
```

### 3. 依赖管理

- **config.gradle**: 统一管理版本号
- **按需依赖**: 核心模块最小化依赖
- **版本对齐**: 使用BOM统一版本

---

## 📦 模块依赖关系

```
demo (应用模块)
  └─> mvi-core (框架模块)
        ├─> Kotlin stdlib
        ├─> AndroidX (lifecycle, viewmodel)
        ├─> Coroutines
        ├─> Retrofit + OkHttp
        ├─> MMKV
        └─> Glide
```

---

## 🚀 如何使用

### 新项目集成

1. 复制 `mvi-core` 模块到你的项目
2. 在 `settings.gradle` 添加: `include ':mvi-core'`
3. 在 app 模块添加依赖: `implementation project(':mvi-core')`
4. 参考 `demo` 模块编写代码

### 现有项目迁移

1. 参考 `ARCHITECTURE.md` 了解设计差异
2. 逐步将现有页面迁移到新架构
3. 可以新旧架构共存

---

## 📚 文档导航

| 文档 | 内容 | 适合人群 |
|------|------|----------|
| [README.md](README.md) | 完整功能说明 | 所有人 |
| [QUICKSTART.md](QUICKSTART.md) | 5分钟快速入门 | 新手 |
| [ARCHITECTURE.md](ARCHITECTURE.md) | 架构设计对比 | 架构师 |

---

## 💡 特色亮点

✅ **极简设计** - 核心代码仅750行
✅ **完整文档** - 3份完整文档覆盖所有场景
✅ **可运行Demo** - 开箱即用的示例代码
✅ **生产就绪** - 包含ProGuard配置
✅ **类型安全** - 完整的泛型和sealed class

---

**创建时间**: 2024-12-12
**框架版本**: v1.0.0
**最低SDK**: 23 (Android 6.0)
**目标SDK**: 35 (Android 15)
