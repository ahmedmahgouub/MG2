package com.muhgoub.hud

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var btnLaunchPanel: Button
    private lateinit var btnStopPanel: Button
    private lateinit var btnModeNormal: Button
    private lateinit var btnModeTurbo: Button
    private lateinit var switchPermission: Switch

    private var currentMode = "normal"

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

        // زر Normal - طلب صلاحية الروت عند النقر
        btnModeNormal.setOnClickListener {
            currentMode = "normal"
            requestRootPermissionForAction("Normal")
        }

        // زر Kernel - طلب صلاحية الروت عند النقر
        btnModeTurbo.setOnClickListener {
            currentMode = "kernel"
            requestRootPermissionForAction("Kernel")
        }

        switchPermission.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                Toast.makeText(this, "تنبيه: يجب منح الصلاحية لعمل الرادار بكفاءة", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // دالة لطلب صلاحيات الروت فعلياً من نظام الـ Root / KernelSU
    private fun requestRootPermissionForAction(modeName: String) {
        Toast.makeText(this, "جارِ طلب صلاحيات الروت لوضع $modeName...", Toast.LENGTH_SHORT).show()

        Thread {
            var success = false
            try {
                // تنفيذ أمر su لفتح نافذة منح الصلاحية للمستخدم (Magisk / KernelSU / SuperSU)
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
                val exitValue = process.waitFor()
                if (exitValue == 0) {
                    success = true
                }
            } catch (e: Exception) {
                success = false
            }

            runOnUiThread {
                if (success) {
                    Toast.makeText(this, "تم منح صلاحيات الروت بنجاح لوضع $modeName!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "فشل منح صلاحيات الروت! تأكد من أن الجهاز به روت نشط", Toast.LENGTH_LONG).show()
                }
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
