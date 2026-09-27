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
    private var statusMessage = "MUHGOUB ESP - SAFE MODE"
    private var currentPid = -1
    
    private var viewMatrix = FloatArray(16)
    private val playerList = mutableListOf<MemoryUtils.Vector3>()

    private val VIEW_WORLD_OFFSET = 0xF5FBFD0L
    private val GWORLD_BASE_OFFSET = 0xF624D40L
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
                try {
                    currentPid = MemoryUtils.findProcessId("com.tencent.ig")
                    
                    if (currentPid != -1) {
                        val libBase = MemoryUtils.getModuleBase(currentPid, "libUE4.so")
                        
                        if (libBase != 0L) {
                            // 1. قراءة الـ ViewMatrix بأمان
                            val matrixTemp = MemoryUtils.readMatrix(currentPid, libBase + VIEW_WORLD_OFFSET)
                            if (matrixTemp.size == 16) {
                                viewMatrix = matrixTemp
                            }

                            // 2. قراءة GWorld مع حماية كاملة من الـ Crash والتأكد من نطاق الذاكرة
                            var gWorldPtr = 0L
                            val potentialAddr = libBase + GWORLD_BASE_OFFSET
                            val candidate = MemoryUtils.readLong(currentPid, potentialAddr)
                            
                            if (candidate != 0L && candidate > 0x10000000L) {
                                val checkVal = MemoryUtils.readLong(currentPid, candidate - 0x20L)
                                if (checkVal != 0L && checkVal > 0x10000000L) {
                                    gWorldPtr = checkVal + 0x30L
                                }
                            }

                            var count = 0
                            synchronized(playerList) {
                                playerList.clear()
                                if (gWorldPtr != 0L && gWorldPtr > 0x10000000L) {
                                    val persistentLevel = MemoryUtils.readLong(currentPid, gWorldPtr + OFFSET_PERSISTENT_LEVEL)
                                    if (persistentLevel != 0L && persistentLevel > 0x10000000L) {
                                        val actorsPtr = MemoryUtils.readLong(currentPid, persistentLevel + OFFSET_ACTORS_ARRAY)
                                        val actorsCount = MemoryUtils.readLong(currentPid, persistentLevel + OFFSET_ACTORS_COUNT).toInt()
                                        
                                        if (actorsPtr != 0L && actorsCount in 1..5000) {
                                            val maxCount = minOf(actorsCount, 500)
                                            for (i in 0 until maxCount) {
                                                val actor = MemoryUtils.readLong(currentPid, actorsPtr + (i * 8L))
                                                if (actor != 0L && actor > 0x10000000L) {
                                                    val rootComponent = MemoryUtils.readLong(currentPid, actor + 0x208L)
                                                    if (rootComponent != 0L && rootComponent > 0x10000000L) {
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
                } catch (e: Exception) {
                    statusMessage = "ERROR: ${e.localizedMessage}"
                }

                withContext(Dispatchers.Main) {
                    invalidate()
                }
                delay(30L)
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
