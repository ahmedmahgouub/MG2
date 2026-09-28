package com.muhgoub.hud

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class ESPView(context: Context) : View(context) {

    private val textPaint = Paint().apply {
        isAntiAlias = true
        textSize = 32f
        color = Color.GREEN
        style = Paint.Style.FILL
    }

    private var screenWidth = 0
    private var screenHeight = 0
    
    private var currentPid = -1
    private var currentBaseAddr = 0L
    private var currentUWorld = 0L
    private var playerCount = 0
    var isPlayerCountEnabled: Boolean = true

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenWidth = w
        screenHeight = h
    }

    fun updateGameData(pid: Int, baseAddr: Long, uWorld: Long, count: Int) {
        this.currentPid = pid
        this.currentBaseAddr = baseAddr
        this.currentUWorld = uWorld
        this.playerCount = count
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (screenWidth == 0 || screenHeight == 0) return

        if (isPlayerCountEnabled) {
            val isGwTrue = currentUWorld != 0L
            val statusText = "PID: $currentPid | GW: $isGwTrue | Players: $playerCount"
            canvas.drawText(statusText, 50f, 100f, textPaint)
        }
    }
}
