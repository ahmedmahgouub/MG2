package com.muhgoub.hud

import android.app.ActivityManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var btnLaunchPanel: Button
    private lateinit var btnStopPanel: Button
    private lateinit var switchPermission: Switch
    private lateinit var btnModeNormal: Button
    private lateinit var btnModeTurbo: Button
    private lateinit var tvKernelDisplay: TextView
    private lateinit var prefs: PrefsManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refreshPermissionUi()
            if (hasOverlayPermission()) {
                startOverlayServiceWithRoot()
            } else {
                Toast.makeText(this, "اخفاء الهاك عند تصوير الشاشه", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = PrefsManager(this)

        btnLaunchPanel = findViewById(R.id.btnLaunchPanel)
        btnStopPanel = findViewById(R.id.btnStopPanel)
        switchPermission = findViewById(R.id.switchPermission)
        btnModeNormal = findViewById(R.id.btnModeNormal)
        btnModeTurbo = findViewById(R.id.btnModeTurbo)
        tvKernelDisplay = findViewById(R.id.tvKernelDisplay)

        btnLaunchPanel.setOnClickListener { onLaunchPanelClicked() }
        btnStopPanel.setOnClickListener { onStopPanelClicked() }
        btnModeNormal.setOnClickListener { setAppMode("normal") }
        
        // عند الضغط على زر Kernel: طلب صلاحيات الروت، جلب أول 5 أحرف من الكيرنال، وتفعيل الوضع
        btnModeTurbo.setOnClickListener { 
            setAppMode("turbo")
            requestRootAndFetchKernel()
        }

        applyModeUi(prefs.getAppMode())

        // فحص صلاحية العرض فوق التطبيقات عند بدء التشغيل
        if (!hasOverlayPermission()) {
            requestOverlayPermission()
        }
        
        // منح وتأكيد صلاحيات الروت الشاملة للتطبيق في البداية
        requestRootPrivileges()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionUi()
    }

    private fun onLaunchPanelClicked() {
        if (hasOverlayPermission()) {
            startOverlayServiceWithRoot()
        } else {
            requestOverlayPermission()
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        overlayPermissionLauncher.launch(intent)
    }

    private fun refreshPermissionUi() {
        switchPermission.isChecked = hasOverlayPermission()
    }

    // دمج صلاحيات الروت لتغذية التطبيق بالكامل
    private fun requestRootPrivileges() {
        Thread {
            try {
                val process = Runtime.getRuntime().exec("su")
                val os = process.outputStream
                os.write("id\n".toByteArray())
                os.flush()
                os.write("exit\n".toByteArray())
                os.flush()
                process.waitFor()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    // فحص الكيرنال وجلب أول 5 أحرف فقط بدقة مكان MUHGOUB
    private fun requestRootAndFetchKernel() {
        Thread {
            var kernelResult = "MUHGOUB"
            try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "uname -r"))
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val line = reader.readLine()
                if (!line.isNullOrEmpty()) {
                    // اقتطاع أول 5 أرقام/حروف فقط من إصدار الكيرنال
                    kernelResult = if (line.length >= 5) line.substring(0, 5) else line
                }
                process.waitFor()
            } catch (e: Exception) {
                // في حال فشل جلب الكيرنال بالروت يتم جلب الطريقة العادية واختصارها لأول 5 أحرف
                try {
                    val fallback = Build.VERSION.INCREMENTAL
                    kernelResult = if (fallback.length >= 5) fallback.substring(0, 5) else fallback
                } catch (ex: Exception) {
                    kernelResult = "ERROR"
                }
            }

            mainHandler.post {
                tvKernelDisplay.text = kernelResult
                Toast.makeText(this, "تم تفعيل كيرنال الهاك بنجاح: $kernelResult", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    private fun startOverlayServiceWithRoot() {
        // تنفيذ أمر su لخدمة الرادار والقائمة العائمة لتغذية التطبيق بصلاحيات عميقة
        requestRootPrivileges()

        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "تم تشغيل الرادار", Toast.LENGTH_SHORT).show()
    }

    private fun onStopPanelClicked() {
        if (!isOverlayServiceRunning()) {
            Toast.makeText(this, "الرادار متوقف بالفعل", Toast.LENGTH_SHORT).show()
            return
        }
        btnStopPanel.setBackgroundResource(R.drawable.bg_stop_button_active)
        stopOverlayService()
        mainHandler.postDelayed({
            btnStopPanel.setBackgroundResource(R.drawable.bg_main_button)
        }, 2000)
    }

    private fun stopOverlayService() {
        val intent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_STOP
        }
        startService(intent)
        Toast.makeText(this, "تم إيقاف الرادار", Toast.LENGTH_SHORT).show()
    }

    private fun isOverlayServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        return manager.getRunningServices(Integer.MAX_VALUE).any {
            it.service.className == OverlayService::class.java.name
        }
    }

    private fun setAppMode(mode: String) {
        prefs.setAppMode(mode)
        applyModeUi(mode)
        Toast.makeText(
            this,
            if (mode == "turbo") "تم تفعيل وضع Kernel" else "تم تفعيل الوضع Normal",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun applyModeUi(mode: String) {
        val isTurbo = mode == "turbo"
        btnModeNormal.setBackgroundResource(if (isTurbo) R.drawable.bg_segment_outline else R.drawable.bg_segment_filled)
        btnModeTurbo.setBackgroundResource(if (isTurbo) R.drawable.bg_segment_filled else R.drawable.bg_segment_outline)
    }
}
