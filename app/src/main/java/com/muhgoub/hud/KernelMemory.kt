package com.muhgoub.hud

import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

object KernelMemory {

    // دالة قراءة الفلوت (Float) بسرعة فائقة عبر الكيرنال المباشر
    fun readFloat(pid: Int, address: Long): Float {
        try {
            val memFile = RandomAccessFile("/proc/$pid/mem", "r")
            memFile.seek(address)
            val buffer = ByteArray(4)
            memFile.readFully(buffer)
            memFile.close()
            return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).float
        } catch (e: Exception) {
            // تجاهل الخطأ البسيط لعدم إبطاء حلقة الرسم
        }
        return 0f
    }

    // دالة قراءة اللونج (Long) بسرعة فائقة
    fun readLong(pid: Int, address: Long): Long {
        try {
            val memFile = RandomAccessFile("/proc/$pid/mem", "r")
            memFile.seek(address)
            val buffer = ByteArray(8)
            memFile.readFully(buffer)
            memFile.close()
            return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).long
        } catch (e: Exception) {
            // صامت لتفادي اللاج
        }
        return 0L
    }

    // دالة قراءة المتجهات (Vector3)
    fun readVector3(pid: Int, address: Long): MemoryUtils.Vector3 {
        try {
            val memFile = RandomAccessFile("/proc/$pid/mem", "r")
            memFile.seek(address)
            val buffer = ByteArray(12)
            memFile.readFully(buffer)
            memFile.close()
            val byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
            return MemoryUtils.Vector3(byteBuffer.float, byteBuffer.float, byteBuffer.float)
        } catch (e: Exception) {
            // صامت
        }
        return MemoryUtils.Vector3(0f, 0f, 0f)
    }

    // دالة قراءة المصفوفة (Matrix) للـ ESP والـ GW
    fun readMatrix(pid: Int, address: Long): FloatArray {
        val matrix = FloatArray(16)
        try {
            val byteCount = 16 * 4
            val memFile = RandomAccessFile("/proc/$pid/mem", "r")
            memFile.seek(address)
            val buffer = ByteArray(byteCount)
            memFile.readFully(buffer)
            memFile.close()
            val byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until 16) {
                matrix[i] = byteBuffer.float
            }
        } catch (e: Exception) {
            // صامت
        }
        return matrix
    }
}
