package com.example.roomi;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatMessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_USER = 0;
    private static final int TYPE_BOT = 1;
    private final List<ChatMessage> messageList;
    private final String currentUserId; // 🔸 현재 로그인한 사용자 UID

    public ChatMessageAdapter(List<ChatMessage> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        // 🔸 현재 유저 UID와 메시지 sender UID를 비교
        return messageList.get(position).getSender().equals(currentUserId) ? TYPE_USER : TYPE_BOT;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_USER) {
            View view = inflater.inflate(R.layout.chat_item_user, parent, false);
            return new UserViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.chat_item_bot, parent, false);
            return new BotViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position);

        if (holder.getItemViewType() == TYPE_USER) {
            ((UserViewHolder) holder).userMsg.setText(message.getMessage());
        } else {
            ((BotViewHolder) holder).botMsg.setText(message.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView userMsg;

        UserViewHolder(View itemView) {
            super(itemView);
            userMsg = itemView.findViewById(R.id.text_user_message);
        }
    }

    static class BotViewHolder extends RecyclerView.ViewHolder {
        TextView botMsg;

        BotViewHolder(View itemView) {
            super(itemView);
            botMsg = itemView.findViewById(R.id.text_bot_message);
        }
    }
}
