package com.muhgoub.hud

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class OverlayService : Service() {

    companion object {
        private const val CHANNEL_ID = "muhgoub_hud_channel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "com.muhgoub.hud.action.STOP"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var prefs: PrefsManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var bubbleView: View? = null
    private var panelView: View? = null
    private var bubbleParams: WindowManager.LayoutParams? = null
    private var panelParams: WindowManager.LayoutParams? = null

    private val toggleButtons = arrayOfNulls<Button>(12)

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        prefs = PrefsManager(this)

        startForeground(NOTIFICATION_ID, buildNotification())

        addBubbleView()
        addPanelView()

        setPanelVisible(prefs.isOverlayExpanded())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) stopSelf()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        bubbleView?.let { runCatching { windowManager.removeView(it) } }
        panelView?.let { runCatching { windowManager.removeView(it) } }
        super.onDestroy()
    }

    private fun buildNotification(): android.app.Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }
        val stopIntent = Intent(this, OverlayService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.panel_running))
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .addAction(0, getString(R.string.close_panel), stopPendingIntent)
            .build()
    }

    private fun addBubbleView() {
        val view = View.inflate(this, R.layout.overlay_bubble, null)
        val (savedX, savedY) = prefs.getOverlayPosition()

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX
            y = savedY
        }

        view.setOnTouchListener(DragTapListener(params) { tapped ->
            if (tapped) togglePanelVisibility()
        })

        windowManager.addView(view, params)
        bubbleView = view
        bubbleParams = params
    }

    private fun togglePanelVisibility() {
        setPanelVisible(panelView?.visibility != View.VISIBLE)
    }

    private fun setPanelVisible(visible: Boolean) {
        prefs.setOverlayExpanded(visible)
        panelView?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    private fun addPanelView() {
        val view = View.inflate(this, R.layout.overlay_panel, null)

        val panelWidthPx = cmToPx(6f)
        val panelHeightPx = cmToPx(6f)

        val params = WindowManager.LayoutParams(
            panelWidthPx,
            panelHeightPx,
            overlayWindowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val (savedX, savedY) = prefs.getOverlayPosition()
            x = savedX
            y = savedY
        }

        val dragHandle = view.findViewById<View>(R.id.dragHandle)
        dragHandle.setOnTouchListener(DragTapListener(params) { })

        view.findViewById<ImageButton>(R.id.btnMinimize).setOnClickListener {
            setPanelVisible(false)
        }

        bindToolButtons(view)
        bindControlGroups(view)

        windowManager.addView(view, params)
        panelView = view
        panelParams = params
    }

    private fun bindToolButtons(root: View) {
        val ids = intArrayOf(
            R.id.btnTool0, R.id.btnTool1, R.id.btnTool2, R.id.btnTool3,
            R.id.btnTool4, R.id.btnTool5, R.id.btnTool6, R.id.btnTool7,
            R.id.btnTool8, R.id.btnTool9, R.id.btnTool10, R.id.btnTool11
        )

        for (i in ids.indices) {
            val button = root.findViewById<Button>(ids[i])
            toggleButtons[i] = button
            applyToggleStyle(button, prefs.isToolOn(i))

            button.setOnClickListener {
                val newState = !prefs.isToolOn(i)
                prefs.setToolOn(i, newState)
                applyToggleStyle(button, newState)
                performToolAction(i, newState)
            }
        }
    }

    private fun applyToggleStyle(button: Button, on: Boolean) {
        val checkIcon = if (on) R.drawable.ic_check_on else R.drawable.ic_check_off
        button.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, checkIcon, 0)
        button.setBackgroundResource(if (on) R.drawable.bg_toggle_on else R.drawable.bg_toggle_off)
        val textColorRes = if (on) R.color.bg_dark else R.color.text_primary
        button.setTextColor(resources.getColor(textColorRes, theme))
    }

    private fun bindControlGroups(root: View) {
        // مجموع أزرار Bounding Box
        val boxButtons = mapOf(
            root.findViewById<Button>(R.id.btnBoxOff) to "off",
            root.findViewById<Button>(R.id.btnBoxFilled) to "filled",
            root.findViewById<Button>(R.id.btnBoxPrecise) to "precise"
        )
        
        fun refreshBoxUI(selected: String) {
            boxButtons.forEach { (btn, value) ->
                val active = (value == selected)
                btn?.setBackgroundResource(if (active) R.drawable.bg_toggle_on else R.drawable.bg_toggle_off)
            }
        }
        
        // افتراض قيمة أولية لو مش محفوظة
        val currentBox = prefs.getString("box_mode", "off")
        refreshBoxUI(currentBox)
        boxButtons.forEach { (btn, value) ->
            btn?.setOnClickListener {
                prefs.setString("box_mode", value)
                refreshBoxUI(value)
                toast("Bounding Box: $value")
            }
        }

        // مجموع أزرار Radar Line
        val radarButtons = mapOf(
            root.findViewById<Button>(R.id.btnRadarOff) to "off",
            root.findViewById<Button>(R.id.btnRadarTop) to "top",
            root.findViewById<Button>(R.id.btnRadarBottom) to "bottom"
        )

        fun refreshRadarUI(selected: String) {
            radarButtons.forEach { (btn, value) ->
                val active = (value == selected)
                btn?.setBackgroundResource(if (active) R.drawable.bg_toggle_on else R.drawable.bg_toggle_off)
            }
        }

        val currentRadar = prefs.getString("radar_mode", "off")
        refreshRadarUI(currentRadar)
        radarButtons.forEach { (btn, value) ->
            btn?.setOnClickListener {
                prefs.setString("radar_mode", value)
                refreshRadarUI(value)
                toast("Radar Line: $value")
            }
        }
    }

    private fun overlayWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private fun cmToPx(cm: Float): Int =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, cm * 10f, resources.displayMetrics).toInt()

    private fun performToolAction(index: Int, on: Boolean) {
        val toolNames = arrayOf(
            "إطار الأعداء", "شريط الصحة", "أسماء اللاعبين", "المسافة",
            "الهيكل العظمي", "نقطة الرأس", "السلاح المستخدم", "خطوط الرادار",
            "تحذير القنابل", "تنبيه الحواف", "رقم الفريق", "عدد الاعداء"
        )
        val name = if (index in toolNames.indices) toolNames[index] else "أداة $index"
        toast("$name: ${if (on) "تم التفعيل" else "تم الإيقاف"}")
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private inner class DragTapListener(
        private val params: WindowManager.LayoutParams,
        private val onTap: (Boolean) -> Unit
    ) : View.OnTouchListener {

        private var initialX = 0
        private var initialY = 0
        private var initialTouchX = 0f
        private var initialTouchY = 0f
        private var moved = false

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moved = false
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()

                    if (abs(dx) > 5 || abs(dy) > 5) {
                        moved = true
                    }

                    params.x = initialX + dx
                    params.y = initialY + dy

                    bubbleParams?.let {
                        it.x = params.x
                        it.y = params.y
                        bubbleView?.let { bv -> runCatching { windowManager.updateViewLayout(bv, it) } }
                    }
                    panelParams?.let {
                        it.x = params.x
                        it.y = params.y
                        panelView?.let { pv -> runCatching { windowManager.updateViewLayout(pv, it) } }
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    prefs.setOverlayPosition(params.x, params.y)
                    onTap(!moved)
                    return true
                }
            }
            return false
        }
    }
}
