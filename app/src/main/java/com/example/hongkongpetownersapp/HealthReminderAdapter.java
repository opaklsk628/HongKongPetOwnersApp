package com.example.hongkongpetownersapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class HealthReminderAdapter extends RecyclerView.Adapter<HealthReminderAdapter.ReminderViewHolder> {

    private List<HealthReminder> reminders;
    private OnReminderClickListener listener;
    private OnReminderToggleListener toggleListener;

    public interface OnReminderClickListener {
        void onReminderClick(HealthReminder reminder);
    }

    public interface OnReminderToggleListener {
        void onReminderToggle(HealthReminder reminder, boolean isActive);
    }

    public HealthReminderAdapter(List<HealthReminder> reminders,
                                 OnReminderClickListener listener,
                                 OnReminderToggleListener toggleListener) {
        this.reminders = reminders;
        this.listener = listener;
        this.toggleListener = toggleListener;
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_health_reminder, parent, false);
        return new ReminderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        HealthReminder reminder = reminders.get(position);
        holder.bind(reminder);
    }

    @Override
    public int getItemCount() {
        return reminders.size();
    }

    class ReminderViewHolder extends RecyclerView.ViewHolder {
        private TextView textIcon;
        private TextView textTitle;
        private TextView textPetName;
        private TextView textTime;
        private TextView textFrequency;
        private Switch switchActive;

        public ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            textIcon = itemView.findViewById(R.id.text_reminder_icon);
            textTitle = itemView.findViewById(R.id.text_reminder_title);
            textPetName = itemView.findViewById(R.id.text_pet_name);
            textTime = itemView.findViewById(R.id.text_reminder_time);
            textFrequency = itemView.findViewById(R.id.text_frequency);
            switchActive = itemView.findViewById(R.id.switch_active);
        }

        public void bind(HealthReminder reminder) {
            // Set icon
            textIcon.setText(reminder.getTypeIcon());

            // Set title
            textTitle.setText(reminder.getTitle());

            // Set pet name
            textPetName.setText("For " + reminder.getPetName());

            // Set time
            String time = String.format(Locale.getDefault(), "%02d:%02d",
                    reminder.getHour(), reminder.getMinute());
            textTime.setText(time);

            // Set frequency
            String frequencyText = getFrequencyText(reminder);
            textFrequency.setText(frequencyText);

            // Set active state
            switchActive.setChecked(reminder.isActive());

            // Handle switch toggle without triggering during binding
            switchActive.setOnCheckedChangeListener(null);
            switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (toggleListener != null) {
                    toggleListener.onReminderToggle(reminder, isChecked);
                }
            });

            // Handle item click
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReminderClick(reminder);
                }
            });

            // Update UI based on active state
            itemView.setAlpha(reminder.isActive() ? 1.0f : 0.5f);
        }

        private String getFrequencyText(HealthReminder reminder) {
            switch (reminder.getFrequency()) {
                case "daily":
                    return "Every day";
                case "weekly":
                    if (reminder.getDaysOfWeek() != null && reminder.getDaysOfWeek().length > 0) {
                        return "Weekly: " + String.join(", ", reminder.getDaysOfWeek());
                    }
                    return "Weekly";
                case "monthly":
                    return "Monthly (Day " + reminder.getDayOfMonth() + ")";
                case "custom":
                    return "Every " + reminder.getIntervalDays() + " days";
                default:
                    return reminder.getFrequency();
            }
        }
    }
}