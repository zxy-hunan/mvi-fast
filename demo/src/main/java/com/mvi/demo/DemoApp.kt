package com.mvi.demo

import com.mvi.core.base.MviApplication

/**
 * Demo Application
 * 
 * DoKit 已在 MviApplication 中自动初始化
 */
class DemoApp : MviApplication() {
    
    override fun onInit() {
        // 这里可以添加其他初始化代码
        // DoKit 已经在父类中自动初始化了
    }
}
