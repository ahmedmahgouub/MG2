package com.muhgoub.hud

import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

object MemoryUtils {

    fun findProcessId(packageName: String): Int {
        var pid = -1
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "pidof $packageName"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            if (!line.isNullOrEmpty()) {
                pid = line.trim().toInt()
            }
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return pid
    }

    data class Point2D(val x: Float, val y: Float, val isValid: Boolean)
    data class Vector3(val x: Float, val y: Float, val z: Float)

    // قراءة قيمة Float من الذاكرة عبر dd والروت
    fun readFloat(pid: Int, address: Long): Float {
        try {
            val cmd = "dd if=/proc/$pid/mem bs=1 skip=$address count=4 2>/dev/null"
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            val inputStream = process.inputStream
            val buffer = ByteArray(4)
            val bytesRead = inputStream.read(buffer)
            process.waitFor()
            if (bytesRead == 4) {
                return ByteBuffer.wrap(buffer)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .float
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0f
    }

    // قراءة مصفوفة كاملة (مثلاً ViewMatrix مكونة من 16 قيمة Float)
    fun readMatrix(pid: Int, address: Long): FloatArray {
        val matrix = FloatArray(16)
        try {
            val byteCount = 16 * 4 // 16 floats * 4 bytes
            val cmd = "dd if=/proc/$pid/mem bs=1 skip=$address count=$byteCount 2>/dev/null"
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            val inputStream = process.inputStream
            val buffer = ByteArray(byteCount)
            val bytesRead = inputStream.read(buffer)
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

    // معادلة تحويل الإحداثيات من 3D إلى 2D
    fun worldToScreen(worldLocation: Vector3, matrix: FloatArray, screenWidth: Int, screenHeight: Int): Point2D {
        val w = matrix[3] * worldLocation.x + matrix[7] * worldLocation.y + matrix[11] * worldLocation.z + matrix[15]

        if (w < 0.01f) {
            return Point2D(0f, 0f, false)
        }

        val invW = 1.0f / w
        val x = screenWidth / 2 + (matrix[0] * worldLocation.x + matrix[4] * worldLocation.y + matrix[8] * worldLocation.z + matrix[12]) * invW * (screenWidth / 2)
        val y = screenHeight / 2 - (matrix[1] * worldLocation.x + matrix[5] * worldLocation.y + matrix[9] * worldLocation.z + matrix[13]) * invW * (screenHeight / 2)

        return Point2D(x, y, true)
    }
}
