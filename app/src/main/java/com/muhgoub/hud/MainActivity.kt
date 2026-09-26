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
    private lateinit var btnModeNormal: Button  // زرار Normal
    private lateinit var btnModeTurbo: Button   // زرار Kernel
    private lateinit var switchPermission: Switch

    private var currentMode = "normal" // الوضع الافتراضي

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
        // زرار تشغيل الرادار
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

        // زرار إيقاف الرادار
        btnStopPanel.setOnClickListener {
            val intent = Intent(this, OverlayService::class.java).apply {
                action = OverlayService.ACTION_STOP
            }
            startService(intent)
            Toast.makeText(this, "تم إيقاف الرادار", Toast.LENGTH_SHORT).show()
        }

        // زرار Normal (الوضع العادي)
        btnModeNormal.setOnClickListener {
            currentMode = "normal"
            Toast.makeText(this, "تم التفعيل على وضع: Normal", Toast.LENGTH_SHORT).show()
        }

        // زرار Kernel (الفحص ومنح صلاحيات الكيرنل)
        btnModeTurbo.setOnClickListener {
            currentMode = "kernel"
            checkAndRequestKernelPermissions()
        }

        // سويتش إخفاء التصوير
        switchPermission.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                Toast.makeText(this, "تنبيه: يجب منح الصلاحية لعمل الرادار بكفاءة", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // دالة البحث والتحقق من صلاحيات الكيرنل والروت
    private fun checkAndRequestKernelPermissions() {
        Toast.makeText(this, "جارِ البحث عن صلاحيات الكيرنل...", Toast.LENGTH_SHORT).show()

        Thread {
            val hasKernelAccess = verifyRootOrKernelSU()

            runOnUiThread {
                if (hasKernelAccess) {
                    Toast.makeText(this, "تم العثور على الكيرنل ومنح الصلاحية بنجاح!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "تنبيه: لم يتم اكتشاف صلاحيات كيرنل نشطة، سيتم العمل بوضع محدود", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    // فحص مسارات أدوات الروت و KernelSU
    private fun verifyRootOrKernelSU(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su",
            "/data/adb/ksu"
        )
        
        try {
            for (path in paths) {
                if (File(path).exists()) {
                    return true
                }
            }

            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val exitValue = process.waitFor()
            if (exitValue == 0) {
                return true
            }
        } catch (e: Exception) {
            // التعامل مع الخطأ بأمان بدون كراش
        }
        
        return false
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
