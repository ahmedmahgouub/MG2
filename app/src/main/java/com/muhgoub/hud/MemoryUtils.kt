package com.muhgoub.hud

import java.io.BufferedReader
import java.io.InputStreamReader

object MemoryUtils {
    @JvmStatic var nativeStatusMessage: String = "WAITING FOR GAME..."

    init {
        System.loadLibrary("hud_internal")
    }

    @JvmStatic 
    external fun getPlayersLocations(pid: Int, baseAddress: Long): Array<Vector3>?

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
        } catch (e: Exception) { e.printStackTrace() }
        return pid
    }

    fun getModuleBase(pid: Int): Long {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "cat /proc/$pid/maps"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if ((line!!.contains("libUE4.so") || line!!.contains("libshadowtracker")) && line!!.contains("r-xp")) {
                    return line!!.substringBefore("-").toLong(16)
                }
            }
            process.waitFor()
        } catch (e: Exception) { e.printStackTrace() }
        return 0L
    }
}
