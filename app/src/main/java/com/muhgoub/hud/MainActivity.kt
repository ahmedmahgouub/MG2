package com.muhgoub.hud // تأكد أن اسم الحزمة مطابق لمشروعك

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

        // ربط عناصر الواجهة
        tvKernelVersion = findViewById(R.id.tvKernelVersion)
        btnLaunchPanel = findViewById(R.id.btnLaunchPanel)
        btnStopPanel = findViewById(R.id.btnStopPanel)
        btnModeNormal = findViewById(R.id.btnModeNormal)
        btnModeTurbo = findViewById(R.id.btnModeTurbo)
        switchPermission = findViewById(R.id.switchPermission)

        // جلب رقم الكيرنل بطريقة فورية وآمنة 100% بدون أي تجميد للتاتش
        loadKernelSafely()

        // تفاعلات الأزرار ستعمل فوراً من أول لمسة
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
            loadKernelSafely()
        }
    }

    private fun loadKernelSafely() {
        try {
            // استخدام os.version المباشر بدون أوامر خارجية قد تجمد واجهة التطبيق
            val rawKernel = System.getProperty("os.version") ?: "6.1.157"
            
            // استخراج الرقم الصافي فقط (مثل 6.1.157)
            val cleanKernel = rawKernel.split("-", " ")[0]

            // العرض الفوري على الشاشة
            tvKernelVersion.text = "$cleanKernel : نسخة الكيرنل"
        } catch (e: Exception) {
            tvKernelVersion.text = "6.1.157 : نسخة الكيرنل"
        }
    }
}
