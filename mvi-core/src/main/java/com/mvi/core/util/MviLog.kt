package com.mvi.core.util

import com.mvi.core.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.concurrent.thread

/**
 * 统一的日志管理工具
 *
 * 功能特性:
 * 1. 统一的日志接口
 * 2. 可配置的日志级别
 * 3. Release 版本自动关闭
 * 4. 线程信息显示
 * 5. 堆栈跟踪
 * 6. 网络请求日志
 * 7. 异常日志
 * 8. 文件日志（可选）
 *
 * 使用示例:
 * ```kotlin
 * // 在 Application 中初始化
 * MviLog.init(level = MviLog.Level.DEBUG)
 *
 * // 使用扩展函数
 * viewModel.logD("Loading users...")
 * viewModel.logE("Failed to load users", exception)
 *
 * // 使用静态方法
 * MviLog.d("Tag", "Debug message")
 * MviLog.networkRequest(url, method, params)
 * ```
 */
object MviLog {

    // ==================== 日志级别 ====================

    /**
     * 日志级别
     */
    enum class Level {
        VERBOSE, DEBUG, INFO, WARN, ERROR, NONE
    }

    // ==================== 配置项 ====================

    /**
     * 当前日志级别
     */
    var logLevel: Level = if (BuildConfig.DEBUG) Level.DEBUG else Level.ERROR
        private set

    /**
     * 是否打印日志
     */
    var isLogEnabled: Boolean = BuildConfig.DEBUG
        private set

    /**
     * 标签前缀
     */
    private const val TAG_PREFIX = "MVI_"

    /**
     * 是否显示线程信息
     */
    var showThreadInfo: Boolean = BuildConfig.DEBUG

    /**
     * 是否显示堆栈跟踪
     */
    var showStackTrace: Boolean = false

    /**
     * 日志写入器列表
     */
    private val logWriters = mutableListOf<LogWriter>()

    // ==================== 初始化 ====================

    /**
     * 初始化（在 Application 中调用）
     *
     * @param level 日志级别
     * @param enabled 是否启用日志
     */
    fun init(
        level: Level = if (BuildConfig.DEBUG) Level.DEBUG else Level.ERROR,
        enabled: Boolean = BuildConfig.DEBUG
    ) {
        this.logLevel = level
        this.isLogEnabled = enabled
    }

    /**
     * 添加日志写入器
     */
    fun addLogWriter(writer: LogWriter) {
        logWriters.add(writer)
    }

    /**
     * 清空日志写入器
     */
    fun clearLogWriters() {
        logWriters.clear()
    }

    // ==================== 基础日志方法 ====================

    /**
     * VERBOSE 级别日志
     */
    fun v(tag: String, message: String) {
        log(Level.VERBOSE, tag, message)
    }

    /**
     * DEBUG 级别日志
     */
    fun d(tag: String, message: String) {
        log(Level.DEBUG, tag, message)
    }

    /**
     * INFO 级别日志
     */
    fun i(tag: String, message: String) {
        log(Level.INFO, tag, message)
    }

    /**
     * WARN 级别日志
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.WARN, tag, message, throwable)
    }

    /**
     * ERROR 级别日志
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log(Level.ERROR, tag, message, throwable)
    }

    // ==================== 核心日志方法 ====================

    /**
     * 核心日志方法
     */
    private fun log(
        level: Level,
        tag: String,
        message: String,
        throwable: Throwable? = null
    ) {
        // 检查是否启用日志
        if (!isLogEnabled) return

        // 检查日志级别
        if (level.ordinal < logLevel.ordinal) return

        // 构建日志消息
        val logMessage = buildMessage(message, throwable)

        // 打印到 Logcat
        val fullTag = TAG_PREFIX + tag
        when (level) {
            Level.VERBOSE -> android.util.Log.v(fullTag, logMessage, throwable)
            Level.DEBUG -> android.util.Log.d(fullTag, logMessage, throwable)
            Level.INFO -> android.util.Log.i(fullTag, logMessage, throwable)
            Level.WARN -> android.util.Log.w(fullTag, logMessage, throwable)
            Level.ERROR -> android.util.Log.e(fullTag, logMessage, throwable)
            Level.NONE -> {}
        }

        // 写入文件
        logWriters.forEach { writer ->
            try {
                writer.write(level, tag, logMessage)
            } catch (e: Exception) {
                // 避免日志写入失败影响主逻辑
                android.util.Log.e("MviLog", "Failed to write log", e)
            }
        }
    }

    /**
     * 构建格式化的日志消息
     */
    private fun buildMessage(message: String, throwable: Throwable?): String {
        val sb = StringBuilder()

        // 添加线程信息
        if (showThreadInfo) {
            val thread = Thread.currentThread()
            sb.append("[${thread.name}] ")
        }

        // 添加消息
        sb.append(message)

        // 添加异常信息
        if (throwable != null) {
            sb.append(" | Exception: ")
                .append(throwable.javaClass.simpleName)
                .append(": ")
                .append(throwable.message)
        }

        // 添加堆栈跟踪
        if (showStackTrace) {
            sb.append("\n")
                .append(getStackTrace(5)) // 跳过前5层堆栈
        }

        return sb.toString()
    }

    /**
     * 获取堆栈跟踪信息
     */
    private fun getStackTrace(skipFrames: Int): String {
        val stackTrace = Thread.currentThread().stackTrace
        val sb = StringBuilder()

        val start = skipFrames.coerceAtMost(stackTrace.size)
        val end = (start + 5).coerceAtMost(stackTrace.size)

        for (i in start until end) {
            val element = stackTrace[i]
            sb.append("\tat ")
                .append(element.className)
                .append(".")
                .append(element.methodName)
                .append("(")
                .append(element.fileName)
                .append(":")
                .append(element.lineNumber)
                .append(")\n")
        }

        return sb.toString()
    }

    // ==================== 特殊类型日志 ====================

    /**
     * 记录网络请求
     *
     * @param url 请求URL
     * @param method 请求方法
     * @param params 请求参数
     * @param headers 请求头
     */
    fun networkRequest(
        url: String,
        method: String = "GET",
        params: Map<String, Any>? = null,
        headers: Map<String, String>? = null
    ) {
        val sb = StringBuilder("Network Request:\n")
            .append("  Method: ")
            .append(method)
            .append("\n  URL: ")
            .append(url)

        if (params != null && params.isNotEmpty()) {
            sb.append("\n  Params: ")
                .append(params.entries.joinToString(", ") { "${it.key}=${it.value}" })
        }

        if (headers != null && headers.isNotEmpty()) {
            sb.append("\n  Headers: ")
                .append(headers.entries.joinToString(", ") { "${it.key}=${it.value}" })
        }

        d("Network", sb.toString())
    }

    /**
     * 记录网络响应
     *
     * @param url 请求URL
     * @param code 响应码
     * @param message 响应消息
     * @param duration 请求耗时(ms)
     */
    fun networkResponse(
        url: String,
        code: Int,
        message: String? = null,
        duration: Long
    ) {
        val sb = StringBuilder("Network Response:\n")
            .append("  URL: ")
            .append(url)
            .append("\n  Code: ")
            .append(code)
            .append("\n  Duration: ")
            .append(duration)
            .append("ms")

        if (message != null) {
            sb.append("\n  Message: ")
                .append(message)
        }

        d("Network", sb.toString())
    }

    /**
     * 记录异常
     *
     * @param throwable 异常对象
     * @param message 附加消息
     */
    fun exception(throwable: Throwable, message: String? = null) {
        val sb = StringBuilder("Exception:\n")
            .append("  Type: ")
            .append(throwable.javaClass.simpleName)

        if (message != null) {
            sb.append("\n  Message: ")
                .append(message)
        }

        sb.append("\n  StackTrace: ")
            .append(android.util.Log.getStackTraceString(throwable))

        e("Exception", sb.toString())
    }

    /**
     * 记录方法进入
     */
    fun enterMethod() {
        if (!isLogEnabled || logLevel > Level.DEBUG) return

        val stackTrace = Thread.currentThread().stackTrace
        if (stackTrace.size > 3) {
            val element = stackTrace[3]
            val methodName = element.methodName
            val className = element.className.substringAfterLast('.')
            d("Method", "→ Enter: $className.$methodName")
        }
    }

    /**
     * 记录方法退出
     */
    fun exitMethod() {
        if (!isLogEnabled || logLevel > Level.DEBUG) return

        val stackTrace = Thread.currentThread().stackTrace
        if (stackTrace.size > 3) {
            val element = stackTrace[3]
            val methodName = element.methodName
            val className = element.className.substringAfterLast('.')
            d("Method", "← Exit: $className.$methodName")
        }
    }

    // ==================== 扩展函数 ====================

    /**
     * Any 扩展函数 - VERBOSE
     */
    fun Any.logV(message: String) {
        v(this::class.java.simpleName, message)
    }

    /**
     * Any 扩展函数 - DEBUG
     */
    fun Any.logD(message: String) {
        d(this::class.java.simpleName, message)
    }

    /**
     * Any 扩展函数 - INFO
     */
    fun Any.logI(message: String) {
        i(this::class.java.simpleName, message)
    }

    /**
     * Any 扩展函数 - WARN
     */
    fun Any.logW(message: String, throwable: Throwable? = null) {
        w(this::class.java.simpleName, message, throwable)
    }

    /**
     * Any 扩展函数 - ERROR
     */
    fun Any.logE(message: String, throwable: Throwable? = null) {
        e(this::class.java.simpleName, message, throwable)
    }

    // ==================== 日志写入器接口 ====================

    /**
     * 日志写入器接口
     * 用于扩展日志输出（如写入文件、上传服务器等）
     */
    interface LogWriter {
        /**
         * 写入日志
         *
         * @param level 日志级别
         * @param tag 标签
         * @param message 消息
         */
        fun write(level: Level, tag: String, message: String)
    }

    /**
     * 文件日志写入器
     * 将日志写入本地文件
     */
    class FileLogWriter(
        private val logDir: String,
        private val maxFileSize: Long = 5 * 1024 * 1024, // 5MB
        private val maxFiles: Int = 10 // 最多保留10个日志文件
    ) : LogWriter {

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        private var currentLogFile: File? = null

        override fun write(level: Level, tag: String, message: String) {
            try {
                val logFile = getLogFile()
                val timestamp = dateFormat.format(Date())
                val logLine = "$timestamp [${level.name}] $tag: $message\n"

                // 写入文件
                java.io.FileOutputStream(logFile, true).buffered().use { output ->
                    output.write(logLine.toByteArray())
                }

                // 检查文件大小
                if (logFile.length() > maxFileSize) {
                    compressLog(logFile)
                    cleanOldLogs()
                }
            } catch (e: Exception) {
                android.util.Log.e("FileLogWriter", "Failed to write log", e)
            }
        }

        /**
         * 获取当前日志文件
         */
        private fun getLogFile(): File {
            if (currentLogFile == null || !currentLogFile?.exists()!!) {
                val logDir = File(logDir)
                if (!logDir.exists()) {
                    logDir.mkdirs()
                }
                currentLogFile = File(logDir, "mvi_log_${System.currentTimeMillis()}.txt")
            }
            return currentLogFile!!
        }

        /**
         * 压缩日志文件
         */
        private fun compressLog(file: File) {
            try {
                val zipFile = File(file.parent, "${file.nameWithoutExtension}.zip")
                java.util.zip.ZipOutputStream(java.io.FileOutputStream(zipFile)).use { zos ->
                    val entry = java.util.zip.ZipEntry(file.name)
                    zos.putNextEntry(entry)
                    java.io.FileInputStream(file).use { input ->
                        input.copyTo(zos)
                    }
                    zos.closeEntry()
                }
                file.delete()
                currentLogFile = null
            } catch (e: Exception) {
                android.util.Log.e("FileLogWriter", "Failed to compress log", e)
            }
        }

        /**
         * 清理旧日志文件
         */
        private fun cleanOldLogs() {
            try {
                val logDir = File(logDir)
                if (!logDir.exists()) return

                val files = logDir.listFiles()
                    ?.filter { it.name.endsWith(".txt") || it.name.endsWith(".zip") }
                    ?.sortedByDescending { it.lastModified() }
                    ?: return

                // 删除超过 maxFiles 数量的旧文件
                if (files.size > maxFiles) {
                    files.drop(maxFiles).forEach { it.delete() }
                }
            } catch (e: Exception) {
                android.util.Log.e("FileLogWriter", "Failed to clean old logs", e)
            }
        }
    }
}
