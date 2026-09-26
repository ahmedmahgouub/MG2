package com.muhgoub.hud

import android.content.Intent
import android.graphics.Color
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
    private lateinit var tvMahgoub: TextView

    private var currentMode = "normal"
    private var hasRequestedRoot = false // لضمان طلب صلاحية الروت الحقيقية مرة واحدة فقط عند أول ضغطة

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
        tvMahgoub = findViewById(R.id.tvMahgoub)
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

        // زر Normal (يطلب صلاحية الروت الحقيقية أول ضغطة فقط)
        btnModeNormal.setOnClickListener {
            currentMode = "normal"
            Toast.makeText(this, "تم التفعيل على وضع: Normal", Toast.LENGTH_SHORT).show()
            checkAndRequestRootOnce()
        }

        // زر Kernel (يطلب الروت أول ضغطة + يجيب أول 5 أرقام من الكيرنال مكان محجوب باللون البرتقالي الغامق)
        btnModeTurbo.setOnClickListener {
            currentMode = "kernel"
            Toast.makeText(this, "تم التفعيل على وضع: Kernel", Toast.LENGTH_SHORT).show()
            checkAndRequestRootOnce()
            fetchKernelVersionAndDisplay()
        }

        switchPermission.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                Toast.makeText(this, "تنبيه: يجب منح الصلاحية لعمل الرادار بكفاءة", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // دالة طلب صلاحية الروت الحقيقية مرة واحدة فقط
    private fun checkAndRequestRootOnce() {
        if (!hasRequestedRoot) {
            hasRequestedRoot = true
            requestRealRootPermission()
        }
    }

    private fun requestRealRootPermission() {
        Thread {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
                process.waitFor()
            } catch (e: Exception) {
                // تجاهل أي خطأ بأمان تام
            }
        }.start()
    }

    // جلب كيرنال الجهاز واستخراج أول 5 أرقام فقط وعرضها مكان كلمة محجوب باللون البرتقالي الغامق
    private fun fetchKernelVersionAndDisplay() {
        Thread {
            var kernelStr = ""
            try {
                val process = Runtime.getRuntime().exec("uname -r")
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val line = reader.readLine()
                if (!line.isNullOrEmpty()) {
                    kernelStr = line.trim()
                }
                process.waitFor()
            } catch (e: Exception) {
                kernelStr = ""
            }

            if (kernelStr.isEmpty()) {
                kernelStr = Build.VERSION.INCREMENTAL ?: "3.10.0"
            }

            // استخراج أول 5 أرقام أو رموز كحد أقصى من نص الكيرنال
            val first5 = if (kernelStr.length >= 5) kernelStr.substring(0, 5) else kernelStr

            runOnUiThread {
                tvMahgoub.text = first5
                tvMahgoub.setTextColor(Color.parseColor("#FF8C00")) // لون برتقالي غامق
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
