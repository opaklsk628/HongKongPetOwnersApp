package com.example.hongkongpetownersapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class ReminderBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "ReminderReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Reminder broadcast received");

        String reminderId = intent.getStringExtra("reminderId");
        String title = intent.getStringExtra("title");
        String petName = intent.getStringExtra("petName");
        String type = intent.getStringExtra("type");
        String frequency = intent.getStringExtra("frequency");

        if (title == null || petName == null) {
            Log.e(TAG, "Missing reminder data");
            return;
        }

        // Create notification message
        String message = "Time for " + petName + "'s " + title;

        // Get icon based on type
        String icon = "⏰";
        if (type != null) {
            switch (type) {
                case "feeding": icon = "🍽️"; break;
                case "medication": icon = "💊"; break;
                case "exercise": icon = "🏃"; break;
                case "checkup": icon = "🏥"; break;
                case "grooming": icon = "✂️"; break;
            }
        }

        // Show notification
        String notificationTitle = icon + " Pet Reminder";
        int notificationId = reminderId != null ? reminderId.hashCode() : (int) System.currentTimeMillis();

        LocalNotificationHelper.showNotification(context, notificationTitle, message, notificationId);

        // For non-daily reminders, reschedule the next occurrence
        if (frequency != null && !"daily".equals(frequency) && reminderId != null) {
            // Create a minimal reminder object for rescheduling
            HealthReminder reminder = new HealthReminder();
            reminder.setId(reminderId);
            reminder.setTitle(title);
            reminder.setPetName(petName);
            reminder.setType(type);
            reminder.setFrequency(frequency);
            reminder.setHour(intent.getIntExtra("hour", 9));
            reminder.setMinute(intent.getIntExtra("minute", 0));
            reminder.setIntervalDays(intent.getIntExtra("intervalDays", 1));
            reminder.setActive(true);

            // Reschedule for next occurrence
            LocalNotificationHelper.rescheduleNotification(context, reminder);
        }
    }
}