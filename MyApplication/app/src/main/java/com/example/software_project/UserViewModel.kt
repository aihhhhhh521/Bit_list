package com.example.software_project

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.ResponseBody

class UserViewModel(
    application: Application
) : AndroidViewModel(application) {
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var newPassword by mutableStateOf("")
    var code by mutableStateOf("")
    var confirmPassword by mutableStateOf("")
    var name by mutableStateOf("")
    var stuId by mutableStateOf("")
    var grade by mutableStateOf("")
    var birth by mutableStateOf("")
    var school by mutableStateOf("")
    var avatarUri by mutableStateOf<String?>(null)
    var errorMessage by mutableStateOf("")
    var isCodeSent by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var isLoggedIn by mutableStateOf(false)
    var currentUserId by mutableStateOf<Int?>(null)
    private var authToken by mutableStateOf<String?>(null)

    // <-- 新增状态，用于存储正在查看的他人资料 -->
    var viewedUserProfile by mutableStateOf<User?>(null)
    var isViewingProfile by mutableStateOf(false)
    var viewProfileError by mutableStateOf("")

    init {
        checkAndValidateToken()
    }

    private fun checkAndValidateToken() {
        val sharedPreferences = getApplication<Application>().getSharedPreferences("auth", Context.MODE_PRIVATE)
        val existingToken = sharedPreferences.getString("auth_token", null)
        val savedUserId = sharedPreferences.getInt("user_id", -1) // <-- 修改: 默认值改为-1
        if (existingToken != null && savedUserId != -1) { // <-- 修改: 检查-1
            viewModelScope.launch {
                try {
                    authToken = existingToken
                    currentUserId = savedUserId
                    isLoggedIn = true
                    loadProfile() // 启动时只加载个人资料
                } catch (e: Exception) {
                    sharedPreferences.edit().remove("auth_token").remove("user_id").apply()
                    isLoggedIn = false
                    authToken = null
                    currentUserId = null
                }
            }
        }
    }

    private fun parseErrorBody(errorBody: ResponseBody?): String {
        return try {
            errorBody?.let {
                val errorJson = it.string()
                val errorResponse = Gson().fromJson(errorJson, ErrorResponse::class.java)
                // 使用 ?. 安全调用，如果 errorResponse 是 null，整个表达式会返回 null
                // 然后 elvis 操作符 ?: 会提供一个默认值
                errorResponse?.error ?: "Unknown error (or failed to parse server error)"
            } ?: "No error details provided"
        } catch (e: Exception) {
            "Error parsing response: ${e.message}"
        }
    }

    fun sendVerificationCode() {
        if (!isValidEmail(email)) {
            errorMessage = "无效的邮箱格式"
            return
        }
        viewModelScope.launch {
            isLoading = true
            try {
                val response = ApiClient.getApiService(getApplication()).sendCode(VerifyCodeRequest(email))
                /*response.isSuccessful*/
                if (response.isSuccessful) {
                    isCodeSent = true
                    errorMessage = "验证码已发送至 $email"
                } else {
                    errorMessage = "发送验证码失败: ${response.message()} (${parseErrorBody(response.errorBody())})"
                }
            } catch (e: Exception) {
                errorMessage = "发送验证码失败: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun verifyAndRegister(context: Context, navController: NavHostController) {
        if (password != confirmPassword) {
            errorMessage = "两次输入的密码不一致"
            return
        }
        if (!isValidPassword(password)) {
            errorMessage = "密码需至少8位，包含大小写字母和数字"
            return
        }
        if (code.isBlank()) {
            errorMessage = "验证码不能为空"
            return
        }
        viewModelScope.launch {
            isLoading = true
            try {
                // <-- 修改: User ID 传 0, 表示新用户
                val user = User(id = 0, name = name, email = email, grade = grade, birth = birth, stuId = stuId, school = school)
                val response = ApiClient.getApiService(context).verifyCode(RegisterRequest(user, password, code))
                if (response.isSuccessful) {
                    val registerResponse = response.body()
                    registerResponse != null
                    if (true) {
                        isLoggedIn = true
                        errorMessage = "注册成功"
                        navController.navigate("tasks") {
                            popUpTo("auth") { inclusive = true }
                        }
                    } else {
                        errorMessage = "注册失败：响应数据为空"
                    }
                } else {
                    errorMessage = "注册失败: ${response.message()} (${parseErrorBody(response.errorBody())})"
                }
            } catch (e: Exception) {
                errorMessage = "注册失败：${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun login(context: Context, navController: NavHostController) {
        if (!isValidEmail(email)) {
            errorMessage = "无效的邮箱格式"
            return
        }
        if (password.isBlank()) {
            errorMessage = "密码不能为空"
            return
        }
        viewModelScope.launch {
            isLoading = true
            try {
                val response = ApiClient.getApiService(context).login(LoginRequest(email, password))
                if (response.isSuccessful) {
                    val loginResponse = response.body()!!
                    authToken = loginResponse.token
                    currentUserId = loginResponse.userId
                    context.getSharedPreferences("auth", Context.MODE_PRIVATE)
                        .edit()
                        .putString("auth_token", authToken)
                        .putInt("user_id", currentUserId!!)
                        .apply()
                    isLoggedIn = true
                    loadProfile() // 登录后加载个人资料
                    errorMessage = "登录成功"
                    navController.navigate("tasks") {
                        popUpTo("auth") { inclusive = true }
                    }
                } else {
                    errorMessage = "登录失败: ${response.message()} (${parseErrorBody(response.errorBody())})"
                }
            } catch (e: Exception) {
                errorMessage = "登录失败: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun loadProfile() {
        currentUserId?.let {
            viewModelScope.launch {
                try {
                    val response = ApiClient.getApiService(getApplication()).getProfile()
                    if (response.isSuccessful) {
                        val user = response.body()!!
                        name = user.name
                        email = user.email
                        stuId = user.stuId
                        grade = user.grade
                        birth = user.birth
                        school = user.school
                        avatarUri = user.avatarUri
                        errorMessage = ""
                    } else {
                        errorMessage = "加载用户信息失败: ${response.message()} (${parseErrorBody(response.errorBody())})"
                    }
                } catch (e: Exception) {
                    errorMessage = "加载用户信息失败: ${e.message}"
                }
            }
        } ?: run {
            errorMessage = "用户未登录"
        }
    }

    fun uploadAvatar(uri: String) {
        currentUserId?.let { userId ->
            viewModelScope.launch {
                try {
                    val response = ApiClient.getApiService(getApplication()).updateProfile(UpdateProfileRequest(userId = userId, name = name, email = email, stuId = stuId, grade = grade, birth = birth, school = school, avatarUri = uri))
                    if (response.isSuccessful) {
                        avatarUri = uri
                        errorMessage = "头像上传成功"
                    } else {
                        errorMessage = "头像上传失败: ${response.message()} (${parseErrorBody(response.errorBody())})"
                    }
                } catch (e: Exception) {
                    errorMessage = "头像上传失败: ${e.message}"
                }
            }
        } ?: run {
            errorMessage = "用户未登录"
        }
    }

    fun updateProfile() {
        if (name.isBlank()) {
            errorMessage = "姓名不能为空"
            return
        }
        if (!isValidEmail(email)) {
            errorMessage = "无效的邮箱格式"
            return
        }
        if (stuId.isBlank()) {
            errorMessage = "学号不能为空"
            return
        }
        if (grade.isBlank()) {
            errorMessage = "年级不能为空"
            return
        }
        if (birth.isBlank()) {
            errorMessage = "生日不能为空"
            return
        }
        if (school.isBlank()) {
            errorMessage = "学校不能为空"
            return
        }
        currentUserId?.let { userId ->
            viewModelScope.launch {
                try {
                    val response = ApiClient.getApiService(getApplication()).updateProfile(UpdateProfileRequest(userId = userId, name = name, email = email, stuId = stuId, grade = grade, birth = birth, school = school, avatarUri = avatarUri))
                    if (response.isSuccessful) {
                        errorMessage = "修改成功"
                    } else {
                        errorMessage = "修改失败: ${response.message()} (${parseErrorBody(response.errorBody())})"
                    }
                } catch (e: Exception) {
                    errorMessage = "修改失败: ${e.message}"
                }
            }
        } ?: run {
            errorMessage = "用户未登录"
        }
    }

    fun logout(navController: NavHostController) {
        getApplication<Application>().getSharedPreferences("auth", Context.MODE_PRIVATE)
            .edit()
            .remove("auth_token")
            .remove("user_id")
            .apply()
        isLoggedIn = false
        avatarUri = null
        authToken = null
        currentUserId = null
        // 清空所有字段以备下次登录
        email = ""
        password = ""
        name = ""
        stuId = ""
        grade = ""
        birth = ""
        school = ""
        navController.navigate("auth") {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
        }
    }

    fun changePassword(navController: NavHostController) {
        if (!isValidPassword(newPassword)) {
            errorMessage = "新密码需至少8位，包含大小写字母和数字"
            return
        }
        if (password == newPassword) {
            errorMessage = "新密码不能与旧密码相同"
            return
        }
        if (newPassword != confirmPassword) {
            errorMessage = "两次输入的新密码不一致"
            return
        }
        currentUserId?.let { userId ->
            viewModelScope.launch {
                isLoading = true
                try {
                    val request = ChangePasswordRequest(userId, password, newPassword)
                    val response = ApiClient.getApiService(getApplication()).changePassword(request)
                    if (response.isSuccessful && response.body() == true) {
                        errorMessage = "密码修改成功"
                        password = ""
                        newPassword = ""
                        confirmPassword = ""
                        navController.popBackStack()
                    } else {
                        errorMessage = "密码修改失败: ${response.message()}"
                    }
                } catch (e: Exception) {
                    errorMessage = "密码修改失败: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
        } ?: run {
            errorMessage = "用户未登录"
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPassword(password: String): Boolean {
        return password.length >= 8 && password.any { it.isUpperCase() } &&
                password.any { it.isLowerCase() } && password.any { it.isDigit() }
    }

    // <-- 新增函数，用于根据ID加载特定用户的个人资料 -->
    fun loadProfileForViewing(userId: Int) {
        viewModelScope.launch {
            isViewingProfile = true
            viewProfileError = ""
            try {
                // 调用新增的API接口
                val response = ApiClient.getApiService(getApplication()).getUserProfileById(userId)
                if (response.isSuccessful) {
                    viewedUserProfile = response.body()
                } else {
                    viewProfileError = "无法加载成员资料: ${response.message()}"
                    viewedUserProfile = null
                }
            } catch (e: Exception) {
                viewProfileError = "加载成员资料时出错: ${e.message}"
                viewedUserProfile = null
            } finally {
                isViewingProfile = false
            }
        }
    }
}

data class ErrorResponse(
    val error: String?
)