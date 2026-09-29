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
    external fun getPlayersLocations(pid: Int, baseAddress: Long): Array<Vector3>?

    data class Point2D(val x: Float, val y: Float, val isValid: Boolean)
    data class Vector3(val x: Float, val y: Float, val z: Float)

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
                if ((line!!.contains("libUE4.so") || line!!.contains("libanogs.so") || line!!.contains("libshadowtracker")) && line!!.contains("r-xp")) {
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

    fun worldToScreen(worldLocation: Vector3, matrix: FloatArray, screenWidth: Int, screenHeight: Int): Point2D {
        if (matrix.size < 16) return Point2D(0f, 0f, false)
        val w = matrix * worldLocation.x + matrix * worldLocation.y + matrix * worldLocation.z + matrix
        if (w < 0.01f) return Point2D(0f, 0f, false)
        val invW = 1.0f / w
        val x = screenWidth / 2 + (matrix * worldLocation.x + matrix * worldLocation.y + matrix * worldLocation.z + matrix) * invW * (screenWidth / 2)
        val y = screenHeight / 2 - (matrix * worldLocation.x + matrix * worldLocation.y + matrix * worldLocation.z + matrix) * invW * (screenHeight / 2)
        return Point2D(x, y, true)
    }
}
