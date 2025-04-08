package com.example.roomi;

import java.io.Serializable;

public class RoutineInfo implements Serializable {
    public String title;
    public String location;
    public long durationMillis;
    public String id;

    public RoutineInfo(String title, String location, long durationMillis, String id) {
        this.title = title;
        this.location = location;
        this.durationMillis = durationMillis;
    }

    public String getTitle() {
        return title;
    }

    public String getDuration() {
        long totalSeconds = durationMillis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        StringBuilder durationString = new StringBuilder();
        if (hours > 0) durationString.append(hours).append("시간 ");
        if (minutes > 0) durationString.append(minutes).append("분 ");
        if (seconds > 0 || durationString.length() == 0) durationString.append(seconds).append("초");

        return durationString.toString().trim();
    }

    public String getId() {
        return id;
    }
}
