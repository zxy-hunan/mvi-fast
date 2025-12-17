# 文件上传下载功能

mvi-core 框架现已支持文件上传和下载功能，包括：

- ✅ 单文件/多文件上传
- ✅ 上传进度监听
- ✅ 文件下载
- ✅ 断点续传下载
- ✅ 下载进度监听
- ✅ 中英文国际化支持

## 目录结构

```
mvi-core/
├── network/
│   ├── upload/
│   │   ├── UploadRequestBody.kt      # 支持进度监听的RequestBody
│   │   ├── UploadInfo.kt             # 上传文件信息和状态
│   ├── download/
│   │   ├── DownloadManager.kt        # 文件下载管理器
│   │   ├── DownloadState.kt          # 下载状态
├── ext/
│   └── FileExt.kt                    # 上传下载扩展函数
└── example/
    └── FileUploadDownloadExample.kt  # 使用示例
```

## 使用方法

### 1. 文件上传

#### 1.1 单文件上传

```kotlin
class MyViewModel : MviViewModel<MyIntent>() {
    val uploadState = MutableStateFlow<UploadState>(UploadState.Idle)

    fun uploadFile(file: File) {
        uploadFile(
            uploadInfo = UploadInfo(
                file = file,
                contentType = "image/jpeg",
                fileName = file.name,
                formFieldName = "file"
            ),
            uploadStateFlow = uploadState
        ) { multipartBody ->
            apiService.uploadFile(multipartBody)
        }
    }
}
```

#### 1.2 多文件上传

```kotlin
fun uploadMultipleFiles(files: List<File>) {
    val uploadInfoList = files.map { file ->
        UploadInfo(
            file = file,
            contentType = "image/jpeg",
            fileName = file.name,
            formFieldName = "files"
        )
    }

    uploadFiles(
        uploadInfoList = uploadInfoList,
        uploadStateFlow = uploadState
    ) { parts ->
        apiService.uploadMultipleFiles(parts)
    }
}
```

#### 1.3 监听上传状态

```kotlin
lifecycleScope.launch {
    viewModel.uploadState.collect { state ->
        when (state) {
            is UploadState.Idle -> {
                // 空闲状态
            }
            is UploadState.Uploading -> {
                // 更新进度
                val progress = state.progress // 0-100
                val bytesWritten = state.bytesWritten
                val totalBytes = state.totalBytes
                updateProgress(progress)
            }
            is UploadState.Success -> {
                // 上传成功
                showToast("Upload successful")
            }
            is UploadState.Error -> {
                // 上传失败
                showToast(state.message)
            }
        }
    }
}
```

### 2. 文件下载

#### 2.1 下载文件（支持断点续传）

```kotlin
class MyViewModel : MviViewModel<MyIntent>() {
    val downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)

    fun downloadFile(url: String, savePath: String) {
        downloadFile(
            url = url,
            savePath = savePath,
            supportResume = true, // 是否支持断点续传
            downloadStateFlow = downloadState
        )
    }
}
```

#### 2.2 监听下载状态

```kotlin
lifecycleScope.launch {
    viewModel.downloadState.collect { state ->
        when (state) {
            is DownloadState.Idle -> {
                // 空闲状态
            }
            is DownloadState.Downloading -> {
                // 更新进度
                val progress = state.progress // 0-100
                val downloaded = state.bytesDownloaded
                val total = state.totalBytes
                updateProgress(progress)
            }
            is DownloadState.Success -> {
                // 下载成功
                val file = state.file
                showToast("Download completed")
                // 可以进行后续操作，如安装APK
                installApk(file)
            }
            is DownloadState.Error -> {
                // 下载失败
                showToast(state.message)
            }
        }
    }
}
```

#### 2.3 取消下载

```kotlin
fun cancelDownload(savePath: String) {
    cancelDownload(savePath)
}
```

### 3. API Service 定义

在 Retrofit 中定义上传和下载接口：

```kotlin
interface FileApiService {

    // 单文件上传
    @Multipart
    @POST("upload/single")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part
    ): ApiResponse<UploadResponse>

    // 多文件上传
    @Multipart
    @POST("upload/multiple")
    suspend fun uploadMultipleFiles(
        @Part files: List<MultipartBody.Part>
    ): ApiResponse<UploadResponse>

    // 文件下载（如果需要通过Retrofit）
    @Streaming
    @GET
    suspend fun downloadFile(
        @Url url: String
    ): Response<ResponseBody>
}
```

### 4. 完整使用示例

详细的使用示例请查看：
- [FileUploadDownloadExample.kt](src/main/java/com/mvi/core/example/FileUploadDownloadExample.kt)

## 核心类说明

### UploadInfo

上传文件信息配置：

```kotlin
data class UploadInfo(
    val file: File,                              // 要上传的文件
    val contentType: String = "application/octet-stream", // MIME类型
    val fileName: String = file.name,             // 文件名
    val formFieldName: String = "file"            // 表单字段名
)
```

### UploadState

上传状态：

```kotlin
sealed class UploadState {
    object Idle : UploadState()                  // 空闲
    data class Uploading(                        // 上传中
        val progress: Int,                        // 进度 0-100
        val bytesWritten: Long,                   // 已上传字节
        val totalBytes: Long                      // 总字节数
    ) : UploadState()
    data class Success<T>(val data: T) : UploadState()  // 成功
    data class Error(                            // 失败
        val message: String,
        val throwable: Throwable? = null
    ) : UploadState()
}
```

### DownloadState

下载状态：

```kotlin
sealed class DownloadState {
    object Idle : DownloadState()                // 空闲
    data class Downloading(                      // 下载中
        val progress: Int,                        // 进度 0-100
        val bytesDownloaded: Long,                // 已下载字节
        val totalBytes: Long                      // 总字节数
    ) : DownloadState()
    data class Success(val file: File) : DownloadState()  // 成功
    data class Error(                            // 失败
        val message: String,
        val throwable: Throwable? = null
    ) : DownloadState()
}
```

## 常见文件MIME类型

```kotlin
// 图片
"image/jpeg"     // .jpg, .jpeg
"image/png"      // .png
"image/gif"      // .gif
"image/webp"     // .webp

// 视频
"video/mp4"      // .mp4
"video/mpeg"     // .mpeg

// 文档
"application/pdf"              // .pdf
"application/msword"           // .doc
"application/vnd.openxmlformats-officedocument.wordprocessingml.document" // .docx

// 压缩文件
"application/zip"              // .zip
"application/x-rar-compressed" // .rar

// APK
"application/vnd.android.package-archive" // .apk

// 通用
"application/octet-stream"     // 二进制文件
```

## 注意事项

1. **权限要求**：
   - 上传：需要读取文件权限
   - 下载：需要写入存储权限
   - Android 10+ 需要适配分区存储

2. **断点续传**：
   - 服务器需要支持 `Range` 请求头
   - 下载会在临时文件基础上续传

3. **大文件上传**：
   - 建议对大文件进行分片上传
   - 可以配合进度回调显示上传进度

4. **网络超时**：
   - 上传下载大文件时，建议增加超时时间
   - 在 `RetrofitClient` 初始化时配置

5. **内存优化**：
   - 下载使用流式写入，不会占用过多内存
   - 上传使用分块读取，内存占用可控

## 国际化支持

框架已内置中英文支持：

**中文**：
- upload_progress: "上传中 %d%%"
- upload_success: "上传成功"
- upload_failed: "上传失败"
- download_progress: "下载中 %d%%"
- download_success: "下载完成"
- download_failed: "下载失败"

**English**：
- upload_progress: "Uploading %d%%"
- upload_success: "Upload successful"
- upload_failed: "Upload failed"
- download_progress: "Downloading %d%%"
- download_success: "Download completed"
- download_failed: "Download failed"

## 更新日志

### v1.1.0
- ✅ 新增文件上传功能（单文件/多文件）
- ✅ 新增文件下载功能（支持断点续传）
- ✅ 新增上传下载进度监听
- ✅ 新增中英文国际化支持
