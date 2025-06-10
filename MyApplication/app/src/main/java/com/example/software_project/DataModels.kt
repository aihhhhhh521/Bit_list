package com.example.software_project

// 用户注册请求
data class RegisterRequest(
    val user: User,
    val password: String,
    val code: String
)

// 用户注册响应
data class RegisterResponse(
    val userId: Int,
    val token: String
)

// 验证码验证请求
data class VerifyCodeRequest(
    val email: String
)

// 登录请求
data class LoginRequest(
    val email: String,
    val password: String
)

// 登录响应
data class LoginResponse(
    val userId: Int,
    val token: String
)

//----------修改密码请求----------
data class ChangePasswordRequest(
    val userId: Int,
    val oldPassword: String,
    val newPassword: String
)
//-----------------------------

// 更新用户资料请求
data class UpdateProfileRequest(
    val userId: Int,
    val name: String,
    val email: String,
    val grade: String,
    val birth: String,
    val stuId: String,
    val school: String, // 新增字段
    val avatarUri: String? = null
)

// --- 新增：为图表定义的数据点模型 ---

// 用于描述“完成率趋势折线图”的单个数据点
// period可以是 "W22" (第22周) 或 "Jun" (六月)
// rate是完成率，例如 0.85 表示 85%
data class CompletionRateDataPoint(
    val period: String,
    val rate: Float
)

// --- 新增：用于时间分配报告的单个数据点 ---
data class TimeAllocationDataPoint(
    val category: String, // e.g., "学习", "会议"
    val percentage: Float // e.g., 0.35 for 35%
)

// 用于描述“任务状态统计”的数据
data class TaskStatusSummary(
    val todo: Int,
    val inProgress: Int,
    val completed: Int
)

// 用户统计数据响应
data class UserStatsResponse(
    val completedTasks: Int,
    val inProgressTasks: Int,
    val totalTeams: Int,
    val totalFocusTime: Long, // Total focus time in seconds

    // 新增的图表专用数据
    val completionRateTrend: List<CompletionRateDataPoint>, // 用于完成率趋势折线图
    val recentTaskSummary: TaskStatusSummary,              // 用于任务状态分布饼图/柱状图
    val timeAllocationReport: List<TimeAllocationDataPoint>, // 时间分配报告
    val suggestions: List<String> // 优化建议列表
    // --- 新增字段结束 ---

)

// 用户数据模型
data class User(
    val id: Int,
    val name: String,
    val email: String,
    val grade: String,
    val birth: String,
    val stuId: String,
    val school: String, // 新增字段
    val avatarUri: String? = null
)

//-----------新增接口-------------
data class ChecklistItem(
    val id: Int,
    val text: String,
    var isCompleted: Boolean
)
//------------------------------

// 任务数据模型
data class Task(
    val id: Int,
    val title: String,
    val description: String,
    val priority: Priority,
    val tags: List<String>,
    val status: TaskStatus,
    val dueDate: String,
    val order: Int,

    //-----------新添加内容------------
    val checklist: List<ChecklistItem> = emptyList(), // 新增
    val isRecurring: Boolean = false,
    val recurringType: RecurringType? = null, // e.g., WEEKLY, MONTHLY
    val recurringEndDate: String? = null, // Date when the recurrence stops
    val recurringOnDays: List<Int>? = null, // e.g., [1, 3, 5] for Mon, Wed, Fri
    val reminderSettings: ReminderSettings? = null,

    val deletedAt: String? = null,// --- 新增字段：用于记录任务的删除时间 ---

    //-------------------------------

    val isDeleted: Boolean = false,
    val attachments: List<Attachment> = emptyList(),
    val assignedTo: Int? = null,
    val isTeamTask: Boolean = false,
    val teamId: Int? = null,
    val parentTaskId: Int? = null,
    val weight: Int = 1
)

data class ReminderSettings(
    val reminderMethods: List<ReminderMethod>, // e.g., IN_APP, EMAIL
    val remindAtTimes: List<String>, // e.g., ["1d", "1h"] for 1 day and 1 hour before

    // --- 新增字段：用于支持更复杂的提醒规则 ---
    val dailyReminderTime: String?, // "HH:mm"格式, e.g., "09:00"
    val remindOnAppOpen: Boolean,
    // --- 新增字段结束 ---

    val isRecurring: Boolean = false,
    val recurringDaysOfWeek: List<Int>? = null // 1-7 for Mon-Sun
)

// --- 新增数据类：用于更新附件权限 ---
data class UpdateAttachmentPermissionsRequest(
    val permissions: Map<Int, String> // Key: userId, Value: Role (ADMIN/MEMBER)
)
// --- 新增数据类结束 ---

// 附件数据模型
data class Attachment(
    val id: Int,
    val fileName: String,
    val sizeInBytes: Long,
    val isDeleted: Boolean = false,
    val attachmentLink: String? = null,

    //-----------新添加内容------------
    val permissions: Map<Int, Role>? = null, // Key: userId, Value: Role (ADMIN/MEMBER)
    val file: String? = null, // 新增 file 字段，设为可空以增加兼容性
    val deletedAt: String? = null// --- 新增：为附件添加删除时间戳 ---
)

// 团队数据模型
data class Team(
    val id: Int,
    val name: String,
    val description: String,
    val members: Map<Int, String>,
    val tasks: List<Int>,
    val pendingJoinRequests: List<Int> = emptyList()
)

// --- 新增数据类 ---
data class UpdateMemberRoleRequest(
    val newRole: String
)
// --- 新增数据类结束 ---

// 番茄钟数据模型
data class TomatoFocusData(
    val userId: Int,
    val durationInSeconds: Long,
    val timestamp: String,
    //----------新增taskId-----------
    val taskId: Int
    // ISO 8601 格式，例如 "2025-06-05T12:00:00Z"
)

enum class Role { ADMIN, MEMBER }
enum class Priority { LOWEST, LOW, MEDIUM, HIGH, HIGHEST }
enum class TaskStatus { TODO, IN_PROGRESS, DONE, EXPIRED }
enum class RecurringType {
    DAILY, WEEKLY, MONTHLY
}
enum class ReminderMethod {
    IN_APP, FLOAT_WINDOW, SMS, EMAIL
}