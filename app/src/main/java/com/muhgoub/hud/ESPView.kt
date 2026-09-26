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
    private var statusMessage = "MUHGOUB ESP - RUNNING"
    private var currentPid = -1

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
                    
                    // هنا تقدر تقرأ عناوين الذاكرة الحقيقية لاحقاً باستخدام:
                    // val sampleValue = MemoryUtils.readFloat(currentPid, 0x00000000L)
                } else {
                    statusMessage = "WAITING FOR PUBG..."
                }

                withContext(Dispatchers.Main) {
                    invalidate()
                }
                delay(100L) // تحديث سريع وسلس
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // رسم حالة الكاشف أعلى الشاشة
        canvas.drawText(statusMessage, 50f, 150f, textPaint)

        // رسم صندوق تجريبي للتأكد من دقة الإحداثيات والشاشة العائمة
        val sampleWorldPos = MemoryUtils.Vector3(100f, 500f, 50f)
        val dummyMatrix = FloatArray(16) { 1f }
        
        val screenPoint = MemoryUtils.worldToScreen(sampleWorldPos, dummyMatrix, width, height)
        
        if (screenPoint.isValid) {
            val left = screenPoint.x - 50f
            val top = screenPoint.y - 100f
            val right = screenPoint.x + 50f
            val bottom = screenPoint.y + 100f
            canvas.drawRect(left, top, right, bottom, boxPaint)
            canvas.drawText("Player (Test)", left, top - 10f, textPaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isRunning = false
        scope?.cancel()
    }
}
