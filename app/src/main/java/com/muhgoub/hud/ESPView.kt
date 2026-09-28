package com.muhgoub.hud

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View

class ESPView(context: Context) : View(context) {

    private val paint = Paint().apply {
        isAntiAlias = true
        textSize = 36f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint().apply {
        isAntiAlias = true
        textSize = 32f
        color = Color.GREEN
        style = Paint.Style.FILL
    }

    // حالات أزرار التحكم والخيارات (القائمة العائمة)
    var isBoxEnabled: Boolean = false
    var isLineEnabled: Boolean = false
    var isDistanceEnabled: Boolean = false
    var isHealthEnabled: Boolean = false
    var isNameEnabled: Boolean = false
    var isPlayerCountEnabled: Boolean = true

    // متغيرات اللعبة المحدثة
    private var screenWidth = 0
    private var screenHeight = 0
    private var actorsCount = 0
    private var viewMatrix: FloatArray = FloatArray(16)

    init {
        // تهيئة أولية
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        screenWidth = w
        screenHeight = h
    }

    // دوال تحديث البيانات لتغطية أي طريقة استدعاء من OverlayService بدون أخطاء
    fun updateGameData(count: Int) {
        this.actorsCount = count
        invalidate()
    }

    fun updateGameData(matrix: FloatArray, count: Int) {
        this.viewMatrix = matrix
        this.actorsCount = count
        invalidate()
    }

    fun updateGameData(matrix: FloatArray, actors: Any?, count: Int) {
        this.viewMatrix = matrix
        this.actorsCount = count
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (screenWidth == 0 || screenHeight == 0) return

        // رسم عدد اللاعبين أو الكيانات إذا كان مفعلًا
        if (isPlayerCountEnabled) {
            canvas.drawText("Actors Count: $actorsCount", 50f, 100f, textPaint)
        }
    }
}
