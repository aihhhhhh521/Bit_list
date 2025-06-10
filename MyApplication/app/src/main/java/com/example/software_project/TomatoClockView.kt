package com.example.software_project

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.os.CountDownTimer
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class TomatoClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class TimerState {
        IDLE, WORKING, BREAK
    }
    private var currentState = TimerState.IDLE
        set(value) {
            if (field != value) {
                field = value
                stateListener?.onStateChanged(value)
            }
        }

    companion object {
        const val channelId = "tomato_clock_channel"
        private const val TAG = "TomatoClockView"
        private val WORK_DURATION_MS = 25 * 60 * 1000L
        private val BREAK_DURATION_MS = 5 * 60 * 1000L

        @SuppressLint("DefaultLocale")
        private fun formatCountTime(timeInMillis: Long): String {
            val totalSeconds = timeInMillis / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%02d:%02d", minutes, seconds)
        }
    }

    private var arcPaint: Paint
    private var textPaint: Paint
    private val backgroundColor = Color.parseColor("#D1D1D1")
    private val workColor = Color.BLUE
    private val breakColor = Color.GREEN
    private var textTime = formatCountTime(WORK_DURATION_MS)
    private var sweepVelocity = 0f
    private var valueAnimator: ValueAnimator
    private var mTimeCounter: CountDownTimer? = null
    private var currentTimerDuration = WORK_DURATION_MS
    var isStarted: Boolean = false
        private set
    var isPaused: Boolean = false
        private set
    private var focusDataListener: FocusDataListener? = null
    private var stateListener: TimerStateListener? = null

    interface FocusDataListener {
        fun onFocusCompleted(durationInSeconds: Long)
    }

    interface TimerStateListener {
        fun onStateChanged(newState: TimerState)
    }

    init {
        arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 30f }
        textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; strokeWidth = 20f; textSize = 180f; textAlign = Paint.Align.CENTER }
        valueAnimator = ValueAnimator.ofFloat(0f, 1f).apply { interpolator = android.view.animation.LinearInterpolator(); addUpdateListener { animation -> sweepVelocity = animation.animatedValue as Float; invalidate() } }
        createNotificationChannel()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = (width / 2).toFloat(); val centerY = (height / 2).toFloat(); val radius = (Math.min(width, height) / 2f) - arcPaint.strokeWidth; val rectF = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
        arcPaint.color = backgroundColor; canvas.drawArc(rectF, -90f, 360f, false, arcPaint)
        arcPaint.color = if (currentState == TimerState.WORKING) workColor else breakColor; val sweepAngle = 360 * sweepVelocity; canvas.drawArc(rectF, -90f, sweepAngle, false, arcPaint)
        val metrics = textPaint.fontMetrics; val baseline = centerY - (metrics.ascent + metrics.descent) / 2; canvas.drawText(textTime, centerX, baseline, textPaint)
    }

    override fun onTouchEvent(event: android.view.MotionEvent?): Boolean { return true }

    private inner class MyTimer(
        millisInFuture: Long,
        countDownInterval: Long
    ) : CountDownTimer(millisInFuture, countDownInterval) {
        override fun onTick(millisUntilFinished: Long) { textTime = formatCountTime(millisUntilFinished) }
        override fun onFinish() {
            when (currentState) {
                TimerState.WORKING -> {
                    Log.d(TAG, "Work session finished.")
                    sendNotification("专注结束！", "准备休息5分钟。")
                    focusDataListener?.onFocusCompleted(WORK_DURATION_MS / 1000)
                    currentState = TimerState.BREAK
                    start()
                }
                TimerState.BREAK -> {
                    Log.d(TAG, "Break session finished.")
                    sendNotification("休息结束", "开始新的专注吧！")
                    stop()
                }
                else -> {}
            }
        }
    }

    fun start() {
        if (isStarted) return
        if (currentState == TimerState.IDLE) {
            currentState = TimerState.WORKING
        }
        currentTimerDuration = if (currentState == TimerState.WORKING) WORK_DURATION_MS else BREAK_DURATION_MS
        valueAnimator.duration = currentTimerDuration
        valueAnimator.start()
        mTimeCounter = MyTimer(currentTimerDuration, 1000)
        mTimeCounter?.start()
        isStarted = true
        isPaused = false
        Log.d(TAG, "Timer started in state: $currentState")
    }

    fun pause() {
        if (!isStarted || isPaused) return
        mTimeCounter?.cancel()
        valueAnimator.pause()
        isPaused = true
        sendNotification("专注已暂停", "返回应用以继续。")
        Log.d(TAG, "Timer paused.")
    }

    fun resume() {
        if (!isStarted || !isPaused) return
        val remainingTime = ((1 - sweepVelocity) * currentTimerDuration).toLong()
        mTimeCounter = MyTimer(remainingTime, 1000)
        mTimeCounter?.start()
        valueAnimator.resume()
        isPaused = false
        Log.d(TAG, "Timer resumed.")
    }

    // --- 修改点 ---
    fun stop() {
        if (!isStarted) return

        if (currentState == TimerState.WORKING) {
            val elapsedTime = (sweepVelocity * currentTimerDuration).toLong()
            if (elapsedTime > 1000) {
                focusDataListener?.onFocusCompleted(elapsedTime / 1000)
            }
        }

        // 修改通知文本使其更通用
        sendNotification("专注已停止", "本次专注已结束，时长已记录。")

        mTimeCounter?.cancel()
        valueAnimator.cancel()
        isStarted = false
        isPaused = false
        sweepVelocity = 0f
        textTime = formatCountTime(WORK_DURATION_MS)
        currentState = TimerState.IDLE
        invalidate()
        Log.d(TAG, "Timer stopped and reset.")
    }

    fun setFocusDataListener(listener: FocusDataListener?) { this.focusDataListener = listener }
    fun setStateListener(listener: TimerStateListener?) { this.stateListener = listener }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "番茄钟通知"; val descriptionText = "用于番茄钟完成和提醒的通知"; val importance = NotificationManager.IMPORTANCE_HIGH; val channel = NotificationChannel(channelId, name, importance).apply { description = descriptionText }; val notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager; notificationManager.createNotificationChannel(channel)
        }
    }
    @SuppressLint("MissingPermission")
    private fun sendNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(context, channelId).setSmallIcon(R.drawable.ic_launcher_foreground).setContentTitle(title).setContentText(message).setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true)
        with(NotificationManagerCompat.from(context)) { notify(System.currentTimeMillis().toInt(), builder.build()) }
    }
}