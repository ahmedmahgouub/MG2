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
    private var statusMessage = "MUHGOUB ESP - DYNAMIC SCAN"
    private var currentPid = -1
    
    private var viewMatrix = FloatArray(16)
    private val playerList = mutableListOf<MemoryUtils.Vector3>()

    // ViewWorld يظل ثابت أو ببحث خاص، و GWorld هنبحث عنه ديناميكياً
    private val VIEW_WORLD_OFFSET = 0xF5FBFD0L
    private val OFFSET_PERSISTENT_LEVEL = 0x30L
    private val OFFSET_ACTORS_ARRAY = 0xA0L
    private val OFFSET_ACTORS_COUNT = 0xA8L

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
                        // 1. قراءة الـ ViewWorld Matrix
                        viewMatrix = MemoryUtils.readMatrix(currentPid, libBase + VIEW_WORLD_OFFSET)

                        // 2. البحث الديناميكي واستخراج الـ GWorld (تجنب الثوابت التي تضرب false)
                        // سنستخدم إزاحة آمنة ومتحركة أو طريقة لفحص الإشارة في الذاكرة
                        var gWorldPtr = 0L
                        
                        // محاولة قراءة المؤشر الديناميكي عبر نطاق الـ BSS أو الإزاحة المحيطة لـ libUE4
                        val potentialGWorldAddr = libBase + 0xF624D40L // نقطة البداية للبحث
                        val candidate = MemoryUtils.readLong(currentPid, potentialGWorldAddr)
                        if (candidate != 0L) {
                            val checkVal = MemoryUtils.readLong(currentPid, candidate - 0x20L)
                            if (checkVal != 0L) {
                                gWorldPtr = checkVal + 0x30L
                            }
                        }

                        var count = 0
                        synchronized(playerList) {
                            playerList.clear()
                            if (gWorldPtr != 0L) {
                                val persistentLevel = MemoryUtils.readLong(currentPid, gWorldPtr + OFFSET_PERSISTENT_LEVEL)
                                if (persistentLevel != 0L) {
                                    val actorsPtr = MemoryUtils.readLong(currentPid, persistentLevel + OFFSET_ACTORS_ARRAY)
                                    val actorsCount = MemoryUtils.readLong(currentPid, persistentLevel + OFFSET_ACTORS_COUNT).toInt()
                                    
                                    if (actorsPtr != 0L && actorsCount > 0 && actorsCount < 60000) {
                                        val maxCount = minOf(actorsCount, 800)
                                        for (i in 0 until maxCount) {
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
