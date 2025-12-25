package com.mvi.core.network.download

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

/**
 * 文件下载管理器
 *
 * 使用示例:
 * ```kotlin
 * val downloadManager = DownloadManager()
 *
 * // 下载文件
 * downloadManager.download(
 *     url = "https://example.com/file.apk",
 *     savePath = "/sdcard/Download/file.apk"
 * ).collect { state ->
 *     when (state) {
 *         is DownloadState.Downloading -> {
 *             updateProgress(state.progress)
 *         }
 *         is DownloadState.Success -> {
 *             showToast("Download completed")
 *         }
 *         is DownloadState.Error -> {
 *             showToast(state.message)
 *         }
 *     }
 * }
 * 
 * // 在不使用时释放资源
 * downloadManager.release()
 * ```
 */
class DownloadManager(
    private val okHttpClient: OkHttpClient = OkHttpClient()
) {

    /**
     * 下载文件
     *
     * @param url 下载链接
     * @param savePath 保存路径
     * @param supportResume 是否支持断点续传（默认true）
     * @return Flow<DownloadState> 下载状态流
     */
    fun download(
        url: String,
        savePath: String,
        supportResume: Boolean = true
    ): Flow<DownloadState> = flow {
        try {
            emit(DownloadState.Idle)

            val saveFile = File(savePath)
            val parentDir = saveFile.parentFile
            if (parentDir?.exists() == false) {
                parentDir.mkdirs()
            }

            // 检查是否支持断点续传
            var downloadedLength = 0L
            if (supportResume && saveFile.exists()) {
                downloadedLength = saveFile.length()
            }

            // 构建请求
            val requestBuilder = Request.Builder().url(url)
            if (supportResume && downloadedLength > 0) {
                requestBuilder.addHeader("Range", "bytes=$downloadedLength-")
            }

            // 执行下载
            val response = okHttpClient.newCall(requestBuilder.build()).execute()

            if (!response.isSuccessful) {
                emit(DownloadState.Error("Download failed: ${response.code}"))
                return@flow
            }

            val body = response.body ?: run {
                emit(DownloadState.Error("Response body is null"))
                return@flow
            }

            val contentLength = body.contentLength()
            val totalLength = contentLength + downloadedLength

            // 写入文件
            body.byteStream().use { inputStream ->
                FileOutputStream(saveFile, supportResume).use { outputStream ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var bytesRead: Int
                    var totalBytesRead = downloadedLength

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        // 计算进度
                        val progress = if (totalLength > 0) {
                            ((totalBytesRead * 100) / totalLength).toInt()
                        } else {
                            0
                        }

                        emit(
                            DownloadState.Downloading(
                                progress = progress,
                                bytesDownloaded = totalBytesRead,
                                totalBytes = totalLength
                            )
                        )
                    }
                }
            }

            emit(DownloadState.Success(saveFile))
        } catch (e: Exception) {
            emit(DownloadState.Error(e.message ?: "Download error", e))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 取消下载（删除临时文件）
     */
    fun cancelDownload(savePath: String) {
        val file = File(savePath)
        if (file.exists()) {
            file.delete()
        }
    }
    
    /**
     * 释放资源 - 防止内存泄漏
     * 在不再使用时调用，关闭 OkHttpClient 的连接池和线程池
     */
    fun release() {
        try {
            okHttpClient.dispatcher.executorService.shutdown()
            okHttpClient.connectionPool.evictAll()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 8192
    }
}
