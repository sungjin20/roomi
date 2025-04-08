package com.example.roomi;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class QuestionPageFragment extends Fragment {

    private static final String ARG_QUESTION = "question";
    private static final String ARG_OPTIONS = "options";
    private static final String ARG_INDEX = "index";

    private String question;
    private String[] options;
    private int questionIndex;

    private LinearLayout optionsLayout;
    private EditText etAnswer;
    private Button btnNext;
    private boolean isMultiSelect = false;
    private List<ToggleButton> toggleButtons = new ArrayList<>();

    public static QuestionPageFragment newInstance(String question, String[] options, int index) {
        QuestionPageFragment fragment = new QuestionPageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_QUESTION, question);
        args.putStringArray(ARG_OPTIONS, options);
        args.putInt(ARG_INDEX, index);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            question = getArguments().getString(ARG_QUESTION);
            options = getArguments().getStringArray(ARG_OPTIONS);
            questionIndex = getArguments().getInt(ARG_INDEX);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.question_page, container, false);

        TextView tvQuestion = view.findViewById(R.id.tvQuestion);
        optionsLayout = view.findViewById(R.id.optionsLayout);
        etAnswer = view.findViewById(R.id.etAnswer);
        btnNext = view.findViewById(R.id.btnNext);

        tvQuestion.setText(question);

        if (options == null || options.length == 0) {
            // 주관식 질문
            etAnswer.setVisibility(View.VISIBLE);
        } else {
            isMultiSelect = question.contains("복수") || question.contains("다중");
            createOptions(options);
        }

        btnNext.setOnClickListener(v -> {
            String answer;

            if (etAnswer.getVisibility() == View.VISIBLE) {
                answer = etAnswer.getText().toString().trim();
                if (answer.isEmpty()) return;
            } else {
                if (isMultiSelect) {
                    List<String> selected = new ArrayList<>();
                    for (ToggleButton btn : toggleButtons) {
                        if (btn.isChecked()) selected.add(btn.getText().toString());
                    }
                    if (selected.isEmpty()) return;
                    answer = TextUtils.join(", ", selected);
                } else {
                    for (ToggleButton btn : toggleButtons) {
                        if (btn.isChecked()) {
                            answer = btn.getText().toString();
                            ((SleepSurveyActivity) requireActivity()).saveAnswer(questionIndex, answer);
                            ((SleepSurveyActivity) requireActivity()).goToNextQuestion();
                            return;
                        }
                    }
                    return;
                }
            }

            // 저장 및 다음 질문
            ((SleepSurveyActivity) requireActivity()).saveAnswer(questionIndex, answer);
            ((SleepSurveyActivity) requireActivity()).goToNextQuestion();
        });

        return view;
    }

    private void createOptions(String[] options) {
        toggleButtons.clear();
        for (String option : options) {
            ToggleButton btn = new ToggleButton(requireContext());
            btn.setTextOn(option);
            btn.setTextOff(option);
            btn.setText(option);
            btn.setTextSize(16);
            btn.setTextColor(Color.WHITE);
            btn.setBackgroundResource(R.drawable.toggle_selector); // 선택 시 색상 변경 drawable
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 16, 0, 0);
            btn.setLayoutParams(params);

            btn.setOnClickListener(v -> {
                if (!isMultiSelect) {
                    for (ToggleButton otherBtn : toggleButtons) {
                        if (otherBtn != btn) otherBtn.setChecked(false);
                    }
                }
            });

            optionsLayout.addView(btn);
            toggleButtons.add(btn);
        }
    }
}
