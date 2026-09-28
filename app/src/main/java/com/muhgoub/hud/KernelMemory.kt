package com.muhgoub.hud

import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

object KernelMemory {

    // قراءة الفلوت من عمق النواة مباشرة
    fun readFloat(pid: Int, address: Long): Float {
        try {
            // استخدام أمر مدعوم من KernelSU لقراءة الذاكرة من مساحة النواة
            val cmd = "su -M -c 'dd if=/proc/$pid/mem bs=1 skip=$address count=4 2>/dev/null'"
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val buffer = ByteArray(4)
            val bytesRead = process.inputStream.read(buffer)
            process.waitFor()
            if (bytesRead == 4) {
                return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).float
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0f
    }

    // قراءة اللونج (لجلب الـ UWorld والـ GW من عمق النواة)
    fun readLong(pid: Int, address: Long): Long {
        try {
            val cmd = "su -M -c 'dd if=/proc/$pid/mem bs=1 skip=$address count=8 2>/dev/null'"
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val buffer = ByteArray(8)
            val bytesRead = process.inputStream.read(buffer)
            process.waitFor()
            if (bytesRead == 8) {
                return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).long
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0L
    }

    // قراءة المتجهات من عمق النواة
    fun readVector3(pid: Int, address: Long): MemoryUtils.Vector3 {
        try {
            val cmd = "su -M -c 'dd if=/proc/$pid/mem bs=1 skip=$address count=12 2>/dev/null'"
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val buffer = ByteArray(12)
            val bytesRead = process.inputStream.read(buffer)
            process.waitFor()
            if (bytesRead == 12) {
                val byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
                return MemoryUtils.Vector3(byteBuffer.float, byteBuffer.float, byteBuffer.float)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return MemoryUtils.Vector3(0f, 0f, 0f)
    }

    // قراءة المصفوفة للـ ESP من عمق النواة
    fun readMatrix(pid: Int, address: Long): FloatArray {
        val matrix = FloatArray(16)
        try {
            val byteCount = 16 * 4
            val cmd = "su -M -c 'dd if=/proc/$pid/mem bs=1 skip=$address count=$byteCount 2>/dev/null'"
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val buffer = ByteArray(byteCount)
            val bytesRead = process.inputStream.read(buffer)
            process.waitFor()
            if (bytesRead == byteCount) {
                val byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
                for (i in 0 until 16) {
                    matrix[i] = byteBuffer.float
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return matrix
    }
}
