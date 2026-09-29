package com.muhgoub.hud

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var isKernelMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_Hud)
        setContentView(R.layout.activity_main)

        val btnNormal = findViewById<Button>(R.id.btn_normal_mode)
        val btnKernel = findViewById<Button>(R.id.btn_kernel_mode)
        val btnStart = findViewById<Button>(R.id.btn_start_hud)

        btnNormal.setOnClickListener {
            isKernelMode = false
            Toast.makeText(this, "تم تفعيل الوضع العادي (Root Standard)", Toast.LENGTH_SHORT).show()
        }

        btnKernel.setOnClickListener {
            isKernelMode = true
            Toast.makeText(this, "تم تفعيل وضع الكيرنال (KernelSU Mode)", Toast.LENGTH_SHORT).show()
        }

        btnStart.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                val serviceIntent = Intent(this, OverlayService::class.java).apply {
                    putExtra("KERNEL_MODE", isKernelMode)
                }
                startService(serviceIntent)
                finish()
            }
        }
    }
}
