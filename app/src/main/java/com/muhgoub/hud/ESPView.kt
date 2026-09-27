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

    private val boxPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private var playerCount = 0

    init {
        // اختبار قراءة العناوين عند التهيئة
        val uworld = MemoryUtils.getUWorldAddress()
        // يمكنك إضافة حلقة تحديث خلفية (Coroutine أو Thread) لجلب الكيانات هنا
    }

    fun updatePlayerCount(count: Int) {
        playerCount = count
        invalidate() // إعادة الرسم لتحديث الواجهة
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // رسم عدد اللاعبين في الزاوية للتأكد من عمل الـ ESP
        canvas.drawText("Players: $playerCount", 50f, 100f, textPaint)
        
        // هنا يتم إضافة رسم المربعات وخطوط الإسقاط (Box/Line ESP) بناءً على مصفوفة الإسقاط
    }
}
