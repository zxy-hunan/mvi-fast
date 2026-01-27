package com.mvi.demo

import com.mvi.core.base.MviApplication
import com.mvi.core.network.RetrofitClient
import com.mvi.core.storage.MmkvStorage

/**
 * Demo Application
 * 
 * DoKit 已在 MviApplication 中自动初始化
 */
class DemoApp : MviApplication() {
    
    override fun onInit() {
        // 这里可以添加其他初始化代码
        // DoKit 已经在父类中自动初始化了
        MmkvStorage.init(filesDir.absolutePath)
        RetrofitClient.init { baseUrl = "..." }

    }
}
