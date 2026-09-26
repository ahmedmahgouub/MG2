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
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    private Button btnModeNormal, btnModeTurbo;
    private TextView tvMahgoub;
    private Switch switchPermission;
    private boolean isKernelActive = false;
    private boolean hasRequestedNormalPermissions = false;

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

        btnModeNormal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnModeNormal.setBackgroundResource(R.drawable.bg_segment_filled);
                btnModeTurbo.setBackgroundResource(R.drawable.bg_segment_outline);

                if (!hasRequestedNormalPermissions) {
                    requestRootAndShowNotification();
                    hasRequestedNormalPermissions = true;
                } else {
                    Toast.makeText(MainActivity.this, "وضع Normal مفعل مسبقاً", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnModeTurbo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isKernelActive) {
                    isKernelActive = true;
                    btnModeTurbo.setBackgroundResource(R.drawable.bg_segment_filled);
                    btnModeNormal.setBackgroundResource(R.drawable.bg_segment_outline);

                    String kernelVersion = getKernelVersion();
                    if (kernelVersion.length() >= 5) {
                        tvMahgoub.setText(kernelVersion.substring(0, 5).toUpperCase());
                    } else {
                        tvMahgoub.setText("KERNEL");
                    }
                    Toast.makeText(MainActivity.this, "تم تفعيل وعرض بيانات الكيرنال", Toast.LENGTH_SHORT).show();
                } else {
                    isKernelActive = false;
                    btnModeTurbo.setBackgroundResource(R.drawable.bg_segment_outline);
                    btnModeNormal.setBackgroundResource(R.drawable.bg_segment_filled);

                    tvMahgoub.setText("MUHGOUB");
                    Toast.makeText(MainActivity.this, "تم إيقاف الكيرنال", Toast.LENGTH_SHORT).show();
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

    private void requestRootAndShowNotification() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = reader.readLine();
            if (output != null && output.contains("uid=0")) {
                Toast.makeText(this, "تم الحصول على صلاحيات الروت بنجاح", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        showRunningNotification();
    }

    private void showRunningNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentTitle("الرادار يعمل الآن")
                .setContentText("التطبيق يعمل في الخلفية بصلاحيات الروت")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(false);

        if (notificationManager != null) {
            notificationManager.notify(NOTIFICATION_ID, builder.build());
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

    private String getKernelVersion() {
        try {
            Process p = Runtime.getRuntime().exec("uname -r");
            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = in.readLine();
            if (line != null) {
                return line.trim();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "MUHGOUB";
    }
}
