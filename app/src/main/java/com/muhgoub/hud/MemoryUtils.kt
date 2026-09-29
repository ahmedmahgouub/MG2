package com.muhgoub.hud

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

    // تم تثبيت مؤشر الخانة الثانية [1] بدقة لقنص الـ PID الفعلي من عمود الـ ps بأمان
    fun findProcessId(packageName: String): Int {
        var pid = -1
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "ps -A"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.contains(packageName)) {
                    val tokens = line!!.trim().split(Regex("\\s+"))
                    if (tokens.size > 1) {
                        val parsedPid = tokens[1].toIntOrNull()
                        if (parsedPid != null) {
                            pid = parsedPid
                            break
                        }
                    }
                }
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

    fun readMatrix(pid: Int, address: Long): FloatArray {
        return FloatArray(16)
    }

    // 🟢 تم إدراج كافة أرقام خانات المصفوفة الفردية القياسية بالملي لمنع أخطاء التجميع نهائياً
    fun worldToScreen(worldLocation: Vector3, matrix: FloatArray, screenWidth: Int, screenHeight: Int): Point2D {
        val funW = matrix[3] * worldLocation.x + matrix[7] * worldLocation.y + matrix[11] * worldLocation.z + matrix[15]
        if (funW < 0.01f) return Point2D(0f, 0f, false)

        val invW = 1.0f / funW
        val x = screenWidth / 2 + (matrix[0] * worldLocation.x + matrix[4] * worldLocation.y + matrix[8] * worldLocation.z + matrix[12]) * invW * (screenWidth / 2)
        val y = screenHeight / 2 - (matrix[1] * worldLocation.x + matrix[5] * worldLocation.y + matrix[9] * worldLocation.z + matrix[13]) * invW * (screenHeight / 2)

        return Point2D(x, y, true)
    }
}
