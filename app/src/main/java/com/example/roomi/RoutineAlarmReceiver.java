package com.example.roomi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class RoutineAlarmReceiver extends BroadcastReceiver {
    private static final String ALARM_PREFS = "alarm_prefs";
    private static final String ALARM_KEYS = "alarm_request_keys";
    public static final String SERVICE_PREFS = "service_prefs";
    public static final String ACTIVE_ROUTINES = "active_routines";

    @Override
    public void onReceive(Context context, Intent intent) {
        String title = intent.getStringExtra("title");
        String location = intent.getStringExtra("location");
        long duration = intent.getLongExtra("duration", 0);
        String id = intent.getStringExtra("id");

        // 알람 키에서 제거
        removeFromAlarmPref(context, id);
        addActiveRoutines(context, id);

        // 서비스 호출
        ArrayList<RoutineInfo> routineList = new ArrayList<>();
        routineList.add(new RoutineInfo(title, location, duration, id));

        Intent serviceIntent = new Intent(context, RoutineNotificationService.class);
        serviceIntent.putExtra("routine_list", routineList);
        ContextCompat.startForegroundService(context, serviceIntent);
    }

    private void removeFromAlarmPref(Context context, String routineId) {
        SharedPreferences prefs = context.getSharedPreferences(ALARM_PREFS, Context.MODE_PRIVATE);
        Set<String> keys = new HashSet<>(prefs.getStringSet(ALARM_KEYS, new HashSet<>()));

        String requestCode = String.valueOf(routineId.hashCode());

        if (keys.remove(requestCode)) {
            prefs.edit().putStringSet(ALARM_KEYS, keys).apply();
        }
    }

    private void addActiveRoutines(Context context, String routineId) {
        SharedPreferences prefs = context.getSharedPreferences(SERVICE_PREFS, Context.MODE_PRIVATE);
        Set<String> activeIds = new HashSet<>(prefs.getStringSet(ACTIVE_ROUTINES, new HashSet<>()));
        if (activeIds.add(routineId)) {
            prefs.edit().putStringSet(ACTIVE_ROUTINES, activeIds).apply();
        }
    }
}
