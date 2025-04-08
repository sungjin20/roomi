package com.example.roomi;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
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

public class AddRoutineFragment extends Fragment {

    private EditText etRoutineName, etLocation;
    private EditText etStartHour, etStartMinute, etEndHour, etEndMinute;
    private Spinner spinnerDay;
    private Button btnSaveRoutine, btnCancelRoutine;
    private static final String ALARM_PREFS = "alarm_prefs";
    private static final String ALARM_KEYS = "alarm_request_keys";
    private FirebaseFirestore db;
    private String userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_routine, container, false);

        etRoutineName = view.findViewById(R.id.etRoutineName);
        etLocation = view.findViewById(R.id.etLocation);
        etStartHour = view.findViewById(R.id.etStartHour);
        etStartMinute = view.findViewById(R.id.etStartMinute);
        etEndHour = view.findViewById(R.id.etEndHour);
        etEndMinute = view.findViewById(R.id.etEndMinute);
        spinnerDay = view.findViewById(R.id.spinnerDay);

// 요일 목록 설정
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.days_array, // 이건 strings.xml에 추가해야 함
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDay.setAdapter(adapter);

        btnSaveRoutine = view.findViewById(R.id.btnSaveRoutine);
        btnCancelRoutine = view.findViewById(R.id.btnCancelRoutine); // 취소 버튼 연결

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        btnSaveRoutine.setOnClickListener(v -> saveRoutineToFirestore());

        // 취소 버튼 클릭 시 프래그먼트 종료
        btnCancelRoutine.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());

        return view;
    }

    private void saveRoutineToFirestore() {
        String name = etRoutineName.getText().toString().trim();
        String location = etLocation.getText().toString().trim();
        String startHour = etStartHour.getText().toString().trim();
        String startMinute = etStartMinute.getText().toString().trim();
        String endHour = etEndHour.getText().toString().trim();
        String endMinute = etEndMinute.getText().toString().trim();
        String day = spinnerDay.getSelectedItem().toString();

        if (TextUtils.isEmpty(startHour) || TextUtils.isEmpty(startMinute)
                || TextUtils.isEmpty(endHour) || TextUtils.isEmpty(endMinute)
                || TextUtils.isEmpty(day) || TextUtils.isEmpty(name) || TextUtils.isEmpty(location)) {
            Toast.makeText(getContext(), "모든 정보를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        String time = startHour + ":" + startMinute + " - " + endHour + ":" + endMinute;

        String timestamp = String.valueOf(System.currentTimeMillis());
        String docId = name + "_" + timestamp;
        Routine routine = new Routine(name, location, time, day, docId);

        db.collection("data").document(userId)
                .collection("routines").document(docId)
                .set(routine)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "루틴이 저장되었습니다.", Toast.LENGTH_SHORT).show();
                    cancelExistingAlarms();

                    Intent intent = new Intent(requireContext(), MainActivity.class);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
