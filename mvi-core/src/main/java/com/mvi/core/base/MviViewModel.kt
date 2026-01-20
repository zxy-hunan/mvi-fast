package com.mvi.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

/**
 * MVI ViewModel 基类
 *
 * 优化点:
 * 1. 使用 StateFlow 替代 SharedFlow,更符合状态管理语义
 * 2. 使用 Channel 处理一次性事件,避免事件重复消费
 * 3. 简化Intent处理,单一流设计
 * 4. 提供DSL风格的扩展函数
 *
 * @param I Intent类型
 */
abstract class MviViewModel<I : MviIntent> : ViewModel() {

    /**
     * Intent流 - 接收用户操作
     * 使用 DROP_OLDEST 策略防止积压，提升性能
     */
    private val _intent = MutableSharedFlow<I>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    
    /**
     * Intent 防抖 Job（用于防抖处理）
     */
    private var debounceJob: Job? = null

    /**
     * UI事件 - 一次性事件(Toast、导航等)
     */
    private val _uiEvent = Channel<UiEvent>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        // 订阅Intent流
        viewModelScope.launch {
            _intent.collect { intent ->
                handleIntent(intent)
            }
        }
    }

    /**
     * 发送Intent
     */
    fun sendIntent(intent: I) {
        viewModelScope.launch {
            _intent.emit(intent)
        }
    }

    /**
     * 发送UI事件
     */
    protected fun sendEvent(event: UiEvent) {
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }

    /**
     * 发送UI事件（别名，用于兼容）
     */
    protected fun sendUiEvent(event: UiEvent) {
        sendEvent(event)
    }

    /**
     * 处理Intent - 子类实现具体逻辑
     */
    protected abstract fun handleIntent(intent: I)

    /**
     * 显示Toast
     */
    fun showToast(message: String) {
        sendEvent(UiEvent.ShowToast(message))
    }

    /**
     * 显示/隐藏Loading
     */
    fun showLoading(show: Boolean = true) {
        sendEvent(UiEvent.ShowLoading(show))
    }

    /**
     * 在 IO 线程执行协程
     */
    protected fun launchIO(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            block()
        }
    }

    /**
     * 在主线程执行协程
     */
    protected fun launchMain(block: suspend CoroutineScope.() -> Unit) {
        viewModelScope.launch(Dispatchers.Main) {
            block()
        }
    }
    
    /**
     * 发送Intent（带防抖）
     * 
     * @param intent Intent
     * @param delayMillis 防抖延迟时间（毫秒，默认300ms）
     */
    fun sendIntentDebounced(intent: I, delayMillis: Long = 300L) {
        // 取消之前的防抖任务
        debounceJob?.cancel()
        
        // 创建新的防抖任务
        debounceJob = viewModelScope.launch {
            delay(delayMillis)
            _intent.emit(intent)
        }
    }
    
    /**
     * 批量发送Intent
     * 
     * @param intents Intent列表
     */
    fun sendIntents(intents: List<I>) {
        viewModelScope.launch {
            intents.forEach { intent ->
                _intent.emit(intent)
            }
        }
    }
    
    /**
     * 批量发送Intent（带防抖）
     * 
     * @param intents Intent列表
     * @param delayMillis 防抖延迟时间（毫秒，默认300ms）
     */
    fun sendIntentsDebounced(intents: List<I>, delayMillis: Long = 300L) {
        // 取消之前的防抖任务
        debounceJob?.cancel()
        
        // 创建新的防抖任务
        debounceJob = viewModelScope.launch {
            delay(delayMillis)
            intents.forEach { intent ->
                _intent.emit(intent)
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // 清理防抖任务
        debounceJob?.cancel()
    }
}
