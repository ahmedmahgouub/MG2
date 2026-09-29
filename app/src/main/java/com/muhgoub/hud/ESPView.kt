package com.muhgoub.hud

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlinx.coroutines.*

class ESPView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var isRunning = false
    private var scope: CoroutineScope? = null
    
    private var viewMatrix = FloatArray(16)
    private val playerList = mutableListOf<MemoryUtils.Vector3>()
    private var statusMessage = "WAITING FOR PUBG..."

    private val textPaint = Paint().apply {
        color = android.graphics.Color.GREEN
        textSize = 36f
        isAntiAlias = true
    }

    private val enemyBoxPaint = Paint().apply {
        color = android.graphics.Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startLoop()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopLoop()
    }

    private fun startLoop() {
        if (isRunning) return
        isRunning = true
        scope = CoroutineScope(Dispatchers.Default + Job())
        scope?.launch {
            while (isRunning) {
                // استدعاء مباشر ودائم لدالة الـ C++ وهي التي تتولى قنص الـ PID داخلياً بأمان وبدون تعليق الكوتلن
                val nativePlayers = MemoryUtils.getPlayersLocations(1)
                val tempPlayers = mutableListOf<MemoryUtils.Vector3>()

                if (nativePlayers != null) {
                    tempPlayers.addAll(nativePlayers)
                }

                synchronized(playerList) {
                    playerList.clear()
                    playerList.addAll(tempPlayers)
                }
                
                statusMessage = MemoryUtils.nativeStatusMessage

                withContext(Dispatchers.Main) {
                    invalidate()
                }
                delay(20L)
            }
        }
    }

    private fun stopLoop() {
        isRunning = false
        scope?.cancel()
        scope = null
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
}
