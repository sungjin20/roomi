package com.example.roomi;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashSet;
import java.util.Set;

public class RoutineEditDetailFragment extends Fragment {

    private EditText etRoutineName, etLocation;
    private EditText etStartHour, etStartMinute, etEndHour, etEndMinute;
    private Spinner spinnerDay;
    private Button btnSave, btnCancel;
    private static final String ALARM_PREFS = "alarm_prefs";
    private static final String ALARM_KEYS = "alarm_request_keys";
    private FirebaseFirestore db;
    private String userId;
    private String docId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_routine_edit_detail, container, false);

        // View 초기화
        etRoutineName = view.findViewById(R.id.etRoutineName);
        etLocation = view.findViewById(R.id.etLocation);
        etStartHour = view.findViewById(R.id.etStartHour);
        etStartMinute = view.findViewById(R.id.etStartMinute);
        etEndHour = view.findViewById(R.id.etEndHour);
        etEndMinute = view.findViewById(R.id.etEndMinute);
        spinnerDay = view.findViewById(R.id.spinnerDay);
        btnSave = view.findViewById(R.id.btnSaveRoutine);
        btnCancel = view.findViewById(R.id.btnCancelEditRoutine);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 요일 Spinner 초기화
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.days_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDay.setAdapter(adapter);

        // 전달된 루틴 정보 설정
        if (getArguments() != null) {
            etRoutineName.setText(getArguments().getString("name"));
            etLocation.setText(getArguments().getString("location"));

            // 시간 파싱: "HH:mm - HH:mm"
            String time = getArguments().getString("time");
            if (time != null && time.contains("-")) {
                String[] times = time.split("-");
                if (times.length == 2) {
                    String[] start = times[0].trim().split(":");
                    String[] end = times[1].trim().split(":");
                    if (start.length == 2 && end.length == 2) {
                        etStartHour.setText(start[0]);
                        etStartMinute.setText(start[1]);
                        etEndHour.setText(end[0]);
                        etEndMinute.setText(end[1]);
                    }
                }
            }

            String day = getArguments().getString("day");
            if (day != null) {
                int position = adapter.getPosition(day);
                spinnerDay.setSelection(position);
            }

            docId = getArguments().getString("id");
        }

        btnSave.setOnClickListener(v -> saveRoutineChanges());
        btnCancel.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        return view;
    }

    private void saveRoutineChanges() {
        String name = etRoutineName.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String startHour = etStartHour.getText().toString().trim();
        String startMinute = etStartMinute.getText().toString().trim();
        String endHour = etEndHour.getText().toString().trim();
        String endMinute = etEndMinute.getText().toString().trim();
        String day = spinnerDay.getSelectedItem().toString();

        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(location) ||
                TextUtils.isEmpty(startHour) || TextUtils.isEmpty(startMinute) ||
                TextUtils.isEmpty(endHour) || TextUtils.isEmpty(endMinute) ||
                TextUtils.isEmpty(day)) {
            Toast.makeText(requireContext(), "모든 정보를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String time = startHour + ":" + startMinute + " - " + endHour + ":" + endMinute;

        if (docId == null || docId.isEmpty()) {
            Toast.makeText(requireContext(), "루틴 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("data").document(userId)
                .collection("routines").document(docId)
                .update("name", name,
                        "location", location,
                        "time", time,
                        "day", day)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(requireContext(), "루틴이 수정되었습니다.", Toast.LENGTH_SHORT).show();
                    cancelExistingAlarms();

                    Intent intent = new Intent(requireContext(), MainActivity.class);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(requireContext(), "수정 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("Firestore", "Error updating routine", e);
                });
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
}
