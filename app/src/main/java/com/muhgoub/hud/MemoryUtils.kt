package com.muhgoub.hud

import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder

object MemoryUtils {

    // متغير التحكم في وضع الكيرنال (True = وضع الكيرنال السريع / False = الوضع العادي)
    var isKernelModeEnabled: Boolean = false

    // الأوفسيتات الأساسية والمحدثة للعبة
    const val OFFSET_UWORLD: Long = 0x98E23A0L         // GWorld الحقيقي
    const val OFFSET_PROJECTION_MATRIX: Long = 0xC400000L // Matrix / Projection Base
    const val OFFSET_PERSISTENT_LEVEL: Long = 0x38L      // Persistent Level
    const val OFFSET_PLAYER_CONTROLLER: Long = 0x30L     // PlayerController Offset
    const val OFFSET_PAWN_VELOCITY: Long = 0x330L        // AcknowledgedPawn
    const val OFFSET_PLAYER_INDEX: Long = 0x120L         // TeamIndex

    // الأوفستات الإضافية للـ Actors و Level
    const val OFFSET_ACTORS: Long = 0x98L                // مؤشر قائمة الأكتورس داخل الـ Level
    const val OFFSET_ACTORS_COUNT: Long = 0xA0L          // عدد الأكتورس الكلي

    // الأوفستات الخاصة بالـ Entity والهيكل والشصي
    const val OFFSET_GNAME: Long = 0x981C5C0L
    const val OFFSET_MESH: Long = 0x310L
    const val OFFSET_ROOT_COMPONENT: Long = 0x150L
    const val OFFSET_COMPONENT_TO_WORLD: Long = 0x240L
    const val OFFSET_BONE_ARRAY: Long = 0x5a0L
    const val OFFSET_HEALTH: Long = 0x118cL
    const val OFFSET_MAX_HEALTH: Long = 0x1190L
    const val OFFSET_ACTOR_BOX: Long = 0x140L
    const val OFFSET_CURRENT_WEAPON: Long = 0x2B8L
    const val OFFSET_PLAYER_NAME: Long = 0x308L
    const val OFFSET_GRENADE_WARNING: Long = 0x1E0L
    const val OFFSET_RELATIVE_LOCATION: Long = 0x1E4L    // إحداثيات الموقع (X, Y, Z) للـ RootComponent

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

    fun getModuleBase(pid: Int, moduleName: String = "libUE4.so"): Long {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "cat /proc/$pid/maps"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.contains(moduleName) && line!!.contains("r-xp")) {
                    val addrPart = line!!.substringBefore("-")
                    return addrPart.toLong(16)
                }
            }
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0L
    }

    data class Point2D(val x: Float, val y: Float, val isValid: Boolean)
    data class Vector3(val x: Float, val y: Float, val z: Float)

    // ================= قراءة الفلوت مع الدعم المزدوج (Kernel / Normal) =================
    fun readFloat(pid: Int, address: Long): Float {
        if (isKernelModeEnabled) {
            return KernelMemory.readFloat(pid, address)
        }
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

    // ================= قراءة اللونج مع الدعم المزدوج =================
    fun readLong(pid: Int, address: Long): Long {
        if (isKernelModeEnabled) {
            return KernelMemory.readLong(pid, address)
        }
        try {
            val cmd = "dd if=/proc/$pid/mem bs=1 skip=$address count=8 2>/dev/null"
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            val inputStream = process.inputStream
            val buffer = ByteArray(8)
            val bytesRead = inputStream.read(buffer)
            process.waitFor()
            if (bytesRead == 8) {
                return ByteBuffer.wrap(buffer)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .long
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0L
    }

    // ================= قراءة المتجهات مع الدعم المزدوج =================
    fun readVector3(pid: Int, address: Long): Vector3 {
        if (isKernelModeEnabled) {
            return KernelMemory.readVector3(pid, address)
        }
        try {
            val cmd = "dd if=/proc/$pid/mem bs=1 skip=$address count=12 2>/dev/null"
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            val inputStream = process.inputStream
            val buffer = ByteArray(12)
            val bytesRead = inputStream.read(buffer)
            process.waitFor()
            if (bytesRead == 12) {
                val byteBuffer = ByteBuffer.wrap(buffer).order(ByteOrder.LITTLE_ENDIAN)
                val x = byteBuffer.float
                val y = byteBuffer.float
                val z = byteBuffer.float
                return Vector3(x, y, z)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return Vector3(0f, 0f, 0f)
    }

    // ================= قراءة المصفوفة مع الدعم المزدوج =================
    fun readMatrix(pid: Int, address: Long): FloatArray {
        val matrix = FloatArray(16)
        if (isKernelModeEnabled) {
            return KernelMemory.readMatrix(pid, address)
        }
        try {
            val byteCount = 16 * 4
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
