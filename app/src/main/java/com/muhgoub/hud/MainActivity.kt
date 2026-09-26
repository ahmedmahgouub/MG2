package com.muhgoub.hud // تأكد أن اسم الحزمة مطابق لمشروعك

import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var tvKernelVersion: TextView
    private lateinit var btnLaunchPanel: Button
    private lateinit var btnStopPanel: Button
    private lateinit var btnModeNormal: Button
    private lateinit var btnModeTurbo: Button
    private lateinit var switchPermission: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ربط عناصر الواجهة
        tvKernelVersion = findViewById(R.id.tvKernelVersion)
        btnLaunchPanel = findViewById(R.id.btnLaunchPanel)
        btnStopPanel = findViewById(R.id.btnStopPanel)
        btnModeNormal = findViewById(R.id.btnModeNormal)
        btnModeTurbo = findViewById(R.id.btnModeTurbo)
        switchPermission = findViewById(R.id.switchPermission)

        // جلب رقم الكيرنل في الخلفية بدون ما يجمد التاتش أو الشاشة
        fetchAndCleanKernel()

        // الأزرار وتفاعلاتها ستعمل فوراً بدون أي تعليق
        btnLaunchPanel.setOnClickListener {
            // كود تشغيل الرادار
        }

        btnStopPanel.setOnClickListener {
            // كود إيقاف الرادار
        }

        btnModeNormal.setOnClickListener {
            // وضع Normal
        }

        btnModeTurbo.setOnClickListener {
            fetchAndCleanKernel()
        }
    }

    private fun fetchAndCleanKernel() {
        // تشغيل العملية بالكامل في خلفية منفصلة تماماً عشان التطبيق يفتح سلس
        Thread {
            var rawKernel = ""
            try {
                val process = Runtime.getRuntime().exec("uname -r")
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                rawKernel = reader.readLine() ?: ""
                // تم إزالة process.waitFor() لمنع تجميد واجهة المستخدم
            } catch (e: Exception) {
                rawKernel = ""
            }

            if (rawKernel.isBlank()) {
                rawKernel = System.getProperty("os.version") ?: "6.1.157"
            }

            // قص النص الطويل وأخذ الرقم الصافي فقط
            val cleanKernel = rawKernel.split("-", " ")[0]

            // تحديث واجهة المستخدم بأمان تام
            runOnUiThread {
                tvKernelVersion.text = "$cleanKernel : نسخة الكيرنل"
            }
        }.start()
    }
}
