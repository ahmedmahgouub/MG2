package com.example.esp.utils

object MemoryUtils {
    // تم تحديث الـ Base Address بناءً على استخراج Termux الأخير
    const val LIB_BASE_ADDRESS: Long = 0x766411e000L

    // الأوفسيتات الأساسية للعبة
    const val OFFSET_UWORLD: Long = 0x40D0C7F
    const val OFFSET_PROJECTION_MATRIX: Long = 0x4126140
    const val OFFSET_PERSISTENT_LEVEL: Long = 0x422C7C8

    // دالة لجلب العناوين المطلوبة للـ ESP
    fun getUWorldAddress(): Long {
        return LIB_BASE_ADDRESS + OFFSET_UWORLD
    }

    fun getMatrixAddress(): Long {
        return LIB_BASE_ADDRESS + OFFSET_PROJECTION_MATRIX
    }
}
