package com.example.roomi;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etId);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        progressDialog = new ProgressDialog(this);

        // 로그인 버튼 클릭 이벤트
        btnLogin.setOnClickListener(v -> loginUser());

        // 회원가입 버튼 클릭 이벤트
        Button btnSignUp = findViewById(R.id.btnSignUp);
        btnSignUp.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("이메일을 입력하세요.");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("비밀번호를 입력하세요.");
            return;
        }

        progressDialog.setMessage("로그인 중...");
        progressDialog.show();

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        // 로그인 성공 시 항상 자동 로그인 설정
                        SharedPreferences prefs = getSharedPreferences("login_prefs", MODE_PRIVATE);
                        prefs.edit().putBoolean("stay_logged_in", true).apply();

                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            checkFirstLogin(user.getUid()); // 로그인 성공 후 is_first_login 확인
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "로그인 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkFirstLogin(String userId) {
        DocumentReference userRef = db.collection("data").document(userId).collection("user_info").document(userId);

        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists() && documentSnapshot.contains("is_first_login")) {
                boolean isFirstLogin = documentSnapshot.getBoolean("is_first_login");

                if (isFirstLogin) {
                    userRef.update("is_first_login", false)
                            .addOnSuccessListener(aVoid -> {
                                Intent intent = new Intent(LoginActivity.this, RoutineIntroActivity.class);
                                startActivity(intent);
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(LoginActivity.this, "데이터 업데이트 실패", Toast.LENGTH_SHORT).show());
                } else {
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    startActivity(intent);
                    finish();
                }
            } else {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        }).addOnFailureListener(e -> Toast.makeText(LoginActivity.this, "DB 조회 실패", Toast.LENGTH_SHORT).show());
    }
}
