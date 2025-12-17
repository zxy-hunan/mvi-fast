package com.mvi.demo.ui.dialog

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.mvi.core.base.MviIntent
import com.mvi.core.base.MviViewModel
import com.mvi.ui.base.MviViewModelDialog
import com.mvi.core.base.UiState
import com.mvi.demo.databinding.DialogUserInfoBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 用户信息 Dialog 示例
 * 演示如何使用 MviViewModelDialog
 */
class UserInfoDialog(activity: AppCompatActivity) :
    MviViewModelDialog<DialogUserInfoBinding, UserInfoViewModel, UserInfoIntent>(activity) {

    private var onSaveSuccessCallback: ((UserInfo) -> Unit)? = null

    override fun createBinding(inflater: LayoutInflater): DialogUserInfoBinding {
        return DialogUserInfoBinding.inflate(inflater)
    }

    override fun getViewModelClass(): Class<UserInfoViewModel> {
        return UserInfoViewModel::class.java
    }

    override fun initView() {
        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString()
            val age = binding.etAge.text.toString().toIntOrNull() ?: 0
            sendIntent(UserInfoIntent.SaveUserInfo(name, age))
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.ivClose.setOnClickListener {
            dismiss()
        }

        // 初始加载用户信息
        sendIntent(UserInfoIntent.LoadUserInfo)
    }

    override suspend fun observeData() {
        // 观察用户状态
        // observeData() 是挂起函数，在协程作用域中被调用
        // 使用 kotlinx.coroutines.coroutineScope 创建子作用域并启动协程
        kotlinx.coroutines.coroutineScope {
            launch {
                viewModel.userState.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            binding.progressBar.visibility = android.view.View.VISIBLE
                            binding.btnSave.isEnabled = false
                        }
                        is UiState.Success -> {
                            binding.progressBar.visibility = android.view.View.GONE
                            binding.btnSave.isEnabled = true
                            binding.etName.setText(state.data.name)
                            binding.etAge.setText(state.data.age.toString())

                            // 触发保存成功回调
                            onSaveSuccessCallback?.invoke(state.data)
                        }
                        is UiState.Error -> {
                            binding.progressBar.visibility = android.view.View.GONE
                            binding.btnSave.isEnabled = true
                            showToast(state.message)
                        }
                        else -> {
                            binding.progressBar.visibility = android.view.View.GONE
                            binding.btnSave.isEnabled = true
                        }
                    }
                }
            }
        }
    }

    override fun handleLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnSave.isEnabled = !show
    }

    /**
     * 设置保存成功回调
     * 注意：必须在 show() 之前调用
     */
    fun setOnSaveSuccess(callback: (UserInfo) -> Unit): UserInfoDialog {
        onSaveSuccessCallback = callback
        return this
    }
}

/**
 * 用户信息数据类
 */
data class UserInfo(
    val name: String = "",
    val age: Int = 0
)

/**
 * UserInfo Intent
 */
sealed class UserInfoIntent : MviIntent {
    data object LoadUserInfo : UserInfoIntent()
    data class SaveUserInfo(val name: String, val age: Int) : UserInfoIntent()
}

/**
 * UserInfo ViewModel
 */
class UserInfoViewModel : MviViewModel<UserInfoIntent>() {

    private val _userState = MutableStateFlow<UiState<UserInfo>>(UiState.Idle)
    val userState: StateFlow<UiState<UserInfo>> = _userState.asStateFlow()

    override fun handleIntent(intent: UserInfoIntent) {
        when (intent) {
            is UserInfoIntent.LoadUserInfo -> loadUserInfo()
            is UserInfoIntent.SaveUserInfo -> saveUserInfo(intent.name, intent.age)
        }
    }

    private fun loadUserInfo() {
        launchIO {
            _userState.value = UiState.Loading()

            // 模拟网络请求
            kotlinx.coroutines.delay(1000)

            // 模拟加载用户数据
            val userInfo = UserInfo(name = "张三", age = 25)
            _userState.value = UiState.Success(userInfo)
        }
    }

    private fun saveUserInfo(name: String, age: Int) {
        launchIO {
            _userState.value = UiState.Loading()

            // 验证输入
            if (name.isEmpty()) {
                sendUiEvent(com.mvi.core.base.UiEvent.ShowToast("姓名不能为空"))
                _userState.value = UiState.Error("姓名不能为空")
                return@launchIO
            }

            if (age <= 0) {
                sendUiEvent(com.mvi.core.base.UiEvent.ShowToast("年龄必须大于0"))
                _userState.value = UiState.Error("年龄必须大于0")
                return@launchIO
            }

            // 模拟网络请求
            kotlinx.coroutines.delay(1500)

            // 保存成功
            val userInfo = UserInfo(name = name, age = age)
            _userState.value = UiState.Success(userInfo)
            sendUiEvent(com.mvi.core.base.UiEvent.ShowToast("保存成功"))
        }
    }
}
