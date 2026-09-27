package com.muhgoub.hud

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import kotlinx.coroutines.*

class ESPView(context: Context) : View(context) {

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

    // إزحافات النسخة 64-bit المستخرجة من المصدر
    private val GWORLD_OFFSET = 0xF624D40L
    private val VIEW_WORLD_OFFSET = 0xF5FBFD0L

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
                        statusMessage = "PID: $currentPid | LIVE (x64)"
                        
                        // 1. قراءة الـ ViewMatrix باستخدام أوفسيت x64 الحقيقي
                        viewMatrix = MemoryUtils.readMatrix(currentPid, libBase + VIEW_WORLD_OFFSET)

                        // 2. قراءة العالم والكائنات عبر GWorld (0xF624D40)
                        val gWorldPtr = MemoryUtils.readLong(currentPid, libBase + GWORLD_OFFSET)
                        
                        synchronized(playerList) {
                            playerList.clear()
                            if (gWorldPtr != 0L) {
                                // جاهز لإضافة قراءة اللاعبين الحقيقية لاحقاً هنا
                            }
                        }
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

        // رسم صندوق تجريبي ثابت في منتصف الشاشة للتأكد من عمل الـ Canvas والرسم 100%
        val cx = width / 2f
        val cy = height / 2f
        canvas.drawRect(cx - 50f, cy - 50f, cx + 50f, cy + 50f, enemyBoxPaint)
        canvas.drawText("ESP WORKING [OK]", cx - 80f, cy - 60f, textPaint)

        // رسم العناصر المحولة من الذاكرة الحقيقية إلى الشاشة
        synchronized(playerList) {
            for (player in playerList) {
                val pt = MemoryUtils.worldToScreen(player, viewMatrix, width, height)
                if (pt.isValid) {
                    canvas.drawRect(pt.x - 40f, pt.y - 100f, pt.x + 40f, pt.y + 100f, enemyBoxPaint)
                    canvas.drawText("Enemy", pt.x - 40f, pt.y - 108f, textPaint)
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
