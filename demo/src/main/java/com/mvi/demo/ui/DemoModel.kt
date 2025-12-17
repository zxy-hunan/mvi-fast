package com.mvi.demo.ui

/**
 * Demo数据模型
 */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val avatar: String
)

data class UserListData(
    val users: List<User>,
    val total: Int
)
