package com.muhgoub.hud

import java.io.BufferedReader
import java.io.InputStreamReader

object MemoryUtils {

    // دالة للبحث عن رقم العملية (PID) الخاصة بلعبة ببجي
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

    // دالة أساسية لاختبار قراءة عنوان من الذاكرة عبر الروت
    fun readMemoryHex(pid: Int, address: Long): String {
        var result = "0x0"
        try {
            // استخدام أدوات الروت المتقدمة أو أوامر القراءة المباشرة للذاكرة
            val cmd = "su -c dd if=/proc/$pid/mem bs=1 skip=$address count=4 2>/dev/null | hexdump -C"
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            if (!line.isNullOrEmpty()) {
                result = line
            }
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return result
    }
}
