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

    // حالات أزرار التحكم والخيارات
    var isBoxEnabled: Boolean = false
    var isLineEnabled: Boolean = false
    var isDistanceEnabled: Boolean = false
    var isHealthEnabled: Boolean = false
    var isNameEnabled: Boolean = false
    var isPlayerCountEnabled: Boolean = true

    // متغيرات اللعبة المحدثة من الذاكرة
    private var screenWidth = 0
    private var screenHeight = 0
    private var currentPid = -1
    private var currentBaseAddr = 0L
    private var currentUWorld = 0L
    private var viewMatrix: FloatArray = FloatArray(16)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenWidth = w
        screenHeight = h
    }

    // دالة تحديث البيانات المتوافقة تماماً مع ما ترسله OverlayService
    fun updateGameData(pid: Int, baseAddr: Long, uWorld: Long, matrix: FloatArray) {
        this.currentPid = pid
        this.currentBaseAddr = baseAddr
        this.currentUWorld = uWorld
        this.viewMatrix = matrix
        invalidate() // إعادة الرسم في كل إطار
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (screenWidth == 0 || screenHeight == 0) return

        // عرض معلومات الذاكرة على الشاشة للتأكد من الاتصال
        if (isPlayerCountEnabled) {
            canvas.drawText("UWorld: ${if (currentUWorld != 0L) "Connected" else "Searching..."}", 50f, 100f, textPaint)
        }
    }
}
