package com.muhgoub.hud;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

public class MainActivity extends AppCompatActivity {

    private Button btnModeNormal, btnModeTurbo;
    private TextView tvMahgoub;
    private Switch switchPermission;
    private boolean isNormalActive = false;
    private boolean isKernelActive = false;

    private static final String CHANNEL_ID = "radar_channel_id";
    private static final int NOTIFICATION_ID = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        checkOverlayPermission();
        setContentView(R.layout.activity_main);

        tvMahgoub = findViewById(R.id.tvMahgoub);
        btnModeNormal = findViewById(R.id.btnModeNormal);
        btnModeTurbo = findViewById(R.id.btnModeTurbo);
        switchPermission = findViewById(R.id.switchPermission);

        createNotificationChannel();

        // زر Normal (تبديل Toggle وتغيير الألوان ورسائل التفعيل والإغلاق)
        btnModeNormal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isNormalActive) {
                    isNormalActive = true;
                    isKernelActive = false;

                    btnModeNormal.setBackgroundResource(R.drawable.bg_segment_filled);
                    btnModeTurbo.setBackgroundResource(R.drawable.bg_segment_outline);
                    
                    tvMahgoub.setText("MUHGOUB");
                    Toast.makeText(MainActivity.this, "تم تفعيل Normal", Toast.LENGTH_SHORT).show();
                } else {
                    isNormalActive = false;
                    btnModeNormal.setBackgroundResource(R.drawable.bg_segment_outline);
                    
                    Toast.makeText(MainActivity.this, "تم إغلاق Normal", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // زر Kernel (تبديل Toggle وتغيير الألوان ورسائل التفعيل والإغلاق)
        btnModeTurbo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isKernelActive) {
                    isKernelActive = true;
                    isNormalActive = false;

                    btnModeTurbo.setBackgroundResource(R.drawable.bg_segment_filled);
                    btnModeNormal.setBackgroundResource(R.drawable.bg_segment_outline);

                    tvMahgoub.setText("KERNEL");
                    Toast.makeText(MainActivity.this, "تم تفعيل Kernel", Toast.LENGTH_SHORT).show();
                } else {
                    isKernelActive = false;
                    btnModeTurbo.setBackgroundResource(R.drawable.bg_segment_outline);

                    tvMahgoub.setText("MUHGOUB");
                    Toast.makeText(MainActivity.this, "تم إغلاق Kernel", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "يرجى السماح بالظهور فوق التطبيقات لتشغيل الرادار", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Radar Channel";
            String description = "Channel for Radar Running Notification";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }
}
