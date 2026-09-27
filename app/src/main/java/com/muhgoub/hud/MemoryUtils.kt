package com.muhgoub.hud

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

object MemoryUtils {

    private fun executeRootCmd(command: String): String {
        var result = ""
        try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            outputStream.writeBytes("$command\n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()

            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line).append("\n")
            }
            process.waitFor()
            result = sb.toString().trim()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }

    fun findProcessId(packageName: String): Int {
        val output = executeRootCmd("pidof $packageName")
        if (output.isNotEmpty()) {
            val firstLine = output.lines().firstOrNull()
            if (!firstLine.isNullOrEmpty()) {
                try {
                    return firstLine.trim().toInt()
                } catch (e: NumberFormatException) {
                    // تجاهل الخطأ
                }
            }
        }
        return -1
    }

    fun getModuleBase(pid: Int, moduleName: String = "libUE4.so"): Long {
        val output = executeRootCmd("cat /proc/$pid/maps")
        if (output.isNotEmpty()) {
            for (line in output.lines()) {
                if (line.contains(moduleName) && line.contains("r-xp")) {
                    val addrPart = line.substringBefore("-")
                    try {
                        return addrPart.toLong(16)
                    } catch (e: Exception) {
                        // تجاهل الخطأ
                    }
                }
            }
        }
        return 0L
    }

    data class Point2D(val x: Float, val y: Float, val isValid: Boolean)
    data class Vector3(val x: Float, val y: Float, val z: Float)

    // قراءة بايتات الذاكرة باستخدام dd عبر الروت المضمون
    private fun readBytes(pid: Int, address: Long, count: Int): ByteArray? {
        try {
            // استخدام أمر dd لنقل البيانات إلى ملف مؤقت أو قراءتها عبر الـ stream
            // بما أن الـ dd يطبع على stdout، سنستخدم الطريقة الآمنة
            val cmd = "dd if=/proc/$pid/mem bs=1 skip=$address count=$count 2>/dev/null"
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            val inputStream = process.inputStream
            val buffer = ByteArray(count)
            val bytesRead = inputStream.read(buffer)
            process.waitFor()
            if (bytesRead == count) {
                return buffer
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun readFloat(pid: Int, address: Long): Float {
        val buffer = readBytes(pid, address, 4) ?: return 0f
        return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).float
    }

    fun readLong(pid: Int, address: Long): Long {
        val buffer = readBytes(pid, address, 8) ?: return 0L
        return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).long
    }

    fun readVector3(pid: Int, address: Long): Vector3 {
        val buffer = readBytes(pid, address, 12) ?: return Vector3(0f, 0f, 0f)
        val byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
        return Vector3(byteBuffer.float, byteBuffer.float, byteBuffer.float)
    }

    fun readMatrix(pid: Int, address: Long): FloatArray {
        val matrix = FloatArray(16)
        val byteCount = 16 * 4
        val buffer = readBytes(pid, address, byteCount) ?: return matrix
        val byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until 16) {
            matrix[i] = byteBuffer.float
        }
        return matrix
    }

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
