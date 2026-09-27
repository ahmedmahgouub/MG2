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
    private var statusMessage = "MUHGOUB ESP - 4.6 READY"
    private var currentPid = -1
    
    private var viewMatrix = FloatArray(16)
    private val playerList = mutableListOf<MemoryUtils.Vector3>()

    // العناوين المحدثة بالكامل حسب آخر سكريبت وقائمة تم اعتمادها
    private val GWORLD_BASE_OFFSET = 0xF3B85F8L
    private val VIEW_WORLD_OFFSET = 0xE6D63E0L
    private val OFFSET_ACTORS_ARRAY = 0xA0L

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
                        // 1. قراءة الـ ViewWorld Matrix بالعنوان الجديد
                        viewMatrix = MemoryUtils.readMatrix(currentPid, libBase + VIEW_WORLD_OFFSET)

                        // 2. تطبيق معادلة GWorld الدقيقة: (Base - 0x20) + 0x30
                        val step1 = MemoryUtils.readLong(currentPid, libBase + GWORLD_BASE_OFFSET)
                        val step2 = if (step1 != 0L) MemoryUtils.readLong(currentPid, step1 - 0x20L) else 0L
                        val gWorldPtr = if (step2 != 0L) step2 + 0x30L else 0L
                        
                        var count = 0
                        synchronized(playerList) {
                            playerList.clear()
                            if (gWorldPtr != 0L) {
                                val persistentLevel = MemoryUtils.readLong(currentPid, gWorldPtr + 0x30L)
                                if (persistentLevel != 0L) {
                                    // قراءة مصفوفة الكائنات بالإزحاف 0xA0
                                    val actorsPtr = MemoryUtils.readLong(currentPid, persistentLevel + OFFSET_ACTORS_ARRAY)
                                    if (actorsPtr != 0L) {
                                        for (i in 0 until 100) {
                                            val actor = MemoryUtils.readLong(currentPid, actorsPtr + (i * 8L))
                                            if (actor != 0L) {
                                                val rootComponent = MemoryUtils.readLong(currentPid, actor + 0x208L)
                                                if (rootComponent != 0L) {
                                                    val x = MemoryUtils.readFloat(currentPid, rootComponent + 0x1E4L)
                                                    val y = MemoryUtils.readFloat(currentPid, rootComponent + 0x1E8L)
                                                    val z = MemoryUtils.readFloat(currentPid, rootComponent + 0x1ECL)
                                                    
                                                    if (x != 0f || y != 0f) {
                                                        playerList.add(MemoryUtils.Vector3(x, y, z))
                                                        count++
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        statusMessage = "PID: $currentPid | GW: ${gWorldPtr != 0L} | Players: $count"
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
        canvas.drawText(statusMessage, 30f, 120f, textPaint)

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
