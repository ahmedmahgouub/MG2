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

        // بحث وجلب رقم الكيرنل الصافي فور فتح التطبيق
        fetchAndCleanKernel()

        // الأزرار وتفاعلاتها
        btnLaunchPanel.setOnClickListener {
            // تشغيل الرادار
        }

        btnStopPanel.setOnClickListener {
            // إيقاف الرادار
        }

        btnModeNormal.setOnClickListener {
            // وضع Normal
        }

        // عند الضغط على زر Kernel يعيد البحث والتحديث
        btnModeTurbo.setOnClickListener {
            fetchAndCleanKernel()
        }
    }

    private fun fetchAndCleanKernel() {
        Thread {
            var rawKernel = ""
            try {
                // محاولة البحث عن الكيرنل الحقيقي من خلال أمر النظام
                val process = Runtime.getRuntime().exec("uname -r")
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                rawKernel = reader.readLine() ?: ""
                process.waitFor()
            } catch (e: Exception) {
                rawKernel = ""
            }

            // لو أمر النظام مابش حاجة، نجيبه من خصائص النظام كبديل آمن
            if (rawKernel.isBlank()) {
                rawKernel = System.getProperty("os.version") ?: "6.1.157"
            }

            // فلترة النص الطويل: أخذ الرقم الصافي فقط (أي شيء قبل أول شرطة '-' أو مسافة ' ')
            // مثال: لو الناتج "6.1.157-android14-..." هياخد "6.1.157" بس
            val cleanKernel = rawKernel.split("-", " ")[0]

            // عرض النتيجة النهائية بالترتيب المطلوب تماماً
            runOnUiThread {
                tvKernelVersion.text = "$cleanKernel : نسخة الكيرنل"
            }
        }.start()
    }
}
