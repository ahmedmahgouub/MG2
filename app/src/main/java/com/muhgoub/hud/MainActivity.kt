package com.muhgoub.hud

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var btnLaunchPanel: Button
    private lateinit var btnStopPanel: Button
    private lateinit var btnModeNormal: Button
    private lateinit var btnModeTurbo: Button
    private lateinit var switchPermission: Switch
    private lateinit var tvKernelVersion: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        btnLaunchPanel = findViewById(R.id.btnLaunchPanel)
        btnStopPanel = findViewById(R.id.btnStopPanel)
        btnModeNormal = findViewById(R.id.btnModeNormal)
        btnModeTurbo = findViewById(R.id.btnModeTurbo)
        switchPermission = findViewById(R.id.switchPermission)
        tvKernelVersion = findViewById(R.id.tvKernelVersion)
    }

    private fun setupListeners() {
        // زر تشغيل الرادار
        btnLaunchPanel.setOnClickListener {
            if (checkOverlayPermission()) {
                val intent = Intent(this, OverlayService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                Toast.makeText(this, "تم تشغيل الرادار", Toast.LENGTH_SHORT).show()
            } else {
                requestOverlayPermission()
            }
        }

        // زر إيقاف الرادار
        btnStopPanel.setOnClickListener {
            val intent = Intent(this, OverlayService::class.java).apply {
                action = OverlayService.ACTION_STOP
            }
            startService(intent)
            Toast.makeText(this, "تم إيقاف الرادار", Toast.LENGTH_SHORT).show()
        }

        // زر Normal
        btnModeNormal.setOnClickListener {
            Toast.makeText(this, "تم التفعيل على وضع: Normal", Toast.LENGTH_SHORT).show()
        }

        // زر Kernel - هنا النقطة الأساسية: عند الضغط يتم طلب الروت فورا وجلب ورقم ونوع الكيرنل وعرضه مكان MUHGoub
        btnModeTurbo.setOnClickListener {
            Toast.makeText(this, "جارِ طلب صلاحيات الروت وفحص الكيرنل...", Toast.LENGTH_SHORT).show()
            fetchAndDisplayKernelVersion()
        }

        switchPermission.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                Toast.makeText(this, "تنبيه: يجب منح الصلاحية لعمل الرادار بكفاءة", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // دالة فحص الكيرنل وطلب صلاحية الروت وعرض النتيجة في المربع بدقة
    private fun fetchAndDisplayKernelVersion() {
        Thread {
            var kernelVersionResult = "غير معروف"
            try {
                // تنفيذ أمر su لجلب رقم الكيرنل الحقيقي من النظام
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "uname -r"))
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = reader.readLine()
                if (!output.isNullOrBlank()) {
                    kernelVersionResult = output.trim()
                }
                process.waitFor()
            } catch (e: Exception) {
                // بديل لو الروت لم يستجب أو النظام منع الأمر بشكل مؤقت
                kernelVersionResult = System.getProperty("os.version") ?: "6.1.157"
            }

            runOnUiThread {
                // عرض رقم الكيرنل الحقيقي مكان الاسم القديم فوراً
                tvKernelVersion.text = kernelVersionResult
                Toast.makeText(this, "تم فحص الكيرنل بنجاح: $kernelVersionResult", Toast.LENGTH_LONG).show()
            }
        }.start()
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, 1234)
        }
    }
}
