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
                        
                        // ==========================================
                        // ضع هنا أوفسيت الـ ViewMatrix الخاص بنسختك الحالية
                        // ==========================================
                        val matrixOffset = 0x0L // استبدل الصفر بالأوفسيت الحقيقي للمصفوفة
                        if (matrixOffset != 0L) {
                            viewMatrix = MemoryUtils.readMatrix(currentPid, libBase + matrixOffset)
                        }

                        // ==========================================
                        // قراءة قائمة الكائنات والأعداء (Entity List)
                        // ==========================================
                        synchronized(playerList) {
                            playerList.clear()
                            
                            // مثال لقراءة إحداثيات لاعب عبر مؤشر (Pointer) الأوفسيت الخاص بك:
                            // val entityListOffset = 0x0L
                            // val worldBase = MemoryUtils.readLong(currentPid, libBase + entityListOffset)
                            // هنا يتم إضافة حلقة لوب لجلب إحداثيات كل لاعب وحفظها:
                            // val pos = MemoryUtils.readVector3(currentPid, playerAddress)
                            // if (pos.x != 0f) playerList.add(pos)
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
