package com.example.roomi;

import android.animation.ValueAnimator;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RoutineEditAdapter extends RecyclerView.Adapter<RoutineEditAdapter.ViewHolder> {

    private List<Routine> routineList;
    private OnRoutineEditListener editListener;
    private OnRoutineDeleteListener deleteListener;
    private final List<ViewHolder> activeHolders = new ArrayList<>();

    public RoutineEditAdapter(List<Routine> routineList, OnRoutineEditListener editListener, OnRoutineDeleteListener deleteListener) {
        this.routineList = routineList;
        this.editListener = editListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_routine_edit, parent, false);
        return new ViewHolder(view);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Routine routine = routineList.get(position);
        holder.bind(routine);
        activeHolders.add(holder);

        holder.btnEdit.setOnClickListener(v -> {
            if (editListener != null) editListener.onEdit(routine);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDelete(routine);
        });
    }

    @Override
    public int getItemCount() {
        return routineList.size();
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.cleanup();
        activeHolders.remove(holder);
    }

    public void stopAllUpdates() {
        for (ViewHolder holder : activeHolders) {
            holder.cleanup();
        }
        activeHolders.clear();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, location, time;
        Button btnEdit, btnDelete;
        ProgressBar progressBar;
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable updateRunnable;
        ValueAnimator animator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvRoutineName);
            location = itemView.findViewById(R.id.tvRoutineLocation);
            time = itemView.findViewById(R.id.tvRoutineTime);
            btnEdit = itemView.findViewById(R.id.btnEditRoutine);
            btnDelete = itemView.findViewById(R.id.btnDeleteRoutine);
            progressBar = itemView.findViewById(R.id.progressBar); // 반드시 item_routine_edit.xml에 추가되어 있어야 함
        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        public void bind(Routine routine) {
            title.setText(routine.getTitle());
            location.setText(routine.getLocation());
            time.setText(routine.getTime());
            progressBar.setVisibility(View.GONE);
            progressBar.setProgress(0);
            cleanup();

            updateRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        String today = LocalDate.now().getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH);
                        if (!routine.getDay().equalsIgnoreCase(today)) {
                            progressBar.setVisibility(View.GONE);
                            return;
                        }

                        String[] times = routine.getTime().split(" - ");
                        if (times.length == 2) {
                            LocalTime start = LocalTime.parse(times[0].trim(), DateTimeFormatter.ofPattern("HH:mm"));
                            LocalTime end = LocalTime.parse(times[1].trim(), DateTimeFormatter.ofPattern("HH:mm"));
                            LocalTime now = LocalTime.now();

                            if (!now.isBefore(start) && !now.isAfter(end)) {
                                long total = Duration.between(start, end).toSeconds();
                                long current = Duration.between(start, now).toSeconds();
                                int newProgress = (int) ((current * 100) / total);

                                progressBar.setVisibility(View.VISIBLE);
                                animateProgressBar(progressBar.getProgress(), newProgress);
                            } else {
                                progressBar.setVisibility(View.GONE);
                                progressBar.setProgress(0);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    handler.postDelayed(this, 1000);
                }
            };

            handler.post(updateRunnable);
        }

        private void animateProgressBar(int from, int to) {
            if (animator != null && animator.isRunning()) animator.cancel();

            animator = ValueAnimator.ofInt(from, to);
            animator.setDuration(500);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(animation ->
                    progressBar.setProgress((int) animation.getAnimatedValue()));
            animator.start();
        }

        public void cleanup() {
            if (updateRunnable != null) {
                handler.removeCallbacks(updateRunnable);
                updateRunnable = null;
            }
            if (animator != null && animator.isRunning()) {
                animator.cancel();
            }
        }
    }

    public interface OnRoutineEditListener {
        void onEdit(Routine routine);
    }

    public interface OnRoutineDeleteListener {
        void onDelete(Routine routine);
    }
}
