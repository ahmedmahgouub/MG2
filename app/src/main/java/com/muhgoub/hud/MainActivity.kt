package com.muhgoub.hud

import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

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

        // ربط عناصر الواجهة بدقة عالية
        tvKernelVersion = findViewById(R.id.tvKernelVersion)
        btnLaunchPanel = findViewById(R.id.btnLaunchPanel)
        btnStopPanel = findViewById(R.id.btnStopPanel)
        btnModeNormal = findViewById(R.id.btnModeNormal)
        btnModeTurbo = findViewById(R.id.btnModeTurbo)
        switchPermission = findViewById(R.id.switchPermission)

        // جلب كيرنل الجهاز الفعلي فور الفتح
        loadDeviceKernel()

        // تفاعلات الأزرار
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
            loadDeviceKernel()
        }
    }

    private fun loadDeviceKernel() {
        try {
            val rawKernel = System.getProperty("os.version") ?: android.os.Build.VERSION.INCREMENTAL ?: "1.0.0"
            val cleanKernel = rawKernel.split("-", " ")[0]
            tvKernelVersion.text = "$cleanKernel : نسخة الكيرنل"
        } catch (e: Exception) {
            tvKernelVersion.text = "1.0.0 : نسخة الكيرنل"
        }
    }
}
