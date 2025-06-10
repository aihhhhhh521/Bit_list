package com.example.software_project

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import java.io.OutputStream
import java.time.ZonedDateTime
import java.time.Duration
import java.time.ZoneId

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    var tasks by mutableStateOf<List<Task>>(emptyList())
    var deletedTasks by mutableStateOf<List<Task>>(emptyList())
    var isSortByPriority by mutableStateOf(false)
    var selectedDate by mutableStateOf("2025-06-04")
    var deletedAttachments by mutableStateOf<Map<Int, List<Attachment>>>(emptyMap()) // <-- 修改: Key String -> Int
    var teams by mutableStateOf<List<Team>>(emptyList())
    var currentUserId by mutableStateOf<Int?>(null)
    var errorMessage by mutableStateOf("")
    var userStats by mutableStateOf<UserStatsResponse?>(null)
    var isLoading by mutableStateOf(false)
    private var authToken by mutableStateOf<String?>(null)

    // 数据统计相关状态
    var trendLength by mutableStateOf(12)
    var taskDateRange by mutableStateOf(7)
    var timeAllocationPeriod by mutableStateOf("tags")
    var statusFilter by mutableStateOf("ALL")
    var timeAllocationReport by mutableStateOf<List<TimeAllocationDataPoint>>(emptyList())
    var suggestions by mutableStateOf<List<String>>(emptyList())

    private val SINGLE_FILE_MAX_SIZE = 10_485_760L // 10MB
    private val MAX_TOTAL_SIZE = 104_857_600L // 100MB

    init {
        loadAuthToken()
        loadTasks()
        loadTeams()
        loadUserStats()
    }

    private fun loadAuthToken() {
        val sharedPreferences = getApplication<Application>().getSharedPreferences("auth", Context.MODE_PRIVATE)
        authToken = sharedPreferences.getString("auth_token", null)
        currentUserId = sharedPreferences.getInt("user_id", -1).takeIf { it != -1 } // <-- 修改: 读取Int
    }

    fun loadTasks() {
        viewModelScope.launch {
            try {
                currentUserId?.let { userId ->
                    val response = ApiClient.getApiService(getApplication()).getTasks(userId)
                    if (response.isSuccessful) {
                        tasks = response.body() ?: emptyList()
                        errorMessage = ""
                        cleanupExpiredTasks()
                        cleanupExpiredAttachments()
                    } else {
                        errorMessage = "从服务器加载任务失败: ${response.message()}"
                    }
                } ?: run {
                    errorMessage = "用户未登录"
                }
            } catch (e: Exception) {
                errorMessage = "加载任务失败: ${e.message}"
            }
        }
    }

    private fun cleanupExpiredAttachments() {
        val now = ZonedDateTime.now(ZoneId.of("UTC"))
        val attachmentsToPermanentlyDelete = mutableMapOf<Int, List<Int>>() // <-- 修改: taskId, attachmentId -> Int

        deletedAttachments.forEach { (taskId, attachments) ->
            val expiredIds = attachments.filter { attachment ->
                attachment.deletedAt?.let {
                    val deletionTime = ZonedDateTime.parse(it)
                    Duration.between(deletionTime, now).toDays() >= 7
                } ?: false
            }.map { it.id }

            if (expiredIds.isNotEmpty()) {
                attachmentsToPermanentlyDelete[taskId] = expiredIds
            }
        }

        if (attachmentsToPermanentlyDelete.isNotEmpty()) {
            viewModelScope.launch {
                attachmentsToPermanentlyDelete.forEach { (taskId, attachmentIds) ->
                    attachmentIds.forEach { attachmentId ->
                        permanentlyDeleteAttachment(taskId, attachmentId)
                    }
                }
            }
        }
    }

    private fun permanentlyDeleteAttachment(taskId: Int, attachmentId: Int) { // <-- 修改: String -> Int
        viewModelScope.launch {
            try {
                val response = ApiClient.getApiService(getApplication()).deleteAttachment(taskId, attachmentId)
                if (response.isSuccessful) {
                    val updatedList = deletedAttachments[taskId]?.filter { it.id != attachmentId }
                    if (updatedList != null) {
                        deletedAttachments = deletedAttachments.toMutableMap().apply {
                            this[taskId] = updatedList
                        }
                    }
                    Log.d("TaskViewModel", "Successfully auto-cleaned attachment: $attachmentId")
                } else {
                    Log.e("TaskViewModel", "Failed to auto-cleanup attachment $attachmentId: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("TaskViewModel", "Error auto-cleanup attachment $attachmentId: ${e.message}")
            }
        }
    }

    fun deleteAttachment(taskId: Int, attachmentId: Int) {
        val task = tasks.find { it.id == taskId } ?: return
        val attachment = task.attachments.find { it.id == attachmentId } ?: return

        viewModelScope.launch {
            try {
                val response = ApiClient.getApiService(getApplication()).deleteAttachment(taskId, attachmentId)
                if (response.isSuccessful) {
                    val updatedAttachments = task.attachments.filter { it.id != attachmentId }
                    val updatedTask = task.copy(attachments = updatedAttachments)
                    tasks = tasks.map { if (it.id == taskId) updatedTask else it }

                    val timestamp = ZonedDateTime.now(ZoneId.of("UTC")).toString()
                    val attachmentToRecycle = attachment.copy(isDeleted = true, deletedAt = timestamp)

                    val currentRecycled = deletedAttachments[taskId].orEmpty()
                    deletedAttachments = deletedAttachments + (taskId to (currentRecycled + attachmentToRecycle))
                    errorMessage = "附件删除成功"
                } else {
                    errorMessage = "附件删除失败: ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "附件删除失败: ${e.message}"
            }
        }
    }

    fun restoreAttachment(taskId: Int, attachmentId: Int): Boolean { // <-- 修改: String -> Int
        val deletedAttachmentsForTask = deletedAttachments[taskId].orEmpty()
        val attachment = deletedAttachmentsForTask.find { it.id == attachmentId } ?: return false
        val task = tasks.find { it.id == taskId } ?: return false

        if (task.attachments.sumOf { it.sizeInBytes } + attachment.sizeInBytes > MAX_TOTAL_SIZE) {
            errorMessage = "附件大小超出限制"
            return false
        }

        viewModelScope.launch {
            try {
                val response = ApiClient.getApiService(getApplication()).restoreAttachment(taskId, attachmentId)
                if (response.isSuccessful) {
                    val updatedDeletedAttachments = deletedAttachmentsForTask.filter { it.id != attachmentId }
                    val restoredAttachment = attachment.copy(isDeleted = false, deletedAt = null)
                    val updatedTask = task.copy(attachments = task.attachments + restoredAttachment)

                    tasks = tasks.map { if (it.id == taskId) updatedTask else it }
                    deletedAttachments = deletedAttachments + (taskId to updatedDeletedAttachments)
                    errorMessage = "附件恢复成功"
                } else {
                    errorMessage = "附件恢复失败: ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "附件恢复失败: ${e.message}"
            }
        }
        return true
    }

    fun loadTeams() {
        viewModelScope.launch {
            try {
                currentUserId?.let { userId ->
                    val response = ApiClient.getApiService(getApplication()).getTeams(userId)
                    if (response.isSuccessful) {
                        teams = response.body() ?: emptyList()
                        errorMessage = ""
                    } else {
                        errorMessage = "从服务器加载团队失败: ${response.message()}"
                    }
                } ?: run {
                    errorMessage = "用户未登录"
                }
            } catch (e: Exception) {
                errorMessage = "加载团队失败: ${e.message}"
            }
        }
    }

    fun loadUserStats() {
        viewModelScope.launch {
            try {
                currentUserId?.let { userId ->
                    val response = ApiClient.getApiService(getApplication()).getUserStats(userId, trendLength, taskDateRange, timeAllocationPeriod)
                    if (response.isSuccessful) {
                        userStats = response.body()
                        response.body()?.let {
                            timeAllocationReport = it.timeAllocationReport
                            suggestions = it.suggestions
                        }
                        errorMessage = ""
                    } else {
                        errorMessage = "加载用户统计数据失败: ${response.message()}"
                    }
                } ?: run {
                    errorMessage = "用户未登录"
                }
            } catch (e: Exception) {
                errorMessage = "加载用户统计数据失败: ${e.message}"
            }
        }
    }

    fun updateStatsView(
        newTrendLength: Int? = null,
        newDateRange: Int? = null,
        newStatusFilter: String? = null,
        newTimeAllocationPeriod: String? = null
    ) {
        var needsApiReload = false

        newTrendLength?.let {
            if (it != trendLength) {
                trendLength = it
                needsApiReload = true
            }
        }
        newDateRange?.let {
            if (it != taskDateRange) {
                taskDateRange = it
                needsApiReload = true
            }
        }
        newTimeAllocationPeriod?.let {
            if (it != timeAllocationPeriod) {
                timeAllocationPeriod = it
                needsApiReload = true
            }
        }

        if (needsApiReload) {
            loadUserStats()
        }

        newStatusFilter?.let {
            if (it != statusFilter) {
                statusFilter = it
            }
        }
    }

    fun submitFocusData(focusData: TomatoFocusData) {
        viewModelScope.launch {
            try {
                currentUserId?.let { userId ->
                    val response = ApiClient.getApiService(getApplication()).submitFocusData(focusData.copy(userId = userId))
                    if (response.isSuccessful) {
                        errorMessage = "专注数据提交成功"
                        loadUserStats()
                    } else {
                        errorMessage = "提交专注数据失败: ${response.message()}"
                    }
                } ?: run {
                    errorMessage = "用户未登录，无法提交专注数据"
                }
            } catch (e: Exception) {
                errorMessage = "提交专注数据失败: ${e.message}"
            }
        }
    }

    fun createTeam(team: Team) {
        viewModelScope.launch {
            try {
                val response = ApiClient.getApiService(getApplication()).createTeam(team)
                if (response.isSuccessful) {
                    teams = teams + response.body()!!
                    errorMessage = "团队创建成功"
                } else {
                    errorMessage = "创建团队失败: ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "创建团队失败: ${e.message}"
            }
        }
    }

    fun createTask(task: Task) {
        viewModelScope.launch {
            try {
                val newTask = task.copy(order = tasks.size)
                val response = ApiClient.getApiService(getApplication()).createTask(newTask)
                if (response.isSuccessful) {
                    val createdTask = response.body()!!
                    tasks = tasks + createdTask

                    if (newTask.isTeamTask && newTask.teamId != null) {
                        teams = teams.map {
                            if (it.id == newTask.teamId) it.copy(tasks = it.tasks + createdTask.id) else it // <-- 修改
                        }
                        updateTeam(newTask.teamId!!)
                    }
                    errorMessage = "任务创建成功"
                    NotificationScheduler.scheduleNotifications(getApplication(), createdTask)
                } else {
                    errorMessage = "创建任务失败: ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "创建任务失败: ${e.message}"
            }
        }
    }

    fun createSubTask(parentTaskId: Int, subTask: Task) {
        val parentTask = tasks.find { it.id == parentTaskId } ?: return

        viewModelScope.launch {
            try {
                val newSubTask = subTask.copy(
                    isTeamTask = parentTask.isTeamTask,
                    teamId = parentTask.teamId,
                    parentTaskId = parentTaskId,
                    order = tasks.size
                )
                val response = ApiClient.getApiService(getApplication()).createTask(newSubTask)
                if (response.isSuccessful) {
                    val createdSubTask = response.body()!! // <--
                    tasks = tasks + createdSubTask
                    if (newSubTask.isTeamTask && newSubTask.teamId != null) {
                        teams = teams.map {
                            if (it.id == newSubTask.teamId) it.copy(tasks = it.tasks + createdSubTask.id) else it // <--
                        }
                        updateTeam(newSubTask.teamId!!)
                    }
                    errorMessage = "子任务创建成功"
                } else {
                    errorMessage = "创建子任务失败: ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "创建子任务失败: ${e.message}"
            }
        }
    }

    private fun updateTeam(teamId: Int) {
        viewModelScope.launch {
            try {
                val team = teams.find { it.id == teamId } ?: return@launch
                val response = ApiClient.getApiService(getApplication()).updateTeam(teamId, team)
                if (!response.isSuccessful) {
                    errorMessage = "同步团队 $teamId 失败: ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "同步团队失败: ${e.message}"
            }
        }
    }

    fun updateTeamInfo(teamId: Int, newName: String, newDescription: String) {
        if (newName.isBlank()) {
            errorMessage = "团队名称不能为空"
            return
        }

        val originalTeam = teams.find { it.id == teamId } ?: return
        if (originalTeam.members[currentUserId] != Role.ADMIN.toString()) {
            errorMessage = "只有管理员可以修改团队信息"
            return
        }

        val updatedTeam = originalTeam.copy(name = newName, description = newDescription)
        viewModelScope.launch {
            try {
                val response = ApiClient.getApiService(getApplication()).updateTeam(teamId, updatedTeam)
                if (response.isSuccessful) {
                    teams = teams.map { if (it.id == teamId) updatedTeam else it }
                    errorMessage = "团队信息更新成功"
                } else {
                    errorMessage = "更新失败: ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "更新失败: ${e.message}"
            }
        }
    }

    fun reorderTasks(newOrder: List<Task>) {
        viewModelScope.launch {
            try {
                val response = ApiClient.getApiService(getApplication()).reorderTasks(newOrder)
                if (response.isSuccessful) {
                    tasks = newOrder
                    errorMessage = "任务排序成功"
                } else {
                    errorMessage = "任务排序失败: ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "任务排序失败: ${e.message}"
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                val timestamp = ZonedDateTime.now(ZoneId.of("UTC")).toString()
                val updatedTask = task.copy(isDeleted = true, deletedAt = timestamp)
                val response = ApiClient.getApiService(getApplication()).updateTask(task.id, updatedTask)

                if (response.isSuccessful) {
                    tasks = tasks.filter { it.id != task.id }
                    deletedTasks = deletedTasks + updatedTask

                    if (task.isTeamTask && task.teamId != null) {
                        teams = teams.map {
                            if (it.id == task.teamId) it.copy(tasks = it.tasks.filter { id -> id != task.id }) else it
                        }
                        updateTeam(task.teamId!!)
                    }
                    errorMessage = "任务已移至回收站"
                } else {
                    errorMessage = "删除任务失败: ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "删除任务失败: ${e.message}"
            }
        }
    }

    fun markTaskAsCompleted(task: Task) {
        viewModelScope.launch {
            try {
                val response = ApiClient.getApiService(getApplication()).markTaskAsCompleted(task.id)
                if (response.isSuccessful) {
                    tasks = tasks.map {
                        if (it.id == task.id) it.copy(status = TaskStatus.DONE) else it
                    }
                    errorMessage = "任务标记为完成"
                } else {
                    errorMessage = "标记任务失败: ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "标记任务失败: ${e.message}"
            }
        }
    }

    fun restoreTask(task: Task) {
        viewModelScope.launch {
            try {
                val restoredTask = task.copy(isDeleted = false, status = TaskStatus.TODO, order = tasks.size, deletedAt = null)
                val response = ApiClient.getApiService(getApplication()).updateTask(task.id, restoredTask)

                if (response.isSuccessful) {
                    deletedTasks = deletedTasks.filter { it.id != task.id }
                    tasks = tasks + restoredTask
                    if (task.isTeamTask && task.teamId != null) {
                        teams = teams.map {
                            if (it.id == task.teamId) it.copy(tasks = it.tasks + task.id) else it
                        }
                        updateTeam(task.teamId!!)
                    }
                    errorMessage = "任务恢复成功"
                } else {
                    errorMessage = "恢复任务失败: ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "恢复任务失败: ${e.message}"
            }
        }
    }

    private fun cleanupExpiredTasks() {
        val now = ZonedDateTime.now(ZoneId.of("UTC"))
        val tasksToPermanentlyDelete = deletedTasks.filter { task ->
            task.deletedAt?.let {
                val deletionTime = ZonedDateTime.parse(it)
                Duration.between(deletionTime, now).toDays() >= 7
            } ?: false
        }

        if (tasksToPermanentlyDelete.isNotEmpty()) {
            viewModelScope.launch {
                tasksToPermanentlyDelete.forEach { task ->
                    permanentlyDeleteTask(task)
                }
            }
        }
    }

    fun permanentlyDeleteTask(task: Task) {
        viewModelScope.launch {
            try {
                val response = ApiClient.getApiService(getApplication()).deleteTask(task.id)
                if (response.isSuccessful) {
                    deletedTasks = deletedTasks.filter { it.id != task.id }
                    if (task.isTeamTask && task.teamId != null) {
                        teams = teams.map {
                            if (it.id == task.teamId) it.copy(tasks = it.tasks.filter { id -> id != task.id }) else it
                        }
                        updateTeam(task.teamId!!)
                    }
                    errorMessage = "任务永久删除成功"
                } else {
                    errorMessage = "永久删除任务失败: ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "永久删除任务失败: ${e.message}"
            }
        }
    }

    fun sortByPriority() {
        tasks = if (isSortByPriority) {
            tasks.sortedBy { it.order }
        } else {
            tasks.sortedWith(
                compareByDescending<Task> { it.priority.ordinal }
                    .thenBy { it.tags.firstOrNull()?.firstOrNull()?.lowercase() }
            )
        }
        isSortByPriority = !isSortByPriority
        viewModelScope.launch {
            try {
                val response = ApiClient.getApiService(getApplication()).reorderTasks(tasks)
                if (!response.isSuccessful) {
                    errorMessage = "同步排序失败: ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "同步排序失败: ${e.message}"
            }
        }
    }

    fun updateTaskDueDate(taskId: Int, newDueDate: String) {
        viewModelScope.launch {
            try {
                val updatedTask = tasks.find { it.id == taskId }?.copy(dueDate = newDueDate) ?: return@launch
                val response = ApiClient.getApiService(getApplication()).updateTask(taskId, updatedTask)
                if (response.isSuccessful) {
                    tasks = tasks.map {
                        if (it.id == taskId) updatedTask else it
                    }
                    errorMessage = "更新截止日期成功"
                    NotificationScheduler.cancelNotifications(getApplication(), updatedTask)
                    NotificationScheduler.scheduleNotifications(getApplication(), updatedTask)
                } else {
                    errorMessage = "更新截止日期失败: ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "更新截止日期失败: ${e.message}"
            }
        }
    }

    fun uploadAttachmentFromUri(taskId: Int, uri: Uri) {
        val context = getApplication<Application>().applicationContext
        val contentResolver = context.contentResolver
        var fileName = "unknown_file"
        var fileSize = 0L
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    fileSize = cursor.getLong(sizeIndex)
                }
            }
        }

        if (fileSize > SINGLE_FILE_MAX_SIZE) {
            errorMessage = "上传失败：单个文件大小不能超过10MB。"
            return
        }

        val task = tasks.find { it.id == taskId }
        if (task == null) {
            errorMessage = "上传失败：找不到对应的任务。"
            return
        }
        val currentTotalSize = task.attachments.sumOf { it.sizeInBytes }
        if (currentTotalSize + fileSize > MAX_TOTAL_SIZE) {
            errorMessage = "上传失败：该任务的附件总大小将超过100MB。"
            return
        }

        val mimeType = contentResolver.getType(uri)
        val requestBody = contentResolver.openInputStream(uri)?.use { inputStream ->
            inputStream.readBytes().toRequestBody(mimeType?.toMediaTypeOrNull())
        }

        if (requestBody == null) {
            errorMessage = "无法读取文件数据"
            return
        }

        val filePart = MultipartBody.Part.createFormData("file", fileName, requestBody)

        viewModelScope.launch {
            isLoading = true
            try {
                val response = ApiClient.getApiService(context).uploadAttachment(taskId, filePart)
                if (response.isSuccessful) {
                    val newAttachment = response.body()
                    if (newAttachment != null) {
                        tasks = tasks.map { t ->
                            if (t.id == taskId) {
                                t.copy(attachments = t.attachments + newAttachment)
                            } else {
                                t
                            }
                        }
                        errorMessage = "附件上传成功"
                    } else {
                        errorMessage = "附件上传失败：服务器未返回有效数据。"
                    }
                } else {
                    errorMessage = "附件上传失败: ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "附件上传失败: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun downloadAttachment(taskId: Int, attachmentId: Int, fileName: String) {
        viewModelScope.launch {
            try {
                val response = ApiClient.getApiService(getApplication()).downloadAttachment(taskId, attachmentId)
                if (response.isSuccessful) {
                    response.body()?.let { body ->
                        withContext(Dispatchers.IO) {
                            saveFile(body, fileName)
                        }
                        errorMessage = "文件 '$fileName' 下载成功，已保存至下载目录。"
                    } ?: run {
                        errorMessage = "下载失败：响应体为空。"
                    }
                } else {
                    errorMessage = "下载附件失败: ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "下载附件失败: ${e.message}"
            }
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private suspend fun saveFile(body: ResponseBody, fileName: String) {
        val context = getApplication<Application>().applicationContext
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, resolver.getType(Uri.parse(fileName)))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            var outputStream: OutputStream? = null
            try {
                outputStream = resolver.openOutputStream(it)
                outputStream?.let { os ->
                    body.byteStream().use { inputStream ->
                        inputStream.copyTo(os)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                outputStream?.close()
            }
        }
    }

    fun updateAttachmentPermissions(taskId: Int, attachmentId: Int, newPermissions: Map<Int, Role>) { // <-- 修改: Key String -> Int
        viewModelScope.launch {
            isLoading = true
            try {
                val requestBody = UpdateAttachmentPermissionsRequest(newPermissions.mapValues { it.value.name })
                val response = ApiClient.getApiService(getApplication()).updateAttachmentPermissions(taskId, attachmentId, requestBody)

                if (response.isSuccessful && response.body() == true) {
                    tasks = tasks.map { task ->
                        if (task.id == taskId) {
                            val updatedAttachments = task.attachments.map { attachment ->
                                if (attachment.id == attachmentId) {
                                    attachment.copy(permissions = newPermissions)
                                } else {
                                    attachment
                                }
                            }
                            task.copy(attachments = updatedAttachments)
                        } else {
                            task
                        }
                    }
                    errorMessage = "附件权限更新成功"
                } else {
                    errorMessage = "附件权限更新失败: ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "更新附件权限时出错: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun requestJoinTeam(teamId: Int) { // <-- 修改: String -> Int
        val team = teams.find { it.id == teamId } ?: return
        if (currentUserId != null && team.members.containsKey(currentUserId)) { // <--
            errorMessage = "您已经是该团队成员"
            return
        }
        viewModelScope.launch {
            try {
                currentUserId?.let { userId ->
                    val response = ApiClient.getApiService(getApplication()).joinTeam(teamId, userId)
                    if (response.isSuccessful) {
                        teams = teams.map {
                            if (it.id == teamId) it.copy(pendingJoinRequests = it.pendingJoinRequests + userId) else it
                        }
                        errorMessage = "已提交加入团队申请"
                    } else {
                        errorMessage = "加入团队申请失败: ${response.message()}"
                    }
                } ?: run {
                    errorMessage = "用户未登录"
                }
            } catch (e: Exception) {
                errorMessage = "加入团队申请失败: ${e.message}"
            }
        }
    }

    fun approveJoinRequest(teamId: Int, userId: Int) {
        val team = teams.find { it.id == teamId } ?: return
        if (team.members[currentUserId] != Role.ADMIN.toString()) {
            errorMessage = "只有管理员可以审批加入请求"
            return
        }
        viewModelScope.launch {
            try {
                val updatedTeam = team.copy(
                    pendingJoinRequests = team.pendingJoinRequests.filter { id -> id != userId },
                    members = team.members + (userId to Role.MEMBER.toString())
                )
                val response = ApiClient.getApiService(getApplication()).updateTeam(teamId, updatedTeam)
                if (response.isSuccessful) {
                    teams = teams.map {
                        if (it.id == teamId) updatedTeam else it
                    }
                    errorMessage = "已批准用户 $userId 加入团队"
                } else {
                    errorMessage = "审批加入请求失败: ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "审批加入请求失败: ${e.message}"
            }
        }
    }

    fun assignTask(teamId: Int, taskId: Int, userId: Int) {
        val team = teams.find { it.id == teamId } ?: return
        if (team.members[currentUserId] == Role.ADMIN.toString() && team.members.containsKey(userId)) {
            viewModelScope.launch {
                try {
                    val response = ApiClient.getApiService(getApplication()).assignTask(teamId, taskId, userId)
                    if (response.isSuccessful) {
                        tasks = tasks.map {
                            if (it.id == taskId) it.copy(assignedTo = userId) else it
                        }
                        errorMessage = "任务分配成功"
                    } else {
                        errorMessage = "任务分配失败: ${response.message()}"
                    }
                } catch (e: Exception) {
                    errorMessage = "任务分配失败: ${e.message}"
                }
            }
        } else {
            errorMessage = "无权限分配任务或用户不在团队中"
        }
    }

    fun removeMemberFromTeam(teamId: Int, userId: Int) {
        val team = teams.find { it.id == teamId } ?: return
        if (team.members[currentUserId] != Role.ADMIN.toString()) {
            errorMessage = "操作失败：只有管理员才能移除成员"
            return
        }
        if (userId == currentUserId) {
            errorMessage = "操作失败：不能移除自己"
            return
        }

        viewModelScope.launch {
            try {
                val response = ApiClient.getApiService(getApplication()).removeMemberFromTeam(teamId, userId)
                if (response.isSuccessful && response.body() == true) {
                    val updatedMembers = team.members.toMutableMap().apply { remove(userId) }
                    val updatedTeam = team.copy(members = updatedMembers)
                    teams = teams.map { if (it.id == teamId) updatedTeam else it }
                    errorMessage = "成员已成功移除"
                } else {
                    errorMessage = "移除成员失败: ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "移除成员时发生错误: ${e.message}"
            }
        }
    }

    fun updateMemberRole(teamId: Int, userId: Int, newRole: Role) {
        val team = teams.find { it.id == teamId } ?: return
        if (team.members[currentUserId] != Role.ADMIN.toString()) {
            errorMessage = "操作失败：只有管理员才能更改角色"
            return
        }
        if (userId == currentUserId) {
            errorMessage = "操作失败：不能更改自己的角色"
            return
        }

        viewModelScope.launch {
            try {
                val request = UpdateMemberRoleRequest(newRole = newRole.name)
                val response = ApiClient.getApiService(getApplication()).updateMemberRole(teamId, userId, request)
                if (response.isSuccessful && response.body() == true) {
                    val updatedMembers = team.members.toMutableMap().apply { this[userId] = newRole.name }
                    val updatedTeam = team.copy(members = updatedMembers)
                    teams = teams.map { if (it.id == teamId) updatedTeam else it }
                    errorMessage = "角色已成功更新"
                } else {
                    errorMessage = "更新角色失败: ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "更新角色时发生错误: ${e.message}"
            }
        }
    }

    fun calculateParentTaskProgress(parentTaskId: Int): Float {
        val subTasks = tasks.filter { it.parentTaskId == parentTaskId }
        if (subTasks.isEmpty()) return if (tasks.find { it.id == parentTaskId }?.status == TaskStatus.DONE) 100f else 0f

        val totalWeight = subTasks.sumOf { it.weight }
        if (totalWeight == 0) return 0f

        val completedWeight = subTasks
            .filter { it.status == TaskStatus.DONE }
            .sumOf { it.weight }

        return (completedWeight.toFloat() / totalWeight.toFloat()) * 100
    }

    fun dissolveTeam(teamId: Int) {
        val team = teams.find { it.id == teamId } ?: return
        if (team.members[currentUserId] != Role.ADMIN.toString()) {
            errorMessage = "只有管理员可以解散团队"
            return
        }
        viewModelScope.launch {
            try {
                val response = ApiClient.getApiService(getApplication()).deleteTeam(teamId)
                if (response.isSuccessful) {
                    tasks = tasks.filter { it.teamId != teamId }
                    teams = teams.filter { it.id != teamId }
                    errorMessage = "团队解散成功"
                } else {
                    errorMessage = "解散团队失败: ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "解散团队失败: ${e.message}"
            }
        }
    }

    fun calculateTeamProgress(teamId: Int): Float {
        val team = teams.find { it.id == teamId } ?: return 0f
        val teamTasks = tasks.filter { it.teamId == teamId && it.parentTaskId == null }
        if (teamTasks.isEmpty()) return 0f
        val totalWeight = teamTasks.sumOf { it.weight }
        val completedWeight = teamTasks.filter { it.status == TaskStatus.DONE }.sumOf { it.weight }
        return if (totalWeight > 0) (completedWeight.toFloat() / totalWeight) * 100 else 0f
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            try {
                val response = ApiClient.getApiService(getApplication()).updateTask(task.id, task)
                if (response.isSuccessful && response.body() == true) {
                    tasks = tasks.map { if (it.id == task.id) task else it }
                    errorMessage = "任务更新成功"
                } else {
                    errorMessage = "任务更新失败: ${response.message()}"
                }
            } catch (e: Exception) {
                errorMessage = "任务更新失败: ${e.message}"
            }
        }
    }

    fun markTaskAsInProgress(task: Task) {
        val updatedTask = task.copy(status = TaskStatus.IN_PROGRESS)
        updateTask(updatedTask)
    }
    fun canCurrentUserModifyTask(task: Task): Boolean {
        // 规则1：个人任务，总是有权限
        if (!task.isTeamTask || task.teamId == null) {
            return true
        }

        // 规则2：团队任务，需要检查角色和分配情况
        val team = teams.find { it.id == task.teamId } ?: return false // 找不到团队则无法判断
        val userRole = team.members[currentUserId]

        // 规则2a：团队管理员总是有权限
        if (userRole == Role.ADMIN.toString()) {
            return true
        }

        // 规则2b & 2c：普通成员的权限
        if (userRole == Role.MEMBER.toString()) {
            // 如果任务分配给了自己，或任务未分配，则有权限
            if (task.assignedTo == currentUserId || task.assignedTo == null) {
                return true
            }
        }

        // 其他所有情况均无权限
        return false
    }

}