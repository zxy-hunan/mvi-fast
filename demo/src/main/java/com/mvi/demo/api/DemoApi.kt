package com.mvi.demo.api

import com.mvi.core.network.ApiResponse
import com.mvi.demo.ui.User
import com.mvi.demo.ui.UserListData
import kotlinx.coroutines.delay
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Demo API接口
 */
interface DemoApi {

    @GET("/api/users")
    suspend fun getUserList(): ApiResponse<UserListData>

    @DELETE("/api/users/{id}")
    suspend fun deleteUser(@Path("id") userId: String): ApiResponse<Boolean>

    companion object {
        /**
         * Mock实例 - 用于演示
         */
        fun mockInstance(): DemoApi {
            return object : DemoApi {
                override suspend fun getUserList(): ApiResponse<UserListData> {
                    // 模拟网络延迟
                    delay(1000)

                    // 返回Mock数据
                    val users = listOf(
                        User("1", "张三", "zhangsan@example.com", ""),
                        User("2", "李四", "lisi@example.com", ""),
                        User("3", "王五", "wangwu@example.com", "")
                    )
                    return ApiResponse(
                        code = 200,
                        message = "success",
                        data = UserListData(users, users.size)
                    )
                }

                override suspend fun deleteUser(userId: String): ApiResponse<Boolean> {
                    delay(500)
                    return ApiResponse(
                        code = 200,
                        message = "删除成功",
                        data = true
                    )
                }
            }
        }
    }
}
