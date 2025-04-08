package com.example.roomi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SleepSurveyActivity extends AppCompatActivity {

    private List<String> questions;
    private List<String[]> optionsList;
    private HashMap<Integer, String> answers;
    private int currentQuestionIndex = 0;

    private FirebaseFirestore db;
    private FirebaseUser currentUser;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sleep_survey);

        db = FirebaseFirestore.getInstance();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // 질문과 선택지 초기화
        initializeSurveyData();
        answers = new HashMap<>();

        // 첫 질문 로드
        showQuestion(currentQuestionIndex);
    }

    private void initializeSurveyData() {
        questions = new ArrayList<>();
        optionsList = new ArrayList<>();

        questions.add("성별을 입력해주세요");
        optionsList.add(new String[]{"남성", "여성"});

        questions.add("나이를 입력해주세요");
        optionsList.add(new String[]{"19세 이하", "20-25", "25-29", "30대", "40대", "50대", "60대", "70대 이상"});

        questions.add("직업을 알려주세요");
        optionsList.add(new String[]{"기술직", "사무직", "자영업", "사무직", "관리직", "판매직", "서비스직", "교직", "전문직", "전업주부", "대학생", "대학원생", "무직", "기타 (직접입력)"});

        questions.add("평균적으로 하루에 몇 시간 정도 주무시나요?");
        optionsList.add(new String[]{"4시간 이하", "5시간", "6시간", "7시간", "8시간", "9시간", "10시간 이상"});

        questions.add("잠들기까지 얼마나 걸리나요?");
        optionsList.add(new String[]{"0-15분", "16-30분", "31-45분", "46-60분", "61-120분", "120분 이상"});

        questions.add("밤에 자는 동안 깬다면, 총 얼마나 오랫동안 깨어 있나요?");
        optionsList.add(new String[]{"0-15분", "16-30분", "31-45분", "46-60분", "61-120분", "120분 이상"});

        questions.add("계획했던 시간보다 일찍 깨서 억지로 하루를 시작한다면, 얼마나 빨리 깨나요?");
        optionsList.add(new String[]{"더 일찍 깨지 않음", "0-15분", "16-30분", "31-45분", "46-60분", "61-120분", "120분 이상"});

        questions.add("일주일 중 며칠 동안 수면 문제를 경험하나요?");
        optionsList.add(new String[]{"0-1일", "2일", "3일", "4일", "5-7일"});

        questions.add("당신의 수면의 질을 평가한다면 어떻습니까?");
        optionsList.add(new String[]{"매우 좋음", "좋음", "보통", "좋지 않음", "매우 좋지 않음"});

        questions.add("당신의 수면문제는 얼마나 오랫동안 지속되었나요?");
        optionsList.add(new String[]{"문제없음/1개월 미만", "1-2개월", "3-6개월", "6-12개월", "1년 초과"});

        questions.add("지난 한달간 부족한 수면이 당신 기분, 에너지 또는 대인 관계에 얼마나 영향을 미쳤나요?");
        optionsList.add(new String[]{"전혀 아니다", "조금 아니다", "약간 그렇다", "그렇다", "매우 그렇다"});

        questions.add("지난 한달간 부족한 수면이 집중력, 생산성, 깨어 있는 상태 유지에 얼마나 영향을 미쳤나요?");
        optionsList.add(new String[]{"전혀 아니다", "조금 아니다", "약간 그렇다", "그렇다", "매우 그렇다"});

        questions.add("지난 한달간 부족한 수면이 일상생활에 전반적으로 얼마나 영향을 미쳤나요?");
        optionsList.add(new String[]{"전혀 아니다", "조금 아니다", "약간 그렇다", "그렇다", "매우 그렇다"});

        questions.add("낮 시간을 활동적으로 보내지 않나요? (하루 평균 걸음 수 6000보 이상)");
        optionsList.add(new String[]{"네", "아니요"});

        questions.add("낮잠을 자주 자고, 보통 30분 이상 낮잠을 자나요?");
        optionsList.add(new String[]{"네", "아니요"});

        questions.add("밤에 자다가 2회 이상 깨서 화장실에 가나요?");
        optionsList.add(new String[]{"네", "아니요"});

        questions.add("깊은 잠을 못자고 선잠을 자나요?");
        optionsList.add(new String[]{"네", "아니요"});

        questions.add("잠들기 전, 이런저런 걱정 거리들을 많이 떠올리는 편인가요?");
        optionsList.add(new String[]{"네", "아니요"});

        questions.add("생각이 많아져 잠드는데 어려움을 겪으시나요?");
        optionsList.add(new String[]{"네", "아니요"});

        questions.add("잠들기 전, “오늘도 못 자면 어떡하지?” 같은 생각에 스트레스를 느끼시나요?");
        optionsList.add(new String[]{"네", "아니요"});

        questions.add("잠들기 전 잠이 오다가도 잠에 대한 생각을 하면 졸림이 사라지나요?");
        optionsList.add(new String[]{"네", "아니요"});

        questions.add("잠을 못자는 원인이 무엇이라고 생각하나요? (복수 응답)");
        optionsList.add(new String[]{"자기 전 생각이 너무 많아요", "잠에 대한 불안감이 있어요", "커피를 너무 많이 섭취해요", "늦게까지 잠을 못자요", "핸드폰을 너무 많이 해요", "생활습관이 불규칙적이에요", "스트레스를 많이 받아요", "기타 (직접 입력)"});

        questions.add("“내 수면 문제의 원인은 알지만, 어떻게 해결해야 할지 모르겠어요.” 공감하시나요?");
        optionsList.add(new String[]{"네", "아니요"});

        questions.add("“수면을 개선하고 싶었지만, 시도했던 방법들이 지속적으로 효과를 보지 못했어요.” 공감하시나요?");
        optionsList.add(new String[]{"네", "아니요"});

        questions.add("지금까지 수면을 개선하기 위해 어떤 방법들을 시도했는지 알려주세요");
        optionsList.add(new String[]{});

        questions.add("현재 수면을 위해 하고 있는 일들을 알려주세요");
        optionsList.add(new String[]{});

        questions.add("수면 목표를 알려주세요 (복수 선택 가능)");
        optionsList.add(new String[]{"더 빨리 잠들고 싶어요", "밤새 깨지 않고 푹 자고 싶어요", "계획했던 시간보다 일찍 일어나고 싶지 않아요", "깊은 수면 시간을 늘리고 싶어요"});

        questions.add("지금 이 순간, 수면을 개선하고 싶은 마음은 어느 정도인가요?");
        optionsList.add(new String[]{"당장 시작하고 싶어요", "희망을 가지고 있어요", "효과가 없을까봐 자신이 없어요", "잘 모르겠어요"});
    }

    public void showQuestion(int index) {
        if (index < questions.size()) {
            String question = questions.get(index);
            String[] options = optionsList.get(index);

            QuestionPageFragment fragment = QuestionPageFragment.newInstance(question, options, index);

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.fragment_container, fragment);  // XML에서 이 ID를 사용해야 함
            transaction.commit();
        } else {
            finishSurvey();
        }
    }

    public void saveAnswer(int questionIndex, String answer) {
        answers.put(questionIndex, answer);
    }

    public void goToNextQuestion() {
        currentQuestionIndex++;
        showQuestion(currentQuestionIndex);
    }

    private void finishSurvey() {
        if (currentUser == null) {
            Toast.makeText(this, "로그인 정보를 확인할 수 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = currentUser.getUid();

        // 답변 데이터를 Map 형식으로 변환
        HashMap<String, Object> resultData = new HashMap<>();
        for (Integer key : answers.keySet()) {
            resultData.put("question_" + key, answers.get(key));
        }

        // Firestore에 답변 저장
        db.collection("data")
                .document(userId)
                .collection("first_form_result")
                .document(userId)
                .set(resultData)
                .addOnSuccessListener(aVoid -> updateUserFirstLoginStatus(userId))
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    Toast.makeText(SleepSurveyActivity.this, "설문 저장에 실패했습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    // 유저 상태 업데이트 및 화면 전환 처리
    private void updateUserFirstLoginStatus(String userId) {
        DocumentReference userRef = db.collection("data")
                .document(userId)
                .collection("user_info")
                .document(userId);

        userRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("is_first_login")) {
                        userRef.update("is_first_login", false)
                                .addOnSuccessListener(aVoid -> navigateToMainActivity())
                                .addOnFailureListener(e -> {
                                    e.printStackTrace();
                                    Toast.makeText(SleepSurveyActivity.this, "유저 상태 업데이트 실패", Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        navigateToMainActivity();
                    }
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    Toast.makeText(SleepSurveyActivity.this, "유저 정보 조회 실패", Toast.LENGTH_SHORT).show();
                });
    }

    // MainActivity로 이동
    private void navigateToMainActivity() {
        Intent intent = new Intent(SleepSurveyActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // 뒤로가기 방지
        startActivity(intent);
        finish();
    }
}
