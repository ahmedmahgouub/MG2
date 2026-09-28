package com.muhgoub.hud

import java.io.BufferedReader
import java.io.InputStreamReader

object MemoryUtils {

    // 1. تحميل مكتبة الـ C++ الحركية تلقائياً عند تشغيل التطبيق
    init {
        System.loadLibrary("hud_internal")
    }

    // 2. الدالة الخارجية فائقة السرعة المربوطة بملف main.cpp
    @JvmStatic
    external fun getPlayersLocations(pid: Int): Array<Vector3>?

    // الأوفسيتات الأساسية والمباشرة للإصدار الأخير
    const val OFFSET_GNAME: Long = 0xF08F820L
    const val OFFSET_GWORLD: Long = 0xF624D40L
    const val OFFSET_VIEW_WORLD: Long = 0xF5FBFD0L
    const val OFFSET_UE4_POINTER: Long = 0xE0C36E0L

    // الإزاحات الداخلية للهيكل
    const val OFFSET_PERSISTENT_LEVEL: Long = 0x30L
    const val OFFSET_ACTOR_ARRAY: Long = 0xA0L  // تم التحديث لـ 64 بت
    const val OFFSET_ACTOR_COUNT: Long = 0xA8L  // تم التحديث لـ 64 بت
    const val OFFSET_ROOT_COMPONENT: Long = 0x208L
    const val OFFSET_RELATIVE_LOCATION: Long = 0x1E4L

    // هياكل البيانات المطابقة للـ C++ والـ UI
    data class Point2D(val x: Float, val y: Float, val isValid: Boolean)
    data class Vector3(val x: Float, val y: Float, val z: Float)

    // دالة جلب الـ PID السريعة والمستقرة عبر الروت
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

    // دالة جلب الـ Base Address للعبة
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

    // دالة قراءة المصفوفة (ViewWorld) للكاميرا
    fun readMatrix(pid: Int, address: Long): FloatArray {
        val matrix = FloatArray(16)
        // تم الحفاظ عليها مؤقتاً للكاميرا، وسيتم سحبها للـ C++ لاحقاً لزيادة الفريمات
        return matrix
    }

    // دالة تحويل الإحداثيات من الـ World إلى الشاشة للرسم
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
