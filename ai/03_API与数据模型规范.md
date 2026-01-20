# API 与数据模型规范

## 📋 概述

本文档定义了 API 接口设计、数据模型定义、错误处理和 JSON 序列化的规范。

## 🌐 API 接口规范

### 1. Retrofit 接口定义

#### 基础规范
```kotlin
/**
 * API 接口必须返回 ApiResponse<T>
 * 使用 suspend 函数支持协程
 */
interface UserApi {
    @GET("/api/users")
    suspend fun getUserList(): ApiResponse<List<User>>
    
    @POST("/api/users")
    suspend fun createUser(@Body user: User): ApiResponse<User>
    
    @DELETE("/api/users/{id}")
    suspend fun deleteUser(@Path("id") userId: String): ApiResponse<Boolean>
}
```

#### 命名规范
- ✅ **接口名称**：使用名词，如 `UserApi`、`ProductApi`
- ✅ **方法名称**：使用动词，如 `getUserList`、`createUser`、`deleteUser`
- ✅ **参数名称**：使用驼峰命名，如 `userId`、`userName`

#### HTTP 方法使用
```kotlin
// ✅ GET：查询数据
@GET("/api/users")
suspend fun getUserList(): ApiResponse<List<User>>

// ✅ POST：创建资源
@POST("/api/users")
suspend fun createUser(@Body user: User): ApiResponse<User>

// ✅ PUT：更新资源（完整更新）
@PUT("/api/users/{id}")
suspend fun updateUser(@Path("id") userId: String, @Body user: User): ApiResponse<User>

// ✅ PATCH：更新资源（部分更新）
@PATCH("/api/users/{id}")
suspend fun patchUser(@Path("id") userId: String, @Body updates: Map<String, Any>): ApiResponse<User>

// ✅ DELETE：删除资源
@DELETE("/api/users/{id}")
suspend fun deleteUser(@Path("id") userId: String): ApiResponse<Boolean>
```

### 2. ApiResponse 统一响应格式

#### 定义
```kotlin
/**
 * 通用 API 响应封装
 * 
 * @param T 数据类型
 * @param code 状态码（200/0 表示成功）
 * @param message 消息
 * @param data 数据（可能为 null）
 */
data class ApiResponse<T>(
    val code: Int,
    val message: String,
    val data: T?
) {
    /**
     * 判断是否成功
     */
    fun isSuccess(): Boolean = code == 200 || code == 0
    
    /**
     * 获取数据或抛出异常
     */
    fun getOrThrow(): T {
        if (isSuccess() && data != null) {
            return data
        }
        throw ApiException(code, message)
    }
}
```

#### 使用规范
```kotlin
// ✅ 正确：在 ViewModel 中使用
launchRequest(_userState) {
    val response = apiService.getUserList()
    // launchRequest 会自动处理 ApiResponse
    response
}

// ✅ 正确：手动处理 ApiResponse
val response = apiService.getUserList()
if (response.isSuccess() && response.data != null) {
    _userState.value = UiState.Success(response.data)
} else {
    _userState.value = UiState.Error(response.message)
}
```

### 3. Retrofit 客户端配置

#### 初始化
```kotlin
// ✅ 正确：在 Application 中初始化
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        RetrofitClient.init {
            baseUrl = "https://api.example.com"
            connectTimeout = 30L
            readTimeout = 30L
            writeTimeout = 30L
            enableLogging = BuildConfig.DEBUG
            enableDataInterceptor = true
            headers = mapOf(
                "Content-Type" to "application/json",
                "Accept" to "application/json"
            )
        }
    }
}
```

#### 创建 API 实例
```kotlin
// ✅ 正确：使用 RetrofitClient 创建
val userApi = RetrofitClient.create(UserApi::class.java)
```

## 📦 数据模型规范

### 1. 数据类定义

#### 基础规范
```kotlin
/**
 * 用户数据模型
 * 
 * @param id 用户ID
 * @param name 用户名
 * @param email 邮箱
 * @param avatar 头像URL
 */
data class User(
    val id: String,
    val name: String,
    val email: String,
    val avatar: String = ""
) {
    /**
     * 显示名称（计算属性）
     */
    val displayName: String
        get() = name.ifEmpty { email }
}
```

#### 命名规范
- ✅ **类名**：使用大驼峰，如 `User`、`UserListData`
- ✅ **属性名**：使用小驼峰，如 `userId`、`userName`
- ✅ **布尔属性**：使用 `is` 前缀，如 `isActive`、`isDeleted`

#### JSON 序列化

##### Gson 注解
```kotlin
/**
 * 使用 @SerializedName 映射 JSON 字段
 */
data class User(
    @SerializedName("user_id")
    val userId: String,
    
    @SerializedName("user_name")
    val userName: String,
    
    @SerializedName("created_at")
    val createdAt: Long
)
```

##### 可选字段处理
```kotlin
/**
 * 使用可空类型处理可选字段
 */
data class User(
    val id: String,
    val name: String,
    val email: String? = null,  // 可选字段
    val avatar: String? = null  // 可选字段
)
```

##### 默认值处理
```kotlin
/**
 * 使用默认值处理缺失字段
 */
data class User(
    val id: String,
    val name: String,
    val status: String = "active",  // 默认值
    val score: Int = 0              // 默认值
)
```

### 2. 嵌套数据模型

#### 示例
```kotlin
/**
 * 用户列表响应数据
 */
data class UserListData(
    val users: List<User>,
    val total: Int,
    val page: Int,
    val pageSize: Int
)

/**
 * 分页响应
 */
data class PageResponse<T>(
    val list: List<T>,
    val total: Int,
    val pageNum: Int,
    val pageSize: Int
)
```

### 3. 枚举类型

#### 定义规范
```kotlin
/**
 * 用户状态枚举
 */
enum class UserStatus {
    @SerializedName("active")
    ACTIVE,
    
    @SerializedName("inactive")
    INACTIVE,
    
    @SerializedName("banned")
    BANNED
}
```

## ⚠️ 错误处理规范

### 1. 异常类型

#### ApiException
```kotlin
/**
 * API 异常
 */
class ApiException(
    val code: Int,
    override val message: String
) : Exception(message)
```

#### 错误状态码
```kotlin
object ErrorStatus {
    const val NETWORK_ERROR = -1
    const val NETWORK_TIMEOUT = -2
    const val SOCKET_EXCEPTION = -3
    const val JSON_ERROR = -4
    const val SERVER_ERROR = -5
    const val UNKNOWN_ERROR = -6
    const val NULL_POINTER = -7
    const val INDEX_OUT_OF_BOUNDS = -8
    const val CLASS_CAST = -9
    const val ARRAY_STORE = -10
}
```

### 2. 统一异常处理

#### ExceptionHandle 使用
```kotlin
/**
 * 统一异常处理
 * 在 Application 中初始化
 */
ExceptionHandle.init(context)

/**
 * 在 ViewModel 中使用
 */
try {
    val response = apiService.getUserList()
    // 处理成功
} catch (e: Exception) {
    val errorData = e.handleException()
    _userState.value = UiState.Error(errorData.message, e)
    showToast(errorData.message)
}
```

#### 错误数据模型
```kotlin
/**
 * 网络错误数据
 */
data class NetErrorData(
    var code: Int = ErrorStatus.UNKNOWN_ERROR,
    var message: String = "未知错误"
) {
    /**
     * 是否为网络错误
     */
    fun isNetworkError(): Boolean {
        return code == ErrorStatus.NETWORK_ERROR ||
               code == ErrorStatus.NETWORK_TIMEOUT ||
               code == ErrorStatus.SOCKET_EXCEPTION
    }
    
    /**
     * 是否需要重新登录
     */
    fun needReLogin(): Boolean {
        return code == HttpURLConnection.HTTP_UNAUTHORIZED
    }
}
```

### 3. HTTP 状态码处理

#### 常见状态码
```kotlin
when (httpCode) {
    HttpURLConnection.HTTP_UNAUTHORIZED -> {
        // 401：未授权，需要重新登录
        errorData.message = "登录已过期，请重新登录"
        // 跳转到登录页
    }
    HttpURLConnection.HTTP_FORBIDDEN -> {
        // 403：禁止访问
        errorData.message = "没有权限访问"
    }
    HttpURLConnection.HTTP_NOT_FOUND -> {
        // 404：资源不存在
        errorData.message = "请求的资源不存在"
    }
    HttpURLConnection.HTTP_INTERNAL_ERROR -> {
        // 500：服务器错误
        errorData.message = "服务器错误，请稍后重试"
    }
    else -> {
        errorData.message = "请求失败：$httpCode"
    }
}
```

## 🔧 JSON 序列化配置

### 1. Gson 配置

#### Retrofit 使用 Gson
```kotlin
Retrofit.Builder()
    .baseUrl(baseUrl)
    .client(okHttpClient)
    .addConverterFactory(GsonConverterFactory.create(Gson()))
    .build()
```

#### 自定义 Gson 配置
```kotlin
val gson = GsonBuilder()
    .setDateFormat("yyyy-MM-dd HH:mm:ss")
    .setLenient()  // 宽松模式
    .serializeNulls()  // 序列化 null 值
    .create()

Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create(gson))
    .build()
```

### 2. 日期时间序列化

#### 使用 @SerializedName 和自定义转换
```kotlin
data class User(
    val id: String,
    val name: String,
    
    @SerializedName("created_at")
    @JsonAdapter(TimestampAdapter::class)
    val createdAt: Date
)
```

## 📝 最佳实践

### 1. API 接口设计
```kotlin
// ✅ 正确：RESTful 风格
@GET("/api/users")
suspend fun getUserList(): ApiResponse<List<User>>

@GET("/api/users/{id}")
suspend fun getUser(@Path("id") userId: String): ApiResponse<User>

@POST("/api/users")
suspend fun createUser(@Body user: User): ApiResponse<User>

// ❌ 错误：非 RESTful 风格
@POST("/api/getUserList")  // ❌ 应该使用 GET
suspend fun getUserList(): ApiResponse<List<User>>
```

### 2. 数据模型设计
```kotlin
// ✅ 正确：使用 data class，不可变
data class User(
    val id: String,
    val name: String,
    val email: String
)

// ❌ 错误：使用 var，可变
class User(
    var id: String,  // ❌ 应该使用 val
    var name: String
)
```

### 3. 错误处理
```kotlin
// ✅ 正确：统一异常处理
launchRequest(_userState) {
    apiService.getUserList()
}

// ❌ 错误：忽略异常
try {
    val response = apiService.getUserList()  // ❌ 没有错误处理
} catch (e: Exception) {
    // 空 catch
}
```

## 🔍 常见问题

### Q1: 如何处理分页数据？
**A**: 使用统一的分页响应模型：
```kotlin
data class PageResponse<T>(
    val list: List<T>,
    val total: Int,
    val pageNum: Int,
    val pageSize: Int
)

interface UserApi {
    @GET("/api/users")
    suspend fun getUserList(
        @Query("page") page: Int,
        @Query("size") size: Int
    ): ApiResponse<PageResponse<User>>
}
```

### Q2: 如何处理文件上传？
**A**: 使用 `@Multipart` 和 `@Part`：
```kotlin
@Multipart
@POST("/api/upload")
suspend fun uploadFile(
    @Part file: MultipartBody.Part
): ApiResponse<FileInfo>
```

### Q3: 如何处理自定义请求头？
**A**: 使用拦截器或在接口方法上使用 `@Header`：
```kotlin
@GET("/api/users")
suspend fun getUserList(
    @Header("Authorization") token: String
): ApiResponse<List<User>>
```

---

**最后更新**: 2024-12-17  
**维护者**: aFramework Team
