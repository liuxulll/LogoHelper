package com.example.logohelper

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import kotlin.math.abs

/**
 * 悬浮窗前台服务：
 *  - Android 8+ 通知渠道 + startForegroundService 启动
 *  - Android 13+ 通知权限（无权限时仍可作为前台服务运行，仅不展示通知）
 *  - Android 14+ 需要 FOREGROUND_SERVICE_TYPE_SPECIAL_USE
 *  - TYPE_APPLICATION_OVERLAY（API 26 起统一类型），FLAG_NOT_FOCUSABLE
 *  - header 拖动、折叠为小圆点、点击关闭
 */
class OverlayService : Service() {

    companion object {
        const val ACTION_SHOW = "com.example.logohelper.ACTION_SHOW"
        const val ACTION_DISMISS = "com.example.logohelper.ACTION_DISMISS"
        const val EXTRA_HOMEWORK = "extra_homework"

        private const val CHANNEL_ID = "logo_helper_channel"
        private const val NOTIFICATION_ID = 1001

        fun startShow(context: Context, homework: ParsedHomework) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_SHOW
                putExtra(EXTRA_HOMEWORK, homework)
            }
            // Android 8+ 必须 startForegroundService
            context.startForegroundService(intent)
        }

        fun dismiss(context: Context) {
            val intent = Intent(context, OverlayService::class.java).apply {
                action = ACTION_DISMISS
            }
            context.startService(intent)
        }
    }

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var dotView: View? = null
    private val layoutParams = WindowManager.LayoutParams().apply {
        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        format = PixelFormat.TRANSLUCENT
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
        gravity = Gravity.TOP or Gravity.START
        x = 0
        y = 120
    }
    private val dotParams = WindowManager.LayoutParams().apply {
        type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        format = PixelFormat.TRANSLUCENT
        width = dp(52)
        height = dp(52)
        gravity = Gravity.TOP or Gravity.START
        x = 0
        y = 600
    }

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var lastRawX = 0f
    private var lastRawY = 0f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISMISS -> {
                removeViews()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SHOW -> {
                createChannel()
                startForegroundCompat(buildNotification())
                val homework = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getSerializableExtra(EXTRA_HOMEWORK, ParsedHomework::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getSerializableExtra(EXTRA_HOMEWORK) as? ParsedHomework
                }
                if (homework != null) {
                    showOverlay(homework)
                } else {
                    removeViews()
                    stopSelf()
                    return START_NOT_STICKY
                }
            }
            else -> if (overlayView == null && dotView == null) {
                stopSelf()
                return START_NOT_STICKY
            }
        }
        return START_STICKY
    }

    // ---------- 通知与前台服务 ----------

    private fun createChannel() {
        // Android 8+（minSdk=26 恒成立，但按规范仍用版本包裹）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "logo助手悬浮窗运行通知" }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("悬浮窗显示中，点击返回应用")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= 34) {
            // Android 14+ 必须声明前台服务类型
            startForeground(
                NOTIFICATION_ID, notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    // ---------- 悬浮窗视图 ----------

    private fun ensureWindowManager() {
        if (windowManager == null) {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        }
    }

    private fun showOverlay(homework: ParsedHomework) {
        ensureWindowManager()
        removeViews()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(Color.parseColor("#CC1E1E1E"), dp(12))
            setPadding(dp(14), dp(8), dp(14), dp(10))
        }

        // 标题栏：logo助手 + 折叠 + 关闭
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = getString(R.string.app_name)
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val foldBtn = TextView(this).apply {
            text = "— 收起"
            setTextColor(Color.parseColor("#9E9E9E"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dp(10), dp(4), dp(10), dp(4))
            setOnClickListener { collapseToDot(homework) }
        }
        val closeBtn = TextView(this).apply {
            text = "✕ 关闭"
            setTextColor(Color.parseColor("#EF5350"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dp(10), dp(4), dp(4), dp(4))
            setOnClickListener {
                removeViews()
                stopSelf()
            }
        }
        header.addView(title)
        header.addView(foldBtn)
        header.addView(closeBtn)

        // 拖动：touch header 区域更新 LayoutParams.x/y
        header.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.rawX
                    lastTouchY = event.rawY
                    lastRawX = layoutParams.x.toFloat()
                    lastRawY = layoutParams.y.toFloat()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    // gravity = TOP|START，直接累加偏移，坐标不会跳
                    layoutParams.x = lastRawX.toInt() + (event.rawX - lastTouchX).toInt()
                    layoutParams.y = lastRawY.toInt() + (event.rawY - lastTouchY).toInt()
                    overlayView?.let { windowManager?.updateViewLayout(it, layoutParams) }
                    true
                }
                else -> false
            }
        }

        root.addView(header)

        // 固定提示行
        val tip = TextView(this).apply {
            text = getString(R.string.overlay_tip)
            setTextColor(Color.parseColor("#9E9E9E"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setPadding(0, dp(4), 0, dp(6))
        }
        root.addView(tip)

        // 内容滚动区
        val contentColumn = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        if (homework.questions.isEmpty()) {
            contentColumn.addView(TextView(this).apply {
                text = "该作业未解析出题目。"
                setTextColor(Color.LTGRAY)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, dp(8), 0, dp(8))
            })
        } else {
            homework.questions.forEachIndexed { i, q ->
                if (i > 0) contentColumn.addView(View(this).apply {
                    setBackgroundColor(Color.parseColor("#33FFFFFF"))
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1
                    ).apply { topMargin = dp(8); bottomMargin = dp(8) }
                })
                contentColumn.addView(TextView(this).apply {
                    text = "Q${q.index + 1}  ${q.questionText}"
                    setTextColor(Color.WHITE)
                    setTypeface(typeface, Typeface.BOLD)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setLineSpacing(dp(2).toFloat(), 1.05f)
                })
                contentColumn.addView(TextView(this).apply {
                    text = "答：${q.answerText}"
                    setTextColor(Color.parseColor("#4CAF50"))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setPadding(0, dp(3), 0, 0)
                })
            }
        }

        val scroll = ScrollView(this).apply {
            addView(contentColumn)
            isVerticalScrollBarEnabled = true
        }
        val density = resources.displayMetrics.density
        val maxH = (resources.displayMetrics.heightPixels * 0.6f).toInt()
        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                maxH.coerceAtMost((resources.displayMetrics.heightPixels * 0.9f).toInt())
            )
        )

        // 长按整窗也可折叠
        root.setOnLongClickListener {
            collapseToDot(homework)
            true
        }

        // 宽度 ≈ 屏幕 85%
        layoutParams.width = (resources.displayMetrics.widthPixels * 0.85f).toInt()
        windowManager?.addView(root, layoutParams)
        overlayView = root
    }

    private fun collapseToDot(homework: ParsedHomework) {
        ensureWindowManager()
        overlayView?.let {
            runCatching { windowManager?.removeView(it) }
            overlayView = null
        }
        if (dotView != null) return
        val dot = TextView(this).apply {
            text = "L"
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#CC1976D2"))
            }
            setOnClickListener {
                removeDot()
                showOverlay(homework)
            }
            setOnTouchListener { _, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        lastTouchX = event.rawX
                        lastTouchY = event.rawY
                        lastRawX = dotParams.x.toFloat()
                        lastRawY = dotParams.y.toFloat()
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        dotParams.x = lastRawX.toInt() + (event.rawX - lastTouchX).toInt()
                        dotParams.y = lastRawY.toInt() + (event.rawY - lastTouchY).toInt()
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val moved = abs(event.rawX - lastTouchX) > dp(6) ||
                                abs(event.rawY - lastTouchY) > dp(6)
                        if (!moved) performClick()
                        true
                    }
                    else -> false
                }
            }
        }
        windowManager?.addView(dot, dotParams)
        dotView = dot
    }

    private fun removeDot() {
        dotView?.let { runCatching { windowManager?.removeView(it) } }
        dotView = null
    }

    private fun removeViews() {
        overlayView?.let { runCatching { windowManager?.removeView(it) } }
        overlayView = null
        removeDot()
    }

    override fun onDestroy() {
        removeViews()
        super.onDestroy()
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius.toFloat()
            setColor(color)
        }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
