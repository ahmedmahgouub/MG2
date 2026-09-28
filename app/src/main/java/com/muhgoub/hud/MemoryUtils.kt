package com.muhgoub.hud

import java.io.BufferedReader
import java.io.InputStreamReader

object MemoryUtils {

    // الأوفسيتات الأساسية المستخدمة حالياً فقط
    const val OFFSET_GNAME = 0x981C5C0L
    const val OFFSET_UWORLD = 0x98E23A0L
    const val UE4_POINTER_BASE = 0xC400000L

    fun findProcessId(packageName: String): Int {
        var pid = -1
        try {
            val process = Runtime.getRuntime().exec("pidof $packageName")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            if (line != null && line.isNotBlank()) {
                pid = line.trim().split(" ")[0].toInt()
            }
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return pid
    }

    fun getModuleBase(pid: Int, moduleName: String): Long {
        try {
            val process = Runtime.getRuntime().exec("su -M -c 'cat /proc/$pid/maps'")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.contains(moduleName) && line!!.contains("r-xp")) {
                    val addrPart = line!!.split(" ")[0]
                    val startAddr = addrPart.split("-")[0]
                    return startAddr.toLong(16)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0L
    }
}
