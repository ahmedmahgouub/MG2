package com.muhgoub.hud

import java.nio.ByteBuffer
import java.nio.ByteOrder

object KernelMemory {

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
