package com.example.hudoverlay // استبدل هذا بحزمة المشروع لديك

import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var params: WindowManager.LayoutParams

    // مصفوفة لتخزين حالة الـ 12 زرار أداة (تشغيل / إيقاف)
    private val toolStates = BooleanArray(12) { false }

    // متغيرات حفظ حالات أقسام التحكم السفلية
    private var boundingBoxMode = "off"
    private var radarLineMode = "off"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val LAYOUT_FLAG = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            LAYOUT_FLAG,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        // نفخ الواجهة من ملف الـ XML المحدث
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_panel, null)

        // تفعيل حركة السحب والإغلاق
        setupDragging()

        // ربط وتفعيل الـ 12 زرار بالترتيب الصحيح
        setupToolButtons()

        // ربط وتفعيل أزرار التحكم السفلي (Bounding Box و Radar Line)
        setupControlGroups()

        windowManager.addView(overlayView, params)
    }

    private fun setupDragging() {
        val dragHandle = overlayView.findViewById<View>(R.id.dragHandle)
        dragHandle?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(overlayView, params)
                        return true
                    }
                }
                return false
            }
        })

        overlayView.findViewById<ImageButton>(R.id.btnMinimize)?.setOnClickListener {
            stopSelf()
        }
    }

    private fun setupToolButtons() {
        // مصفوفة الـ IDs للـ 12 زرار مطابقة لملف الـ XML تماماً
        val toolButtonIds = intArrayOf(
            R.id.btnTool0,  // إطار الأعداء
            R.id.btnTool1,  // شريط الصحة
            R.id.btnTool2,  // أسماء اللاعبين
            R.id.btnTool3,  // المسافة
            R.id.btnTool4,  // الهيكل العظمي
            R.id.btnTool5,  // نقطة الرأس
            R.id.btnTool6,  // السلاح المستخدم
            R.id.btnTool7,  // خطوط الرادار
            R.id.btnTool8,  // تحذير القنابل
            R.id.btnTool9,  // تنبيه الحواف
            R.id.btnTool10, // رقم الفريق
            R.id.btnTool11  // عدد الاعداء
        )

        for (i in toolButtonIds.indices) {
            val button = overlayView.findViewById<Button>(toolButtonIds[i])
            button?.setOnClickListener {
                toolStates[i] = !toolStates[i]
                updateToolButtonVisual(button, toolStates[i])
                performToolAction(i, toolStates[i])
            }
        }
    }

    private fun updateToolButtonVisual(button: Button, isActive: Boolean) {
        if (isActive) {
            button.setBackgroundColor(Color.parseColor("#4CAF50")) // أخضر عند التفعيل
            button.setTextColor(Color.WHITE)
        } else {
            button.setBackgroundResource(R.drawable.bg_toggle_off)
            button.setTextColor(Color.parseColor("#CCCCCC"))
        }
    }

    private fun performToolAction(index: Int, isActive: Boolean) {
        val toolName = when (index) {
            0 -> "إطار الأعداء"
            1 -> "شريط الصحة"
            2 -> "أسماء اللاعبين"
            3 -> "المسافة"
            4 -> "الهيكل العظمي"
            5 -> "نقطة الرأس"
            6 -> "السلاح المستخدم"
            7 -> "خطوط الرادار"
            8 -> "تحذير القنابل"
            9 -> "تنبيه الحواف"
            10 -> "رقم الفريق"
            11 -> "عدد الاعداء"
            else -> "أداة $index"
        }
        val statusText = if (isActive) "تم التفعيل" else "تم الإيقاف"
        Toast.makeText(this, "$toolName: $statusText", Toast.LENGTH_SHORT).show()
    }

    private fun setupControlGroups() {
        // --- مجموعة Bounding box (Off / Filled / Precise) ---
        val btnBoxOff = overlayView.findViewById<Button>(R.id.btnBoxOff)
        val btnBoxFilled = overlayView.findViewById<Button>(R.id.btnBoxFilled)
        val btnBoxPrecise = overlayView.findViewById<Button>(R.id.btnBoxPrecise)

        fun updateBoxUI(mode: String) {
            boundingBoxMode = mode
            // تحديث الألوان بصرياً (مثال: الأخضر للمحدد والباقي عادي)
            btnBoxOff?.setBackgroundColor(if (mode == "off") Color.parseColor("#4CAF50") else Color.DKGRAY)
            btnBoxFilled?.setBackgroundColor(if (mode == "filled") Color.parseColor("#4CAF50") else Color.DKGRAY)
            btnBoxPrecise?.setBackgroundColor(if (mode == "precise") Color.parseColor("#4CAF50") else Color.DKGRAY)
        }

        btnBoxOff?.setOnClickListener {
            updateBoxUI("off")
            Toast.makeText(this, "Bounding box: Off", Toast.LENGTH_SHORT).show()
        }
        btnBoxFilled?.setOnClickListener {
            updateBoxUI("filled")
            Toast.makeText(this, "Bounding box: Filled", Toast.LENGTH_SHORT).show()
        }
        btnBoxPrecise?.setOnClickListener {
            updateBoxUI("precise")
            Toast.makeText(this, "Bounding box: Precise", Toast.LENGTH_SHORT).show()
        }
        updateBoxUI("off") // الحالة الافتراضية

        // --- مجموعة Radar line (Off / Top / Bottom) ---
        val btnRadarOff = overlayView.findViewById<Button>(R.id.btnRadarOff)
        val btnRadarTop = overlayView.findViewById<Button>(R.id.btnRadarTop)
        val btnRadarBottom = overlayView.findViewById<Button>(R.id.btnRadarBottom)

        fun updateRadarUI(mode: String) {
            radarLineMode = mode
            btnRadarOff?.setBackgroundColor(if (mode == "off") Color.parseColor("#4CAF50") else Color.DKGRAY)
            btnRadarTop?.setBackgroundColor(if (mode == "top") Color.parseColor("#4CAF50") else Color.DKGRAY)
            btnRadarBottom?.setBackgroundColor(if (mode == "bottom") Color.parseColor("#4CAF50") else Color.DKGRAY)
        }

        btnRadarOff?.setOnClickListener {
            updateRadarUI("off")
            Toast.makeText(this, "Radar line: Off", Toast.LENGTH_SHORT).show()
        }
        btnRadarTop?.setOnClickListener {
            updateRadarUI("top")
            Toast.makeText(this, "Radar line: Top", Toast.LENGTH_SHORT).show()
        }
        btnRadarBottom?.setOnClickListener {
            updateRadarUI("bottom")
            Toast.makeText(this, "Radar line: Bottom", Toast.LENGTH_SHORT).show()
        }
        updateRadarUI("off") // الحالة الافتراضية
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::overlayView.isInitialized) {
            windowManager.removeView(overlayView)
        }
    }
}
