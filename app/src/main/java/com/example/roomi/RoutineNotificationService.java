package com.example.roomi;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class RoutineNotificationService extends Service {

    public static final String CHANNEL_ID = "routine_channel";
    public static final int NOTIFICATION_ID = 1;

    public static final String SERVICE_PREFS = "service_prefs";
    public static final String ACTIVE_ROUTINES = "active_routines";

    private Handler handler;
    private Runnable updateRunnable;

    private final List<RoutineInfo> routines = new ArrayList<>();
    private final List<Long> endTimes = new ArrayList<>();

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        handler = new Handler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ArrayList<RoutineInfo> newRoutines = (ArrayList<RoutineInfo>) intent.getSerializableExtra("routine_list");
        if (newRoutines == null || newRoutines.isEmpty()) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }

        mergeRoutines(newRoutines);
        sortByRemainingTime();
        startForeground(NOTIFICATION_ID, buildNotification());

        if (updateRunnable == null) {
            updateRunnable = new Runnable() {
                @Override
                public void run() {
                    long now = System.currentTimeMillis();
                    boolean hasRemaining = false;

                    Iterator<RoutineInfo> routineIterator = routines.iterator();
                    Iterator<Long> endTimeIterator = endTimes.iterator();

                    while (routineIterator.hasNext() && endTimeIterator.hasNext()) {
                        RoutineInfo routine = routineIterator.next();
                        long endTime = endTimeIterator.next();
                        if (endTime <= now) {
                            // 1. SharedPreferences에서 루틴 ID 제거
                            removeFromActiveRoutines(routine.getId());

                            routineIterator.remove();
                            endTimeIterator.remove();
                        } else {
                            hasRemaining = true;
                        }
                    }

                    if (hasRemaining) {
                        NotificationManager manager = getSystemService(NotificationManager.class);
                        manager.notify(NOTIFICATION_ID, buildNotification());
                        handler.postDelayed(this, 1000); // 초 단위 업데이트
                    } else {
                        stopForeground(true);
                        stopSelf();
                    }
                }
            };
            handler.post(updateRunnable);
        }

        return START_STICKY;
    }

    private void removeFromActiveRoutines(String routineId) {
        SharedPreferences prefs = getSharedPreferences(SERVICE_PREFS, MODE_PRIVATE);
        Set<String> activeIds = new HashSet<>(prefs.getStringSet(ACTIVE_ROUTINES, new HashSet<>()));
        if (activeIds.remove(routineId)) {
            prefs.edit().putStringSet(ACTIVE_ROUTINES, activeIds).apply();
        }
    }

    private void mergeRoutines(List<RoutineInfo> newRoutines) {
        long now = System.currentTimeMillis();

        if (routines.isEmpty()) {
            for (RoutineInfo routine : newRoutines) {
                routines.add(routine);
                endTimes.add(now + routine.durationMillis);
            }
            return;
        }

        for (RoutineInfo newRoutine : newRoutines) {
            boolean alreadyExists = false;
            for (RoutineInfo existing : routines) {
                if (existing.getId() != null && existing.getId().equals(newRoutine.getId())) {
                    alreadyExists = true;
                    break;
                }
            }
            if (!alreadyExists) {
                routines.add(newRoutine);
                endTimes.add(now + newRoutine.durationMillis);
            }
        }
    }

    private void sortByRemainingTime() {
        long now = System.currentTimeMillis();
        List<Pair> pairs = new ArrayList<>();

        for (int i = 0; i < routines.size(); i++) {
            long remaining = endTimes.get(i) - now;
            if (remaining > 0) {
                pairs.add(new Pair(routines.get(i), endTimes.get(i)));
            }
        }

        pairs.sort(Comparator.comparingLong(p -> p.endTime));

        routines.clear();
        endTimes.clear();
        for (Pair pair : pairs) {
            routines.add(pair.routine);
            endTimes.add(pair.endTime);
        }
    }

    private Notification buildNotification() {
        long now = System.currentTimeMillis();
        StringBuilder content = new StringBuilder();

        for (int i = 0; i < routines.size(); i++) {
            long remaining = Math.max(0, endTimes.get(i) - now);
            if (remaining > 0) {
                String timeFormatted = formatTime(remaining);
                RoutineInfo r = routines.get(i);
                content.append("• ").append(r.title)
                        .append(" @ ").append(r.location)
                        .append(" - ").append(timeFormatted)
                        .append("\n");
            }
        }

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("진행 중인 루틴들")
                .setContentText("자세한 내용을 보려면 확장하세요")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(content.toString()))
                .setSmallIcon(R.drawable.ic_routine)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    private String formatTime(long millis) {
        long minutes = millis / 60000;
        long seconds = (millis % 60000) / 1000;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "진행 중인 루틴",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("현재 진행 중인 루틴의 실시간 상태를 표시합니다.");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static class Pair {
        RoutineInfo routine;
        long endTime;

        Pair(RoutineInfo routine, long endTime) {
            this.routine = routine;
            this.endTime = endTime;
        }
    }
}
