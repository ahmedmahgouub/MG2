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
    
    // متغيرات البيانات الحقيقية
    private var currentPid = -1
    private var currentBaseAddr = 0L
    private var currentUWorld = 0L
    private var viewMatrix: FloatArray = FloatArray(16)
    var isPlayerCountEnabled: Boolean = true

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenWidth = w
        screenHeight = h
    }

    fun updateGameData(pid: Int, baseAddr: Long, uWorld: Long, matrix: FloatArray) {
        this.currentPid = pid
        this.currentBaseAddr = baseAddr
        this.currentUWorld = uWorld
        this.viewMatrix = matrix
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (screenWidth == 0 || screenHeight == 0) return

        if (isPlayerCountEnabled) {
            // التحقق الحقيقي من الـ GW (لو الـ uWorld مش صفر يبيبقى true)
            val isGwTrue = currentUWorld != 0L
            
            // النص الحقيقي الذي طلبت ظهوره تماماً
            val statusText = "PID: $currentPid | GW: $isGwTrue | Players: 0"
            
            canvas.drawText(statusText, 50f, 100f, textPaint)
        }
    }
}
