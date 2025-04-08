package com.example.roomi;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignUpActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etName, etNickname, etAge, etSex;
    private Button btnSignUp;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore db;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        etEmail = findViewById(R.id.etSignUpEmail);
        etPassword = findViewById(R.id.etSignUpPassword);
        etName = findViewById(R.id.etSignUpName);
        etNickname = findViewById(R.id.etSignUpNickname);
        etAge = findViewById(R.id.etSignUpAge);
        etSex = findViewById(R.id.etSignUpSex);
        btnSignUp = findViewById(R.id.btnSignUp);

        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        progressDialog = new ProgressDialog(this);

        // 회원가입 버튼 클릭 이벤트
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });
    }

    private void registerUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String name = etName.getText().toString().trim();
        String nickname = etNickname.getText().toString().trim();
        String age = etAge.getText().toString().trim();
        String sex = etSex.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("이메일을 입력하세요.");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("비밀번호를 입력하세요.");
            return;
        }

        if (TextUtils.isEmpty(name)) {
            etName.setError("이름을 입력하세요.");
            return;
        }

        if (TextUtils.isEmpty(nickname)) {
            etNickname.setError("닉네임을 입력하세요.");
            return;
        }

        if (TextUtils.isEmpty(age)) {
            etAge.setError("나이를 입력하세요.");
            return;
        }

        if (TextUtils.isEmpty(sex)) {
            etSex.setError("성별을 입력하세요.");
            return;
        }

        progressDialog.setMessage("회원가입 중...");
        progressDialog.show();

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        // 회원가입 성공 시 사용자 정보 Firestore에 저장
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user != null) {
                            saveUserToFirestore(user.getUid(), email, name, nickname, age, sex);
                        }

                        Toast.makeText(SignUpActivity.this, "회원가입 성공", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
                        finish();
                    } else {
                        Toast.makeText(SignUpActivity.this, "회원가입 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveUserToFirestore(String uid, String email, String name, String nickname, String age, String sex) {
        // Firestore에 저장할 사용자 정보
        Map<String, Object> user = new HashMap<>();
        user.put("email", email);
        user.put("name", name);
        user.put("nickname", nickname);
        user.put("age", age);
        user.put("sex", sex);
        user.put("is_first_login", true);

        user.put("created_at", com.google.firebase.firestore.FieldValue.serverTimestamp());

        db.collection("data").document(uid).collection("user_info").document(uid)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SignUpActivity.this, "사용자 데이터 저장 성공", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SignUpActivity.this, "사용자 데이터 저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
