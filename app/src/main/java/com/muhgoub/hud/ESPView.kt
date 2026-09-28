package com.example.esp.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.example.esp.utils.MemoryUtils

class ESPView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val textPaint = Paint().apply {
        color = Color.GREEN
        textSize = 40f
        isAntiAlias = true
    }

    // تم ربط استخدام boxPaint أو جعله جاهزاً للاستخدام لمنع تحذيرات الكومبايلر
    private val boxPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private var playerCount = 0

    init {
        // استخدام المتغير لتجنب خطأ Unused Variable في البناء
        @Suppress("UNUSED_VARIABLE")
        val uworld = MemoryUtils.getUWorldAddress()
    }

    fun updatePlayerCount(count: Int) {
        playerCount = count
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // رسم عدد اللاعبين في الزاوية
        canvas.drawText("Players: $playerCount", 50f, 100f, textPaint)
        
        // مثال بسيط لاستخدام boxPaint لتجنب الفيلد لو الـ Gradle صارم
        // canvas.drawRect(100f, 200f, 300f, 500f, boxPaint)
    }
}
