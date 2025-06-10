package com.example.software_project

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeParseException
import java.time.LocalTime
import java.util.Calendar

/**
 * 负责调度、取消和显示任务提醒通知的工具类
 */
object NotificationScheduler {

    const val NOTIFICATION_CHANNEL_ID = "task_reminder_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Task Reminders"
    private const val DAILY_REMINDER_REQUEST_CODE_PREFIX = 2000000000

    // ... (此对象中的其他函数 scheduleNotifications, cancelNotifications 等保持不变) ...
    fun scheduleNotifications(context: Context, task: Task) {
        val reminderSettings = task.reminderSettings ?: return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        try {
            val dueDate = LocalDateTime.parse(task.dueDate + "T23:59:59")
            reminderSettings.remindAtTimes.forEachIndexed { index, reminderTime ->
                val triggerTime = calculateTriggerTime(dueDate, reminderTime)
                if (triggerTime.isAfter(LocalDateTime.now())) {
                    val pendingIntent = createPendingIntent(context, task, index, reminderSettings.reminderMethods.map { it.name })
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                        Toast.makeText(context, "需要精确闹钟权限来设置提醒", Toast.LENGTH_LONG).show()
                        return
                    }
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        pendingIntent
                    )
                }
            }
        } catch (e: DateTimeParseException) {
            // 日期格式错误，忽略此部分提醒
        }

        reminderSettings.dailyReminderTime?.let { timeString ->
            try {
                val time = LocalTime.parse(timeString)
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, time.hour)
                    set(Calendar.MINUTE, time.minute)
                    set(Calendar.SECOND, 0)
                }
                if (calendar.timeInMillis < System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1)
                }

                val requestCode = DAILY_REMINDER_REQUEST_CODE_PREFIX + task.id.hashCode()
                val pendingIntent = createPendingIntent(context, task, requestCode, reminderSettings.reminderMethods.map { it.name })

                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            } catch (e: Exception) {
                // 时间格式解析失败
            }
        }
    }

    fun cancelNotifications(context: Context, task: Task) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val reminderSettings = task.reminderSettings ?: return

        reminderSettings.remindAtTimes.forEachIndexed { index, _ ->
            val pendingIntent = createPendingIntent(context, task, index, null, cancel = true)
            alarmManager.cancel(pendingIntent)
        }

        if (reminderSettings.dailyReminderTime != null) {
            val requestCode = DAILY_REMINDER_REQUEST_CODE_PREFIX + task.id.hashCode()
            val pendingIntent = createPendingIntent(context, task, requestCode, null, cancel = true)
            alarmManager.cancel(pendingIntent)
        }
    }

    private fun createPendingIntent(context: Context, task: Task, requestCode: Int, methods: List<String>?, cancel: Boolean = false): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("task_id", task.id)
            putExtra("task_title", task.title)
            putExtra("task_description", task.description)
            methods?.let {
                putStringArrayListExtra("reminder_methods", ArrayList(it))
            }
        }

        val flags = if (cancel) {
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        }

        // 使用唯一的requestCode来创建或取消PendingIntent
        val finalRequestCode = if (requestCode >= DAILY_REMINDER_REQUEST_CODE_PREFIX) {
            requestCode
        } else {
            // 对于提前提醒，确保每个任务的每个提醒都有唯一的ID
            (task.id.hashCode() + requestCode)
        }


        return PendingIntent.getBroadcast(context, finalRequestCode, intent, flags)!!
    }

    private fun calculateTriggerTime(dueDate: LocalDateTime, reminderString: String): LocalDateTime {
        val duration = when {
            reminderString.endsWith("d") -> Duration.ofDays(reminderString.removeSuffix("d").toLongOrNull() ?: 0)
            reminderString.endsWith("h") -> Duration.ofHours(reminderString.removeSuffix("h").toLongOrNull() ?: 0)
            reminderString.endsWith("m") -> Duration.ofMinutes(reminderString.removeSuffix("m").toLongOrNull() ?: 0)
            else -> Duration.ZERO
        }
        return dueDate.minus(duration)
    }
}

/**
 * 接收闹钟广播并根据提醒方式显示通知 (这是您需要更新的部分)
 */
class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra("task_id") ?: return
        val title = intent.getStringExtra("task_title") ?: "任务提醒"
        val description = intent.getStringExtra("task_description") ?: "您有一个任务即将到期"
        val methods = intent.getStringArrayListExtra("reminder_methods") ?: arrayListOf("IN_APP")

        methods.forEach { methodName ->
            try {
                when (ReminderMethod.valueOf(methodName)) {
                    ReminderMethod.IN_APP, ReminderMethod.FLOAT_WINDOW -> {
                        showSystemNotification(context, title, description)
                    }
                    ReminderMethod.SMS -> {
                        // 假设用户的手机号保存在名为 "auth" 的 SharedPreferences 文件中
                        val sharedPreferences = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
                        val phoneNumber = sharedPreferences.getString("user_phone", null) // 假设键为 "user_phone"

                        if (!phoneNumber.isNullOrBlank()) {
                            val message = "任务提醒: $title. 详情: $description"
                            sendSmsReminder(context, phoneNumber, message)
                        } else {
                            showSystemNotification(context, "短信提醒失败", "未设置用户手机号")
                        }
                    }
                    ReminderMethod.EMAIL -> {
                        // 假设用户的邮箱地址也保存在 "auth" 的 SharedPreferences 中
                        val sharedPreferences = context.getSharedPreferences("auth", Context.MODE_PRIVATE)
                        val emailAddress = sharedPreferences.getString("user_email", null) // 假设键为 "user_email"

                        if (!emailAddress.isNullOrBlank()){
                            val subject = "任务提醒: $title"
                            val body = "您好,\n\n您有一个任务即将到期。\n\n标题: $title\n详情: $description\n\n请及时处理。"
                            sendEmailReminder(context, emailAddress, subject, body)
                        } else {
                            showSystemNotification(context, "邮件提醒失败", "未设置用户邮箱")
                        }
                    }
                }
            } catch (e: IllegalArgumentException) {
                // 忽略无效的提醒方式，例如，如果枚举中删除了某个值但旧的提醒还在
            }
        }
    }

    @SuppressLint("MissingPermission", "ObsoleteSdkInt")
    private fun showSystemNotification(context: Context, title: String, description: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NotificationScheduler.NOTIFICATION_CHANNEL_ID,
                NotificationScheduler.NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                this.description = "Channel for task reminder notifications."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(context, NotificationScheduler.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        notificationManager.notify(title.hashCode(), builder.build())
    }

    /**
     * 新增: 发送短信提醒的函数
     */
    @SuppressLint("MissingPermission")
    private fun sendSmsReminder(context: Context, phoneNumber: String, message: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            try {
                // 为了兼容性，使用 getSystemService 获取 SmsManager
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            } catch (e: Exception) {
                showSystemNotification(context, "短信发送失败", "无法发送提醒至 $phoneNumber")
            }
        } else {
            showSystemNotification(context, "短信权限不足", "请授予应用发送短信的权限")
        }
    }

    /**
     * 新增: 发送邮件提醒的函数 (通过邮件客户端)
     */
    private fun sendEmailReminder(context: Context, emailAddress: String, subject: String, body: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            // 检查是否有应用可以处理这个 Intent
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                showSystemNotification(context, "邮件应用未找到", "无法打开邮件客户端")
            }
        } catch (e: Exception) {
            showSystemNotification(context, "邮件提醒失败", "启动邮件应用时出错")
        }
    }
}