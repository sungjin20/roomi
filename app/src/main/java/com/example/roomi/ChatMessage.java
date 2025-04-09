package com.example.roomi;

public class ChatMessage {
    private String message;
    private String sender; // "user" or "bot"
    private long timestamp;

    public ChatMessage() {} // Firebase용 기본 생성자

    public ChatMessage(String message, String sender, long timestamp) {
        this.message = message;
        this.sender = sender;
        this.timestamp = timestamp;
    }

    public String getMessage() { return message; }
    public String getSender() { return sender; }
    public long getTimestamp() { return timestamp; }
}
