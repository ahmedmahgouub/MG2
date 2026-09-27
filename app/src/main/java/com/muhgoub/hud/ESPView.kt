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
        textSize = 26f
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

    // إزحافات النسخة 64-bit
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
                        // 1. قراءة الـ ViewMatrix
                        viewMatrix = MemoryUtils.readMatrix(currentPid, libBase + VIEW_WORLD_OFFSET)

                        // 2. قراءة GWorld وفحص الـ Pointers لتشخيص البيانات
                        val gWorldPtr = MemoryUtils.readLong(currentPid, libBase + GWORLD_OFFSET)
                        
                        var foundCount = 0
                        synchronized(playerList) {
                            playerList.clear()
                            
                            if (gWorldPtr != 0L) {
                                val persistentLevel = MemoryUtils.readLong(currentPid, gWorldPtr + 0x30L)
                                if (persistentLevel != 0L) {
                                    val actorsPtr = MemoryUtils.readLong(currentPid, persistentLevel + 0x98L)
                                    
                                    if (actorsPtr != 0L) {
                                        for (i in 0 until 50) {
                                            val actor = MemoryUtils.readLong(currentPid, actorsPtr + (i * 8L).toLong())
                                            if (actor != 0L) {
                                                val rootComponent = MemoryUtils.readLong(currentPid, actor + 0x140L)
                                                if (rootComponent != 0L) {
                                                    val x = MemoryUtils.readFloat(currentPid, rootComponent + 0x1C0L)
                                                    val y = MemoryUtils.readFloat(currentPid, rootComponent + 0x1C4L)
                                                    val z = MemoryUtils.readFloat(currentPid, rootComponent + 0x1C8L)
                                                    
                                                    if (x != 0f || y != 0f) {
                                                        playerList.add(MemoryUtils.Vector3(x, y, z))
                                                        foundCount++
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // تحديث رسالة الحالة لعرض نتيجة الفحص وقيمة الـ GWorld مباشرة على الشاشة
                        statusMessage = "PID: $currentPid | GW: ${gWorldPtr != 0L} | Actors: $foundCount"
                        
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

        // رسم الحالة ومعلومات التشخيص أعلى الشاشة
        canvas.drawText(statusMessage, 30f, 120f, textPaint)

        // رسم المربعات للأعداء عند توفر إحداثيات صالحة
        synchronized(playerList) {
            for (player in playerList) {
                val pt = MemoryUtils.worldToScreen(player, viewMatrix, width, height)
                if (pt.isValid) {
                    val l = pt.x - 40f
                    val t = pt.y - 100f
                    val r = pt.x + 40f
                    val b = pt.y + 100f
                    canvas.drawRect(l, t, r, b, enemyBoxPaint)
                    canvas.drawText("Player", l, t - 8f, textPaint)
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
