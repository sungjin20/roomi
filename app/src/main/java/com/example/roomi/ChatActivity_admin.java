package com.example.roomi;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity_admin extends AppCompatActivity {

    private RecyclerView recyclerView;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private ChatMessageAdapter adapter;
    private final List<ChatMessage> messageList = new ArrayList<>();

    private DatabaseReference chatRef;
    private String userUid;
    private String chatRoomId;
    private String userId;  // 사용자 UID (관리자가 아닌 채팅 상대)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_admin);

        Toolbar toolbar = findViewById(R.id.chat_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.chat_recycler_view);
        editTextMessage = findViewById(R.id.edit_text_message);
        buttonSend = findViewById(R.id.button_send);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        userUid = currentUser.getUid();
        chatRoomId = getIntent().getStringExtra("chatRoomId");

        if (TextUtils.isEmpty(chatRoomId)) {
            finish();
            return;
        }

        // 사용자 UID는 chatRoomId에서 관리자UID_사용자UID에서 분리
        String[] parts = chatRoomId.split("_");
        if (parts.length != 2) {
            finish();
            return;
        }

        userId = parts[1];  // 사용자 UID

        adapter = new ChatMessageAdapter(messageList, userUid);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        chatRef = FirebaseDatabase.getInstance()
                .getReference("chatRooms")
                .child(chatRoomId)
                .child("messages");

        // 메시지 전송
        buttonSend.setOnClickListener(v -> {
            String msg = editTextMessage.getText().toString().trim();
            if (!msg.isEmpty()) {
                ChatMessage message = new ChatMessage(msg, userUid, System.currentTimeMillis());
                chatRef.push().setValue(message);
                editTextMessage.setText("");
            }
        });

        // 메시지 수신
        chatRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String previousChildName) {
                ChatMessage message = snapshot.getValue(ChatMessage.class);
                if (message != null) {
                    messageList.add(message);
                    adapter.notifyItemInserted(messageList.size() - 1);
                    recyclerView.scrollToPosition(messageList.size() - 1);
                }
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String previousChildName) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // 툴바에 사용자 이름 표시
        FirebaseFirestore.getInstance()
                .collection("data")
                .document(userId)
                .collection("user_info")
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.contains("name")) {
                        String name = doc.getString("name");
                        getSupportActionBar().setTitle(name + "님과의 채팅");
                    }
                });
    }
}
