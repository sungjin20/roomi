package com.example.roomi;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AllroutineFragment extends Fragment {

    private RecyclerView recyclerView;
    private RoutineEditAdapter adapter;
    private List<Routine> routineList;
    private List<Routine> filteredList;
    private FirebaseFirestore db;
    private String userId;
    private Button selectedDayButton;
    private static final String ALARM_PREFS = "alarm_prefs";
    private static final String ALARM_KEYS = "alarm_request_keys";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_allroutine, container, false);

        Button btnAddRoutine = view.findViewById(R.id.btnAddRoutine);

        Button btnAll = view.findViewById(R.id.btnAll);
        Button btnSun = view.findViewById(R.id.btnSun);
        Button btnMon = view.findViewById(R.id.btnMon);
        Button btnTue = view.findViewById(R.id.btnTue);
        Button btnWed = view.findViewById(R.id.btnWed);
        Button btnThu = view.findViewById(R.id.btnThu);
        Button btnFri = view.findViewById(R.id.btnFri);
        Button btnSat = view.findViewById(R.id.btnSat);

        // '모든 요일'을 기본 선택 버튼으로 설정
        selectedDayButton = btnAll;
        setSelectedButton(btnAll); // 중복 호출을 방지

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        recyclerView = view.findViewById(R.id.recyclerViewEdit);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        routineList = new ArrayList<>();
        filteredList = new ArrayList<>();

        adapter = new RoutineEditAdapter(filteredList, this::openEditRoutineFragment, this::deleteRoutineFromFirestore);
        recyclerView.setAdapter(adapter);

        loadRoutinesFromFirestore();

        btnAddRoutine.setOnClickListener(v -> openAddRoutineFragment());

        btnAll.setOnClickListener(v -> {
            setSelectedButton(btnAll);
            filterRoutines("All");
        });
        btnSun.setOnClickListener(v -> {
            setSelectedButton(btnSun);
            filterRoutines("Sun");
        });
        btnMon.setOnClickListener(v -> {
            setSelectedButton(btnMon);
            filterRoutines("Mon");
        });
        btnTue.setOnClickListener(v -> {
            setSelectedButton(btnTue);
            filterRoutines("Tue");
        });
        btnWed.setOnClickListener(v -> {
            setSelectedButton(btnWed);
            filterRoutines("Wed");
        });
        btnThu.setOnClickListener(v -> {
            setSelectedButton(btnThu);
            filterRoutines("Thu");
        });
        btnFri.setOnClickListener(v -> {
            setSelectedButton(btnFri);
            filterRoutines("Fri");
        });
        btnSat.setOnClickListener(v -> {
            setSelectedButton(btnSat);
            filterRoutines("Sat");
        });

        return view;
    }

    private void loadRoutinesFromFirestore() {
        db.collection("data").document(userId).collection("routines")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    routineList.clear();
                    for (DocumentSnapshot snapshot : queryDocumentSnapshots) {
                        Routine routine = snapshot.toObject(Routine.class);
                        if (routine != null) {
                            routineList.add(routine);
                        }
                    }
                    filterRoutines("All");
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(requireContext(), "루틴 불러오기 실패", Toast.LENGTH_SHORT).show());
    }

    private void filterRoutines(String day) {
        filteredList.clear();

        List<Routine> ongoing = new ArrayList<>();
        List<Routine> others = new ArrayList<>();

        for (Routine routine : routineList) {
            boolean matchesDay = day.equals("All") || routine.getDay().contains(day);
            if (matchesDay) {
                if (isRoutineOngoing(routine)) {
                    ongoing.add(routine);
                } else {
                    others.add(routine);
                }
            }
        }

        filteredList.addAll(ongoing);
        filteredList.addAll(others);
        adapter.notifyDataSetChanged();
    }

    private boolean isRoutineOngoing(Routine routine) {
        Calendar now = Calendar.getInstance();
        int dayOfWeek = now.get(Calendar.DAY_OF_WEEK); // 일=1 ~ 토=7
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

    private void setSelectedButton(Button selectedButton) {
        if (selectedDayButton != null && selectedButton != selectedDayButton) {
            selectedDayButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.purple_light));
        }
        selectedButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.green));
        selectedDayButton = selectedButton;
    }


    private void openAddRoutineFragment() {
        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.frame_layout, new AddRoutineFragment());
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void deleteRoutineFromFirestore(Routine routine) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("루틴 삭제")
                .setMessage("정말 삭제하시겠습니까?")
                .setPositiveButton("삭제", (dialog, which) -> {
                    String docId = routine.getId();
                    db.collection("data").document(userId)
                            .collection("routines").document(docId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(requireContext(), "루틴 삭제 완료", Toast.LENGTH_SHORT).show();
                                cancelExistingAlarms();

                                Intent intent = new Intent(requireContext(), MainActivity.class);
                                startActivity(intent);
                                requireActivity().finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(requireContext(), "루틴 삭제 실패", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("취소", null)
                .show();
    }

    private void openEditRoutineFragment(Routine routine) {
        Bundle bundle = new Bundle();
        bundle.putString("name", routine.getTitle());
        bundle.putString("location", routine.getLocation());
        bundle.putString("time", routine.getTime());
        bundle.putString("day", routine.getDay());
        bundle.putString("id", routine.getId());

        RoutineEditDetailFragment editFragment = new RoutineEditDetailFragment();
        editFragment.setArguments(bundle);

        FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
        transaction.replace(R.id.frame_layout, editFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void cancelExistingAlarms() {
        Context context = requireContext();

        SharedPreferences prefs = context.getSharedPreferences(ALARM_PREFS, Context.MODE_PRIVATE);
        Set<String> alarmKeys = prefs.getStringSet(ALARM_KEYS, new HashSet<>());

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        for (String key : alarmKeys) {
            try {
                int requestCode = Integer.parseInt(key);
                Intent intent = new Intent(context, RoutineAlarmReceiver.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        requestCode,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                alarmManager.cancel(pendingIntent);
            } catch (NumberFormatException ignored) {}
        }

        prefs.edit().remove(ALARM_KEYS).apply();

        SharedPreferences servicePrefs = context.getSharedPreferences("service_prefs", Context.MODE_PRIVATE);
        servicePrefs.edit().remove("active_routines").apply();

        Intent stopIntent = new Intent(context, RoutineNotificationService.class);
        context.stopService(stopIntent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (adapter != null) {
            adapter.stopAllUpdates();
        }

        recyclerView.setAdapter(null);
    }

}
