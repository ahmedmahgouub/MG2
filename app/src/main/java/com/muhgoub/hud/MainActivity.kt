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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var btnLaunchPanel: Button
    private lateinit var btnStopPanel: Button
    private lateinit var switchPermission: Switch
    private lateinit var btnModeNormal: Button
    private lateinit var btnModeTurbo: Button
    private lateinit var prefs: PrefsManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refreshPermissionUi()
            if (hasOverlayPermission()) {
                startOverlayService()
            } else {
                Toast.makeText(this, getString(R.string.need_overlay_permission), Toast.LENGTH_SHORT).show()
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

        btnLaunchPanel.setOnClickListener { onLaunchPanelClicked() }
        btnStopPanel.setOnClickListener { onStopPanelClicked() }
        btnModeNormal.setOnClickListener { setAppMode("normal") }
        btnModeTurbo.setOnClickListener { setAppMode("turbo") }

        applyModeUi(prefs.getAppMode())

        // (1) فحص صلاحية العرض فوق التطبيقات فور فتح التطبيق أول مرة، وتوجيه فوري للإعدادات
        if (!hasOverlayPermission()) {
            requestOverlayPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionUi()
    }

    private fun onLaunchPanelClicked() {
        if (hasOverlayPermission()) {
            startOverlayService()
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

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, getString(R.string.panel_running), Toast.LENGTH_SHORT).show()
    }

    // بتتشغل لما المستخدم يدوس على "إيقاف القائمة": بتتأكد الأول إن الخدمة
    // شغالة فعلاً (عشان ميحصلش تشغيل خدمة جديدة بالغلط لو كانت أصلاً متوقفة)،
    // بعدين بتوقف القائمة العايمة على طول، وفي نفس اللحظة بتحوّل لون الزرار
    // لأحمر (bg_stop_button_active) لمدة ثانيتين كتأكيد بصري، وبعدين يرجع
    // الزرار للونه الأزرق الأصلي (bg_main_button) تاني.
    private fun onStopPanelClicked() {
        if (!isOverlayServiceRunning()) {
            Toast.makeText(this, getString(R.string.panel_already_stopped), Toast.LENGTH_SHORT).show()
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
        Toast.makeText(this, getString(R.string.panel_stopped), Toast.LENGTH_SHORT).show()
    }

    // بنستخدم ActivityManager بدل متغيّر محفوظ في الـ Prefs عشان نتأكد بشكل
    // موثوق إن الخدمة شغالة فعلاً دلوقتي (مش بس آخر حالة محفوظة ممكن تبقى
    // قديمة لو التطبيق اتقفل من النظام مثلاً).
    private fun isOverlayServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        @Suppress("DEPRECATION")
        return manager.getRunningServices(Integer.MAX_VALUE).any {
            it.service.className == OverlayService::class.java.name
        }
    }

    // (3) إصلاح زرّي "عادي / فائق" اللي كانوا من غير id ولا وظيفة
    private fun setAppMode(mode: String) {
        prefs.setAppMode(mode)
        applyModeUi(mode)
        Toast.makeText(
            this,
            if (mode == "turbo") "تم تفعيل الوضع الفائق" else "تم تفعيل الوضع العادي",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun applyModeUi(mode: String) {
        val isTurbo = mode == "turbo"
        btnModeNormal.setBackgroundResource(if (isTurbo) R.drawable.bg_segment_outline else R.drawable.bg_segment_filled)
        btnModeTurbo.setBackgroundResource(if (isTurbo) R.drawable.bg_segment_filled else R.drawable.bg_segment_outline)
    }
}
