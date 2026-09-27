package com.muhgoub.hud

import java.io.File

object MemoryUtils {

    data class Vector3(val x: Float, val y: Float, val z: Float)
    data class ScreenPoint(val x: Float, val y: Float, val isValid: Boolean)

    external fun attachProcess(pid: Int): Boolean
    external fun readMemory(pid: Int, address: Long, buffer: ByteArray, size: Int): Boolean
    external fun writeMemory(pid: Int, address: Long, buffer: ByteArray, size: Int): Boolean

    init {
        System.loadLibrary("muhgoub_memory")
    }

    fun findProcessId(packageName: String): Int {
        val dir = File("/proc")
        if (!dir.exists()) return -1
        for (cmdlineFile in dir.listFiles() ?: arrayOf()) {
            if (!cmdlineFile.isDirectory) continue
            val pid = cmdlineFile.name.toIntOrNull() ?: continue
            try {
                val pkg = File(cmdlineFile, "cmdline").readText().trim { it <= ' ' }
                if (pkg == packageName) return pid
            } catch (e: Exception) {
                // Ignore
            }
        }
        return -1
    }

    fun getModuleBase(pid: Int, moduleName: String): Long {
        try {
            val mapsFile = File("/proc/$pid/maps")
            if (!mapsFile.exists()) return 0L
            
            val lines = mapsFile.readLines()
            for (line in lines) {
                if (line.contains(moduleName) && line.contains("r-xp")) {
                    val addressPart = line.substringBefore("-")
                    return addressPart.toLong(16)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0L
    }

    fun readLong(pid: Int, address: Long): Long {
        val buffer = ByteArray(8)
        if (readMemory(pid, address, buffer, 8)) {
            var value = 0L
            for (i in 0..7) {
                value = value or ((buffer[i].toLong() and 0xFF) shl (8 * i))
            }
            return value
        }
        return 0L
    }

    fun readFloat(pid: Int, address: Long): Float {
        val buffer = ByteArray(4)
        if (readMemory(pid, address, buffer, 4)) {
            val intBits = (buffer[0].toInt() and 0xFF) or
                    ((buffer[1].toInt() and 0xFF) shl 8) or
                    ((buffer[2].toInt() and 0xFF) shl 16) or
                    ((buffer[3].toInt() and 0xFF) shl 24)
            return Float.fromBits(intBits)
        }
        return 0f
    }

    fun readMatrix(pid: Int, address: Long): FloatArray {
        val buffer = ByteArray(64)
        val matrix = FloatArray(16)
        if (readMemory(pid, address, buffer, 64)) {
            for (i in 0..15) {
                val intBits = (buffer[i * 4].toInt() and 0xFF) or
                        ((buffer[i * 4 + 1].toInt() and 0xFF) shl 8) or
                        ((buffer[i * 4 + 2].toInt() and 0xFF) shl 16) or
                        ((buffer[i * 4 + 3].toInt() and 0xFF) shl 24)
                matrix[i] = Float.fromBits(intBits)
            }
        }
        return matrix
    }

    fun worldToScreen(worldLocation: Vector3, matrix: FloatArray, width: Int, height: Int): ScreenPoint {
        val transformedZ = matrix[3] * worldLocation.x + matrix[7] * worldLocation.y + matrix[11] * worldLocation.z + matrix[15]
        if (transformedZ < 0.01f) return ScreenPoint(0f, 0f, false)

        val inv = 1.0f / transformedZ
        val screenX = (width / 2.0f) + (matrix[0] * worldLocation.x + matrix[4] * worldLocation.y + matrix[8] * worldLocation.z + matrix[12]) * inv * (width / 2.0f)
        val screenY = (height / 2.0f) - (matrix[1] * worldLocation.x + matrix[5] * worldLocation.y + matrix[9] * worldLocation.z + matrix[13]) * inv * (height / 2.0f)

        return ScreenPoint(screenX, screenY, true)
    }
}
