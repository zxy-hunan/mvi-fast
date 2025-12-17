package com.mvi.core.network.upload

import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.File
import java.io.FileInputStream

/**
 * 支持上传进度监听的RequestBody
 *
 * 使用示例:
 * ```kotlin
 * val file = File("/path/to/file")
 * val requestBody = UploadRequestBody(file, "image/jpeg") { progress, total ->
 *     // 更新进度
 *     val percent = (progress * 100 / total).toInt()
 *     updateProgress(percent)
 * }
 * ```
 */
class UploadRequestBody(
    private val file: File,
    private val contentType: String,
    private val onProgress: ((bytesWritten: Long, totalBytes: Long) -> Unit)? = null
) : RequestBody() {

    override fun contentType(): MediaType? {
        return contentType.toMediaTypeOrNull()
    }

    override fun contentLength(): Long {
        return file.length()
    }

    override fun writeTo(sink: BufferedSink) {
        val fileLength = file.length()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var uploaded = 0L

        FileInputStream(file).use { inputStream ->
            var read: Int
            while (inputStream.read(buffer).also { read = it } != -1) {
                uploaded += read
                sink.write(buffer, 0, read)

                // 回调进度
                onProgress?.invoke(uploaded, fileLength)
            }
        }
    }

    companion object {
        private const val DEFAULT_BUFFER_SIZE = 8192
    }
}
