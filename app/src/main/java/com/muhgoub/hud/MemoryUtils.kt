package com.muhgoub.hud

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

object MemoryUtils {

    data class Vector3(val x: Float, val y: Float, val z: Float)
    data class ScreenPoint(val x: Float, val y: Float, val isValid: Boolean)

    // البحث عن رقم العملية (PID) للعبة
    fun findProcessId(packageName: String): Int {
        var pid = -1
        try {
            val file = File("/proc")
            val files = file.listFiles() ?: return -1
            for (fileItem in files) {
                if (fileItem.isDirectory) {
                    val cmdlineFile = File(fileItem, "cmdline")
                    if (cmdlineFile.exists() && cmdlineFile.canRead()) {
                        val cmdline = cmdlineFile.readText().trim { it <= ' ' }
                        if (cmdline == packageName) {
                            pid = fileItem.name.toInt()
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {}
        return pid
    }

    // جلب Base Address لمكتبة libUE4.so
    fun getModuleBase(pid: Int, moduleName: String): Long {
        try {
            val mapsFile = File("/proc/$pid/maps")
            if (mapsFile.exists()) {
                mapsFile.forEachLine { line ->
                    if (line.contains(moduleName) && line.contains("r-xp")) {
                        val addressPart = line.substringBefore("-")
                        return addressPart.toLong(16)
                    }
                }
            }
        } catch (e: Exception) {}
        return 0L
    }

    // قراءة قيمة Integer من الذاكرة
    fun readInt(pid: Int, address: Long): Int {
        try {
            val memFile = RandomAccessFile("/proc/$pid/mem", "r")
            memFile.seek(address)
            val buffer = ByteArray(4)
            memFile.readFully(buffer)
            memFile.close()
            return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).int
        } catch (e: Exception) {}
        return 0
    }

    // قراءة قيمة Float من الذاكرة
    fun readFloat(pid: Int, address: Long): Float {
        try {
            val memFile = RandomAccessFile("/proc/$pid/mem", "r")
            memFile.seek(address)
            val buffer = ByteArray(4)
            memFile.readFully(buffer)
            memFile.close()
            return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).float
        } catch (e: Exception) {}
        return 0f
    }

    // قراءة قيمة Long من الذاكرة
    fun readLong(pid: Int, address: Long): Long {
        try {
            val memFile = RandomAccessFile("/proc/$pid/mem", "r")
            memFile.seek(address)
            val buffer = ByteArray(8)
            memFile.readFully(buffer)
            memFile.close()
            return ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN).long
        } catch (e: Exception) {}
        return 0L
    }

    // قراءة مصفوفة الإسقاط (Matrix)
    fun readMatrix(pid: Int, address: Long): FloatArray {
        val matrix = FloatArray(16)
        try {
            val memFile = RandomAccessFile("/proc/$pid/mem", "r")
            memFile.seek(address)
            val buffer = ByteArray(64)
            memFile.readFully(buffer)
            memFile.close()
            val byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
            for (i in 0 until 16) {
                matrix[i] = byteBuffer.float
            }
        } catch (e: Exception) {}
        return matrix
    }

    // تحويل نص البصمة إلى بايتات وMask للبحث الديناميكي
    private fun parsePattern(patternStr: String): Pair<ByteArray, String> {
        val tokens = patternStr.trim().split(Regex("\\s+"))
        val bytes = ByteArray(tokens.size)
        val mask = StringBuilder()
        for (i in tokens.indices) {
            if (tokens[i] == "?" || tokens[i] == "??") {
                bytes[i] = 0
                mask.append("?")
            } else {
                bytes[i] = tokens[i].toInt(16).toByte()
                mask.append("x")
            }
        }
        return Pair(bytes, mask.toString())
    }

    // محرك البحث الديناميكي في الذاكرة (Pattern Scan)
    fun patternScan(pid: Int, startAddress: Long, regionSize: Long, patternStr: String): Long {
        try {
            val (patternBytes, mask) = parsePattern(patternStr)
            val memFile = RandomAccessFile("/proc/$pid/mem", "r")
            val bufferSize = 4096 * 4
            val buffer = ByteArray(bufferSize)
            var currentAddress = startAddress
            val endAddress = startAddress + regionSize
            
            while (currentAddress < endAddress) {
                try {
                    memFile.seek(currentAddress)
                    val bytesRead = memFile.read(buffer)
                    if (bytesRead <= 0) break
                    
                    for (i in 0..bytesRead - patternBytes.size) {
                        var found = true
                        for (j in patternBytes.indices) {
                            if (mask[j] == 'x' && buffer[i + j] != patternBytes[j]) {
                                found = false
                                break
                            }
                        }
                        if (found) {
                            memFile.close()
                            return currentAddress + i
                        }
                    }
                } catch (e: Exception) {}
                currentAddress += (bufferSize - patternBytes.size)
            }
            memFile.close()
        } catch (e: Exception) {}
        return 0L
    }

    // World to Screen Projection
    fun worldToScreen(pos: Vector3, matrix: FloatArray, width: Int, height: Int): ScreenPoint {
        val transX = matrix[3] * pos.x + matrix[7] * pos.y + matrix[11] * pos.z + matrix[15]
        if (transX < 0.01f) return ScreenPoint(0f, 0f, false)

        val transY = matrix[0] * pos.x + matrix[4] * pos.y + matrix[8] * pos.z + matrix[12]
        val transZ = matrix[1] * pos.x + matrix[5] * pos.y + matrix[9] * pos.z + matrix[13]

        val screenX = (width / 2.0f) + (transY * (width / 2.0f) / transX)
        val screenY = (height / 2.0f) - (transZ * (height / 2.0f) / transX)

        return ScreenPoint(screenX, screenY, true)
    }
}
