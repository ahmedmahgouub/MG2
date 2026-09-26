package com.muhgoub.hud

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import kotlinx.coroutines.*

class ESPView(context: Context) : View(context) {

    private val boxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.GREEN
        textSize = 30f
        isAntiAlias = true
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }

    private var scope: CoroutineScope? = null
    private var isRunning = false
    private var statusMessage = "MUHGOUB ESP - INITIALIZED"
    private var currentPid = -1
    
    // مصفوفة الكاميرا الحقيقية
    private var viewMatrix = FloatArray(16)

    init {
        startLoop()
    }

    private fun startLoop() {
        if (isRunning) return
        isRunning = true
        scope = CoroutineScope(Dispatchers.Default + Job())
        scope?.launch {
            while (isRunning) {
                currentPid = MemoryUtils.findProcessId("com.tencent.ig")
                
                if (currentPid != -1) {
                    statusMessage = "PUBG PID: $currentPid | ACTIVE"
                    
                    // ملاحظة: هنا بنحدد عنوان الـ ViewMatrix الخاص بنسخة اللعبة (يتم تحديث العنوان حسب كل التحديث)
                    // مثال توضيحي لقراءة المصفوفة:
                    // viewMatrix = MemoryUtils.readMatrix(currentPid, 0x00000000L)
                    
                } else {
                    statusMessage = "WAITING FOR PUBG..."
                }

                withContext(Dispatchers.Main) {
                    invalidate()
                }
                delay(30L) // تحديث مستمر وسلس للإطارات
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // رسم حالة الكاشف والـ PID أعلى الشاشة
        canvas.drawText(statusMessage, 50f, 150f, textPaint)

        // تجربة إسقاط إحداثي تجريبي عبر الـ WorldToScreen للتأكد من سلاسة الرسم
        // لاحقاً هنا سيتم جلب إحداثيات كل لاعب حقيقي واللف عليها برمجياً
        val dummyWorldPos = MemoryUtils.Vector3(0f, 300f, 50f)
        
        // مصفوفة افتراضية مبدئية للاختبار لو الـ ViewMatrix لسه مجاش عناوينه
        val testMatrix = FloatArray(16) { 1f }
        testMatrix[0] = 1f; testMatrix[5] = 1f; testMatrix[10] = 1f; testMatrix[15] = 1f

        val screenPoint = MemoryUtils.worldToScreen(dummyWorldPos, testMatrix, width, height)

        if (screenPoint.isValid) {
            val left = screenPoint.x - 40f
            val top = screenPoint.y - 100f
            val right = screenPoint.x + 40f
            val bottom = screenPoint.y + 100f
            
            canvas.drawRect(left, top, right, bottom, boxPaint)
            canvas.drawText("Target [ESP]", left, top - 10f, textPaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isRunning = false
        scope?.cancel()
    }
}
