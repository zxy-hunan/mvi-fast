# ExceptionHandle 国际化使用说明

## 概述

ExceptionHandle 现在支持中英文双语错误提示，会根据系统语言自动选择对应的错误信息。

## 初始化

在 Application 的 `onCreate()` 方法中初始化：

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 初始化 ExceptionHandle
        ExceptionHandle.init(this)
    }
}
```

## 使用示例

初始化后，异常处理会自动使用正确的语言：

```kotlin
viewModel.launchRequest(
    onSuccess = { data ->
        // 处理成功
    }
) {
    apiService.getData()
}
```

## 语言支持

### 中文（默认）
- 网络连接失败，请检查网络设置
- 网络连接超时，请稍后重试
- 未授权，请重新登录
- 数据解析失败
- ...

### English
- Network connection failed, please check your network settings
- Network connection timeout, please try again later
- Unauthorized, please login again
- Data parsing failed
- ...

## 添加更多语言

1. 在 `mvi-core/src/main/res/` 目录下创建对应语言的文件夹：
   - `values-zh` - 中文
   - `values-en` - 英文
   - `values-ja` - 日文
   - `values-ko` - 韩文
   - 等等...

2. 在新文件夹中创建 `strings.xml` 并添加翻译

## 注意事项

1. **必须初始化**：在使用前必须调用 `ExceptionHandle.init(context)`
2. **Application Context**：内部使用 ApplicationContext，不会造成内存泄漏
3. **自动切换**：系统语言改变后，错误提示会自动切换
