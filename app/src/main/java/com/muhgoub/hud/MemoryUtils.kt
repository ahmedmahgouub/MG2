package com.muhgoub.hud

import java.io.File
import java.io.FileInputStream
import java.io.BufferedReader
import java.io.InputStreamReader

object MemoryUtils {

    @JvmStatic
    var nativeStatusMessage: String = "WAITING FOR PUBG..."

    init {
        System.loadLibrary("hud_internal")
    }

    @JvmStatic
    external fun getPlayersLocations(pid: Int): Array<Vector3>?

    const val OFFSET_GNAME: Long = 0xF08F820L
    const val OFFSET_GWORLD: Long = 0xF624D40L
    const val OFFSET_VIEW_WORLD: Long = 0xF5FBFD0L
    const val OFFSET_UE4_POINTER: Long = 0xE0C36E0L

    const val OFFSET_PERSISTENT_LEVEL: Long = 0x30L
    const val OFFSET_ACTOR_ARRAY: Long = 0xA0L  
    const val OFFSET_ACTOR_COUNT: Long = 0xA8L  
    const val OFFSET_ROOT_COMPONENT: Long = 0x208L
    const val OFFSET_RELATIVE_LOCATION: Long = 0x1E4L

    data class Point2D(val x: Float, val y: Float, val isValid: Boolean)
    data class Vector3(val x: Float, val y: Float, val z: Float)

    // دالة فحص مجلد الـ /proc لقنص الـ PID الصافي بدون أوامر شيل محظورة
    fun findProcessId(packageName: String): Int {
        val procDir = File("/proc")
        val files = procDir.listFiles() ?: return -1
        for (file in files) {
            if (file.isDirectory) {
                val pid = file.name.toIntOrNull()
                if (pid != null && pid > 0) {
                    try {
                        val cmdlineFile = File(file, "cmdline")
                        if (cmdlineFile.exists()) {
                            val stream = FileInputStream(cmdlineFile)
                            val reader = BufferedReader(InputStreamReader(stream))
                            val cmdline = reader.readLine()
                            reader.close()
                            stream.close()
                            if (cmdline != null && cmdline.trim().startsWith(packageName)) {
                                return pid
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return -1
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

    fun readMatrix(pid: Int, address: Long): FloatArray {
        return FloatArray(16)
    }

    // 🟢 تم إغلاق وتثبيت أرقام خانات مصفوفة الكاميرا الـ 16 القياسية حرفاً بحرف لمنع الـ Type Mismatch
    fun worldToScreen(worldLocation: Vector3, matrix: FloatArray, screenWidth: Int, screenHeight: Int): Point2D {
        if (matrix.size < 16) return Point2D(0f, 0f, false)
        
        val w = matrix[3] * worldLocation.x + matrix[7] * worldLocation.y + matrix[11] * worldLocation.z + matrix[15]
        if (w < 0.01f) return Point2D(0f, 0f, false)

        val invW = 1.0f / w
        val x = screenWidth / 2 + (matrix[0] * worldLocation.x + matrix[4] * worldLocation.y + matrix[8] * worldLocation.z + matrix[12]) * invW * (screenWidth / 2)
        val y = screenHeight / 2 - (matrix[1] * worldLocation.x + matrix[5] * worldLocation.y + matrix[9] * worldLocation.z + matrix[13]) * invW * (screenHeight / 2)

        return Point2D(x, y, true)
    }
}
