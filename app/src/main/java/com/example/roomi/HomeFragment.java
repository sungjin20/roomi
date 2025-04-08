package com.example.roomi;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private RoutineAdapter adapter;
    private List<Routine> routineList;
    private FirebaseFirestore db;
    private String userId;

    private static final String ALARM_PREFS = "alarm_prefs";
    private static final String ALARM_KEYS = "alarm_request_keys";
    private static final String SERVICE_PREFS = "service_prefs";
    private static final String ACTIVE_ROUTINES = "active_routines";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        routineList = new ArrayList<>();
        adapter = new RoutineAdapter(routineList);
        recyclerView.setAdapter(adapter);

        loadRoutinesFromFirestore();

        return view;
    }

    private void loadRoutinesFromFirestore() {
        db.collection("data").document(userId).collection("routines")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    routineList.clear();

                    List<Routine> ongoing = new ArrayList<>();
                    List<Routine> others = new ArrayList<>();

                    for (DocumentSnapshot snapshot : queryDocumentSnapshots) {
                        Routine routine = snapshot.toObject(Routine.class);
                        if (routine != null) {
                            routine.setId(snapshot.getId());
                            if (isRoutineOngoing(routine)) {
                                ongoing.add(routine);
                            } else {
                                others.add(routine);
                            }
                        }
                    }

                    routineList.addAll(ongoing);
                    routineList.addAll(others);
                    adapter.notifyDataSetChanged();

                    List<RoutineInfo> routineInfos = new ArrayList<>();
                    Calendar now = Calendar.getInstance();
                    long currentMillis = now.get(Calendar.HOUR_OF_DAY) * 3600_000L
                            + now.get(Calendar.MINUTE) * 60_000L
                            + now.get(Calendar.SECOND) * 1_000L
                            + now.get(Calendar.MILLISECOND);

                    SharedPreferences servicePrefs = requireContext().getSharedPreferences(SERVICE_PREFS, Context.MODE_PRIVATE);
                    Set<String> existingRoutineIds = new HashSet<>(servicePrefs.getStringSet(ACTIVE_ROUTINES, new HashSet<>()));
                    Set<String> newRoutineIds = new HashSet<>();

                    for (Routine routine : ongoing) {
                        String time = routine.getTime();
                        if (time == null || !time.contains("-")) continue;

                        String routineId = routine.getId();
                        if (routineId == null || existingRoutineIds.contains(routineId)) continue;

                        String[] times = time.split("-");
                        String[] endParts = times[1].trim().split(":");
                        int endHour = Integer.parseInt(endParts[0]);
                        int endMinute = Integer.parseInt(endParts[1]);
                        int endSecond = endParts.length > 2 ? Integer.parseInt(endParts[2]) : 0;

                        long endMillis = endHour * 3600_000L + endMinute * 60_000L + endSecond * 1_000L;
                        long remainingMillis = endMillis - currentMillis;

                        if (remainingMillis <= 0) continue;

                        routineInfos.add(new RoutineInfo(
                                routine.getTitle(),
                                routine.getLocation(),
                                remainingMillis,
                                routine.getId()
                        ));
                        newRoutineIds.add(routineId);
                    }

                    if (!routineInfos.isEmpty()) {
                        Intent intent = new Intent(requireContext(), RoutineNotificationService.class);
                        intent.putExtra("routine_list", new ArrayList<>(routineInfos));
                        ContextCompat.startForegroundService(requireContext(), intent);

                        existingRoutineIds.addAll(newRoutineIds);
                        servicePrefs.edit().putStringSet(ACTIVE_ROUTINES, existingRoutineIds).apply();
                    }

                    for (Routine routine : others) {
                        scheduleRoutineAlarm(requireContext(), routine);
                    }

                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "루틴 불러오기 실패", Toast.LENGTH_SHORT).show());
    }

    private int[] parseTimeToHMS(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            int second = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return new int[]{hour, minute, second};
        } catch (Exception e) {
            return new int[]{-1, -1, -1};
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private void scheduleRoutineAlarm(Context context, Routine routine) {
        String time = routine.getTime();
        if (time == null || !time.contains("-")) return;

        String[] times = time.split("-");
        int[] startHMS = parseTimeToHMS(times[0].trim());
        int[] endHMS = parseTimeToHMS(times[1].trim());

        if (startHMS[0] < 0 || endHMS[0] < 0) return;

        Calendar base = Calendar.getInstance();
        base.set(Calendar.SECOND, startHMS[2]);
        base.set(Calendar.MILLISECOND, 0);
        base.set(Calendar.HOUR_OF_DAY, startHMS[0]);
        base.set(Calendar.MINUTE, startHMS[1]);

        String[] daysKor = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

        SharedPreferences prefs = context.getSharedPreferences(ALARM_PREFS, Context.MODE_PRIVATE);
        Set<String> keys = new HashSet<>(prefs.getStringSet(ALARM_KEYS, new HashSet<>()));

        for (int i = 0; i < daysKor.length; i++) {
            if (routine.getDay() != null && routine.getDay().contains(daysKor[i])) {
                Calendar alarmTime = (Calendar) base.clone();
                int today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
                int target = i == 0 ? Calendar.SUNDAY : i + 1;
                int diff = (target - today + 7) % 7;
                if (diff == 0) {
                    Calendar now = Calendar.getInstance();
                    int nowTotal = now.get(Calendar.HOUR_OF_DAY) * 3600 + now.get(Calendar.MINUTE) * 60 + now.get(Calendar.SECOND);
                    int startTotal = startHMS[0] * 3600 + startHMS[1] * 60 + startHMS[2];
                    if (nowTotal >= startTotal) {
                        diff = 7;
                    }
                }
                alarmTime.add(Calendar.DATE, diff);

                long triggerAtMillis = alarmTime.getTimeInMillis();
                long durationMillis = ((endHMS[0] * 3600 + endHMS[1] * 60 + endHMS[2]) -
                        (startHMS[0] * 3600 + startHMS[1] * 60 + startHMS[2])) * 1000L;
                if (durationMillis <= 0) continue;

                Intent intent = new Intent(context, RoutineAlarmReceiver.class);
                intent.putExtra("title", routine.getTitle());
                intent.putExtra("location", routine.getLocation());
                intent.putExtra("duration", durationMillis);
                intent.putExtra("id", routine.getId());

                int requestCode = routine.getId().hashCode();
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                if (!keys.contains(String.valueOf(requestCode))) {
                    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                    if (alarmManager != null) {
                        alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                triggerAtMillis,
                                pendingIntent
                        );
                    }
                    keys.add(String.valueOf(requestCode));
                }
            }
        }

        prefs.edit().putStringSet(ALARM_KEYS, keys).apply();
    }

    private boolean isRoutineOngoing(Routine routine) {
        Calendar now = Calendar.getInstance();
        int dayOfWeek = now.get(Calendar.DAY_OF_WEEK);
        int currentHour = now.get(Calendar.HOUR_OF_DAY);
        int currentMinute = now.get(Calendar.MINUTE);
        int currentTime = currentHour * 60 + currentMinute;

        String[] daysKor = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        String today = daysKor[dayOfWeek - 1];

        String routineDaysStr = routine.getDay();
        if (routineDaysStr == null || routine.getTime() == null) return false;

        String[] routineDays = routineDaysStr.split(",\\s*");
        boolean isTodayIncluded = false;
        for (String day : routineDays) {
            if (day.equalsIgnoreCase(today)) {
                isTodayIncluded = true;
                break;
            }
        }
        if (!isTodayIncluded) return false;

        String[] times = routine.getTime().split("-");
        if (times.length != 2) return false;

        int startMinutes = timeToMinutes(times[0].trim());
        int endMinutes = timeToMinutes(times[1].trim());

        return currentTime >= startMinutes && currentTime <= endMinutes;
    }

    private int timeToMinutes(String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            return hour * 60 + minute;
        } catch (Exception e) {
            return -1;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (recyclerView != null && recyclerView.getAdapter() instanceof RoutineAdapter) {
            ((RoutineAdapter) recyclerView.getAdapter()).stopAllUpdates();
        }
    }
}
