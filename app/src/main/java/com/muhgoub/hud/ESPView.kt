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
        textSize = 28f
        isAntiAlias = true
        setShadowLayer(4f, 0f, 0f, Color.BLACK)
    }

    private val enemyBoxPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private var scope: CoroutineScope? = null
    private var isRunning = false
    private var statusMessage = "MUHGOUB ESP - READY"
    private var currentPid = -1
    
    private var viewMatrix = FloatArray(16)
    private val playerList = mutableListOf<MemoryUtils.Vector3>()

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
                    val libBase = MemoryUtils.getModuleBase(currentPid, "libUE4.so")
                    
                    if (libBase != 0L) {
                        statusMessage = "PID: $currentPid | LIVE"
                        
                        // قراءة الـ ViewMatrix الحقيقية بإضافة الـ Offset الخاص بالنسخة
                        // val matrixOffset = 0xYOUR_OFFSET_HERE
                        // viewMatrix = MemoryUtils.readMatrix(currentPid, libBase + matrixOffset)
                        
                    } else {
                        statusMessage = "PID: $currentPid | WAITING FOR LIB..."
                    }
                } else {
                    statusMessage = "WAITING FOR PUBG..."
                }

                withContext(Dispatchers.Main) {
                    invalidate()
                }
                delay(25L)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // رسم الحالة وعنوان الأساس أعلى الشاشة
        canvas.drawText(statusMessage, 50f, 150f, textPaint)

        // رسم صندوق تجريبي للتأكد من استقرار الإطارات
        val dummyWorldPos = MemoryUtils.Vector3(0f, 250f, 50f)
        val testMatrix = FloatArray(16) { 1f }.apply {
            this[0] = 1f; this[5] = 1f; this[10] = 1f; this[15] = 1f
        }

        val screenPoint = MemoryUtils.worldToScreen(dummyWorldPos, testMatrix, width, height)
        if (screenPoint.isValid) {
            val left = screenPoint.x - 45f
            val top = screenPoint.y - 110f
            val right = screenPoint.x + 45f
            val bottom = screenPoint.y + 110f
            
            canvas.drawRect(left, top, right, bottom, boxPaint)
            canvas.drawText("Test Box [OK]", left, top - 10f, textPaint)
        }

        // رسم الأعداء الحقيقيين فور امتلاء القائمة بالإحداثيات
        synchronized(playerList) {
            for (player in playerList) {
                val pt = MemoryUtils.worldToScreen(player, viewMatrix, width, height)
                if (pt.isValid) {
                    val l = pt.x - 40f
                    val t = pt.y - 100f
                    val r = pt.x + 40f
                    val b = pt.y + 100f
                    canvas.drawRect(l, t, r, b, enemyBoxPaint)
                    canvas.drawText("Enemy", l, t - 8f, textPaint)
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isRunning = false
        scope?.cancel()
    }
}
