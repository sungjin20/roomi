package com.example.roomi;

public class Routine {
    private String title;
    private String location;
    private String time;
    private String day;
    private String id;

    // ⭐ 기본 생성자 (필수)
    public Routine() {
    }

    // 사용자 정의 생성자
    public Routine(String title, String location, String time, String day, String id) {
        this.title = title;
        this.location = location;
        this.time = time;
        this.day = day;
        this.id = id;
    }

    // ⭐ Getter & Setter (Firestore에서 자동 매핑 시 필요)
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
