package com.example.roomi;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class ChatRoomListActivity extends AppCompatActivity {

    private ListView chatRoomListView;
    private final List<String> chatRoomDisplayList = new ArrayList<>();
    private final List<String> chatRoomIdList = new ArrayList<>();
    private ArrayAdapter<String> adapter;

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room_list_admin);

        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("채팅방 목록");

        chatRoomListView = findViewById(R.id.chat_room_list_view);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, chatRoomDisplayList);
        chatRoomListView.setAdapter(adapter);

        loadChatRooms();

        chatRoomListView.setOnItemClickListener((parent, view, position, id) -> {
            String chatRoomId = chatRoomIdList.get(position);
            Intent intent = new Intent(ChatRoomListActivity.this, ChatActivity_admin.class);
            intent.putExtra("chatRoomId", chatRoomId);
            startActivity(intent);
        });
    }

    private void loadChatRooms() {
        DatabaseReference chatRoomsRef = FirebaseDatabase.getInstance().getReference("chatRooms");
        chatRoomsRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                chatRoomDisplayList.clear();
                chatRoomIdList.clear();
                adapter.notifyDataSetChanged();

                for (DataSnapshot snapshot : task.getResult().getChildren()) {
                    String chatRoomKey = snapshot.getKey();
                    if (chatRoomKey == null) continue;

                    // 관리자ID_사용자UID 형식에서 사용자UID만 분리
                    String[] parts = chatRoomKey.split("_");
                    if (parts.length != 2) continue;

                    String userId = parts[1]; // 실제 사용자 UID
                    chatRoomIdList.add(chatRoomKey); // 전체 키를 ID로 저장 (채팅방 이동용)
                    int index = chatRoomIdList.size() - 1;

                    // Firestore에서 이름 가져오기
                    firestore.collection("data")
                            .document(userId)
                            .collection("user_info")
                            .document(userId)
                            .get()
                            .addOnCompleteListener(nameTask -> {
                                String displayName = userId;
                                if (nameTask.isSuccessful()) {
                                    DocumentSnapshot doc = nameTask.getResult();
                                    if (doc != null && doc.contains("name")) {
                                        displayName = doc.getString("name") + " (" + userId + ")";
                                    }
                                }

                                // 이름을 표시 리스트에 삽입
                                if (chatRoomDisplayList.size() <= index) {
                                    chatRoomDisplayList.add(displayName);
                                } else {
                                    chatRoomDisplayList.set(index, displayName);
                                }

                                adapter.notifyDataSetChanged();
                            });

                    // 초기에는 UID만 표시
                    chatRoomDisplayList.add(userId);
                    adapter.notifyDataSetChanged();
                }
            } else {
                Toast.makeText(this, "채팅방 불러오기 실패", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 로그아웃 메뉴 연결
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat_room_list, menu);
        return true;
    }

    // 로그아웃 처리
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(ChatRoomListActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
