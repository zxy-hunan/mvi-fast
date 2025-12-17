# 权限处理文档

## 概述

mvi-core 框架集成了 [XXPermissions](https://github.com/getActivity/XXPermissions) 权限请求框架，支持 Android 1-16 所有版本的权限适配，特别针对文件上传下载功能提供了完善的权限处理方案。

## 特性

- ✅ 自动适配 Android 1-16 所有版本
- ✅ 支持存储权限（读写）
- ✅ 支持相机权限
- ✅ 智能版本判断（Android 13+ 使用新的媒体权限）
- ✅ 权限被拒绝时自动引导用户到设置页面
- ✅ 简单易用的 API

## 版本适配说明

### Android 13+ (API 33+)
使用分离的媒体权限：
- `READ_MEDIA_IMAGES` - 读取图片
- `READ_MEDIA_VIDEO` - 读取视频
- `READ_MEDIA_AUDIO` - 读取音频

### Android 11-12 (API 30-32)
使用标准存储权限：
- `READ_EXTERNAL_STORAGE` - 读取外部存储
- `WRITE_EXTERNAL_STORAGE` - 写入外部存储

### Android 10- (API 29-)
使用标准存储权限：
- `READ_EXTERNAL_STORAGE` - 读取外部存储
- `WRITE_EXTERNAL_STORAGE` - 写入外部存储

## 在 AndroidManifest.xml 中声明权限

### mvi-core 已自动配置权限

**mvi-core 模块已在其 AndroidManifest.xml 中声明了所需权限，使用该库的 app 模块会自动继承这些权限声明。**

如果你的项目使用了 mvi-core 库，**无需手动添加权限**，以下权限已自动包含：

```xml
<!-- mvi-core/src/main/AndroidManifest.xml -->
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- 相机权限 -->
    <uses-permission android:name="android.permission.CAMERA" />

    <!-- 存储权限 (Android 12 及以下) -->
    <uses-permission
        android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission
        android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />

    <!-- 媒体权限 (Android 13+) -->
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.READ_MEDIA_VIDEO" />
    <uses-permission android:name="android.permission.READ_MEDIA_AUDIO" />

    <application>
        <!-- 如果项目已适配 Android 10 分区存储 -->
        <meta-data
            android:name="ScopedStorage"
            android:value="true" />
    </application>

</manifest>
```

### 可选权限（需要特殊审核）

如果你的应用确实需要访问所有文件（如文件管理器应用），可以在 **app 模块**的 `AndroidManifest.xml` 中手动添加：

```xml
<!-- 管理外部存储权限 (需要 Google Play 特殊审核) -->
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />
```

**注意**：`MANAGE_EXTERNAL_STORAGE` 权限需要 Google Play 特殊审核，仅在确实需要访问所有文件时使用。

## 使用方法

### 方式一：使用带权限检查的扩展函数（推荐）

这是最简单的方式，上传/下载时自动请求权限。

#### 1. 文件上传（自动权限检查）

```kotlin
class MyViewModel : MviViewModel<MyIntent>() {
    val uploadState = MutableStateFlow<UploadState>(UploadState.Idle)

    fun uploadFile(activity: FragmentActivity, file: File) {
        uploadFileWithPermission(
            activity = activity,
            uploadInfo = UploadInfo(file, "image/jpeg"),
            uploadStateFlow = uploadState,
            onPermissionDenied = {
                // 权限被拒绝时的回调
                showToast("需要存储权限才能上传文件")
            }
        ) { multipartBody ->
            apiService.uploadFile(multipartBody)
        }
    }
}
```

#### 2. 文件下载（自动权限检查）

```kotlin
fun downloadFile(activity: FragmentActivity, url: String, savePath: String) {
    downloadFileWithPermission(
        activity = activity,
        url = url,
        savePath = savePath,
        supportResume = true,
        downloadStateFlow = downloadState,
        onPermissionDenied = {
            showToast("需要存储权限才能下载文件")
        }
    )
}
```

#### 3. 拍照并上传（自动检查相机和存储权限）

```kotlin
fun takePictureAndUpload(activity: FragmentActivity, file: File) {
    takePictureAndUploadWithPermission(
        activity = activity,
        uploadInfo = UploadInfo(file, "image/jpeg"),
        uploadStateFlow = uploadState,
        onPermissionDenied = {
            showToast("需要相机和存储权限")
        }
    ) { multipartBody ->
        apiService.uploadFile(multipartBody)
    }
}
```

### 方式二：手动检查权限（更灵活）

如果需要更精细的控制，可以手动检查和请求权限。

#### 1. 检查权限是否已授予

```kotlin
// 检查存储权限
if (FilePermissionHelper.hasStoragePermission(activity)) {
    // 权限已授予，直接执行操作
    uploadFile(file)
} else {
    // 请求权限
    requestStoragePermission(activity)
}

// 检查相机权限
if (FilePermissionHelper.hasCameraPermission(activity)) {
    // 权限已授予
}
```

#### 2. 请求存储权限

```kotlin
FilePermissionHelper.requestStoragePermission(activity) { granted ->
    if (granted) {
        // 权限授予成功，执行文件操作
        uploadFile(file)
    } else {
        // 权限被拒绝
        showToast("需要存储权限")
    }
}
```

#### 3. 请求相机权限

```kotlin
FilePermissionHelper.requestCameraPermission(activity) { granted ->
    if (granted) {
        // 权限授予成功，打开相机
        openCamera()
    } else {
        showToast("需要相机权限")
    }
}
```

#### 4. 同时请求相机和存储权限

```kotlin
FilePermissionHelper.requestCameraAndStoragePermission(activity) { granted ->
    if (granted) {
        // 所有权限都授予成功
        takePicture()
    } else {
        showToast("需要相机和存储权限")
    }
}
```

#### 5. 引导用户到设置页面

```kotlin
// 当权限被永久拒绝时，引导用户到设置页面手动授权
FilePermissionHelper.startPermissionActivity(activity)
```

## 完整示例

### ViewModel 示例

```kotlin
class FileViewModel : MviViewModel<FileIntent>() {

    val uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)

    override fun handleIntent(intent: FileIntent) {
        when (intent) {
            is FileIntent.UploadFile -> handleUploadFile(intent.activity, intent.file)
            is FileIntent.DownloadFile -> handleDownloadFile(intent.activity, intent.url, intent.savePath)
        }
    }

    private fun handleUploadFile(activity: FragmentActivity, file: File) {
        uploadFileWithPermission(
            activity = activity,
            uploadInfo = UploadInfo(file, "image/jpeg"),
            uploadStateFlow = uploadState,
            onPermissionDenied = {
                showToast("需要存储权限")
            }
        ) { multipartBody ->
            apiService.uploadFile(multipartBody)
        }
    }

    private fun handleDownloadFile(activity: FragmentActivity, url: String, savePath: String) {
        downloadFileWithPermission(
            activity = activity,
            url = url,
            savePath = savePath,
            downloadStateFlow = downloadState,
            onPermissionDenied = {
                showToast("需要存储权限")
            }
        )
    }
}
```

### Activity/Fragment 示例

```kotlin
class FileActivity : MviActivity<FileIntent>() {

    private val viewModel: FileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 观察上传状态
        lifecycleScope.launch {
            viewModel.uploadState.collect { state ->
                when (state) {
                    is UploadState.Uploading -> {
                        updateProgress(state.progress)
                    }
                    is UploadState.Success -> {
                        showToast("上传成功")
                    }
                    is UploadState.Error -> {
                        showToast(state.message)
                    }
                    else -> {}
                }
            }
        }

        // 上传文件（自动处理权限）
        binding.btnUpload.setOnClickListener {
            val file = File("/path/to/file")
            viewModel.sendIntent(FileIntent.UploadFile(this, file))
        }

        // 下载文件（自动处理权限）
        binding.btnDownload.setOnClickListener {
            viewModel.sendIntent(
                FileIntent.DownloadFile(
                    activity = this,
                    url = "https://example.com/file.apk",
                    savePath = getExternalFilesDir(null)?.absolutePath + "/file.apk"
                )
            )
        }
    }
}
```

## 最佳实践

### 1. 优先使用带权限检查的扩展函数

```kotlin
// ✅ 推荐
uploadFileWithPermission(...)

// ❌ 不推荐（需要手动处理权限）
uploadFile(...)
```

### 2. 提供友好的权限说明

在请求权限前，向用户解释为什么需要该权限。

```kotlin
// 可以在请求权限前显示一个说明对话框
showPermissionRationaleDialog()
FilePermissionHelper.requestStoragePermission(activity) { ... }
```

### 3. 处理权限被拒绝的情况

```kotlin
onPermissionDenied = {
    // 提供明确的提示
    showToast("需要存储权限才能上传文件")

    // 或者显示一个对话框，引导用户到设置页面
    showPermissionDeniedDialog()
}
```

### 4. 使用分区存储（Android 10+）

优先使用 App 专属目录，不需要权限：

```kotlin
// ✅ 推荐：使用 App 专属目录（不需要权限）
val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "file.apk")

// ❌ 不推荐：使用公共目录（需要权限）
val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "file.apk")
```

### 5. 避免使用 MANAGE_EXTERNAL_STORAGE

`MANAGE_EXTERNAL_STORAGE` 需要 Google Play 特殊审核，仅在确实需要访问所有文件时使用。

### 6. 测试不同权限状态

- 权限未授予
- 权限已授予
- 权限被永久拒绝

## 常见问题

### Q1: 如何处理权限被永久拒绝的情况？

A: `FilePermissionHelper` 会自动检测权限是否被永久拒绝，并显示对话框引导用户到设置页面手动授权。

### Q2: Android 13+ 为什么需要三个媒体权限？

A: Android 13 引入了细粒度的媒体权限，分别控制图片、视频和音频的访问。

### Q3: 如何判断当前 Android 版本需要哪些权限？

A: `FilePermissionHelper.getStoragePermissions()` 会自动根据 Android 版本返回对应的权限列表。

### Q4: 是否必须在 AndroidManifest.xml 中声明所有权限？

A: 是的，即使框架会自动适配版本，所有可能用到的权限都需要在 Manifest 中声明。

### Q5: 使用 XXPermissions 有什么优势？

A: XXPermissions 是第一个适配 Android 16 的权限框架，自动处理各版本差异，使用简单，无需担心兼容性问题。

## 相关资源

- **XXPermissions GitHub**: https://github.com/getActivity/XXPermissions
- **XXPermissions 文档**: https://github.com/getActivity/XXPermissions/blob/master/README-en.md
- **Android 官方权限指南**: https://developer.android.com/guide/topics/permissions/overview
- **FilePermissionHelper.kt**: [mvi-core/permission/FilePermissionHelper.kt](src/main/java/com/mvi/core/permission/FilePermissionHelper.kt)
- **FilePermissionExt.kt**: [mvi-core/ext/FilePermissionExt.kt](src/main/java/com/mvi/core/ext/FilePermissionExt.kt)
- **FilePermissionExample.kt**: [mvi-core/example/FilePermissionExample.kt](src/main/java/com/mvi/core/example/FilePermissionExample.kt)

## Sources

Based on web search results:
- [GitHub - getActivity/XXPermissions: Android Permissions Framework, Adapt to Android 16](https://github.com/getActivity/XXPermissions)
- [XXPermissions/README-en.md at master · getActivity/XXPermissions](https://github.com/getActivity/XXPermissions/blob/master/README-en.md)
