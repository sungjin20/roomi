package com.example.roomi;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_TIME = 2000; // 2초
    private static final String ALARM_PREFS = "alarm_prefs";
    private static final String ALARM_KEYS = "alarm_request_keys";

    private ActivityResultLauncher<String[]> multiplePermissionsLauncher;
    private boolean shouldCheckExactAlarmAgain = false;
    private String adminUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        adminUid = getString(R.string.admin_uid);

        multiplePermissionsLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    // 일반 권한 체크 후 정확 알람 권한 확인
                    checkExactAlarmPermission();
                });

        new Handler().postDelayed(this::checkPermissions, SPLASH_TIME);
    }

    private void checkPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE_DATA_SYNC);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION);
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK);
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (!permissionsToRequest.isEmpty()) {
            multiplePermissionsLauncher.launch(permissionsToRequest.toArray(new String[0]));
        } else {
            checkExactAlarmPermission(); // 일반 권한 이미 있으면 정확 알람 권한 체크
        }
    }

    private void checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(this, "정확한 루틴 알림을 위해 설정에서 권한을 허용해주세요.", Toast.LENGTH_LONG).show();

                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);

                shouldCheckExactAlarmAgain = true;
                return;
            }
        }

        cancelExistingAlarms();
        proceedToNextActivity();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldCheckExactAlarmAgain) {
            shouldCheckExactAlarmAgain = false;
            checkExactAlarmPermission();
        }
    }

    private void cancelExistingAlarms() {
        // 알람 키 초기화
        SharedPreferences prefs = getSharedPreferences(ALARM_PREFS, MODE_PRIVATE);
        Set<String> alarmKeys = prefs.getStringSet(ALARM_KEYS, new HashSet<>());

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        for (String key : alarmKeys) {
            try {
                int requestCode = Integer.parseInt(key);
                Intent intent = new Intent(this, RoutineAlarmReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        this,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                alarmManager.cancel(pendingIntent);
            } catch (NumberFormatException ignored) {}
        }

        prefs.edit().remove(ALARM_KEYS).apply();

        // ForegroundService 관련 키 초기화
        SharedPreferences servicePrefs = getSharedPreferences("service_prefs", MODE_PRIVATE);
        servicePrefs.edit().remove("active_routines").apply();

        // ForegroundService 종료
        Intent stopIntent = new Intent(this, RoutineNotificationService.class);
        stopService(stopIntent);
    }

    private void proceedToNextActivity() {
        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
        boolean stayLoggedIn = prefs.getBoolean("stay_logged_in", false);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (stayLoggedIn && currentUser != null) {
            String uid = currentUser.getUid();
            // 관리자의 UID 확인
            if (uid.equals(adminUid)) {  // 관리자 UID로 변경하세요
                startActivity(new Intent(SplashActivity.this, ChatRoomListActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
            }
        } else {
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        }
        finish();
    }

}
