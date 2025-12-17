package com.mvi.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

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
     */
    private val _intent = MutableSharedFlow<I>()

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
}
