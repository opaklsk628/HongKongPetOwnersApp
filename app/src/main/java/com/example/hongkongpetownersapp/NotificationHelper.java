package com.example.hongkongpetownersapp;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;

public class NotificationHelper {

    private static final String TAG = "NotificationHelper";
    private static final String CHANNEL_ID = "pet_health_reminders";
    private static final String CHANNEL_NAME = "Pet Health Reminders";
    private static final String CHANNEL_DESC = "Notifications for pet health reminders";

    // Create notification channel
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            channel.enableVibration(true);
            channel.setShowBadge(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // Schedule a repeating notification
    public static void scheduleRepeatingNotification(Context context, HealthReminder reminder) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        intent.putExtra("reminderId", reminder.getId());
        intent.putExtra("title", reminder.getTitle());
        intent.putExtra("petName", reminder.getPetName());
        intent.putExtra("type", reminder.getType());

        // Use reminder ID hashcode as request code to ensure uniqueness
        int requestCode = reminder.getId().hashCode();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Calculate first alarm time
        Calendar alarmTime = calculateNextReminderTime(reminder);

        // Schedule based on frequency
        switch (reminder.getFrequency()) {
            case "daily":
                alarmManager.setRepeating(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime.getTimeInMillis(),
                        AlarmManager.INTERVAL_DAY,
                        pendingIntent
                );
                break;

            case "weekly":
            case "monthly":
            case "custom":
                // For non-daily reminders, we'll reschedule after each trigger
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime.getTimeInMillis(),
                        pendingIntent
                );
                break;
        }

        Log.d(TAG, "Scheduled reminder: " + reminder.getTitle() + " at " + alarmTime.getTime());
    }

    // Cancel a notification
    public static void cancelNotification(Context context, String reminderId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        int requestCode = reminderId.hashCode();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
            Log.d(TAG, "Cancelled reminder: " + reminderId);
        }
    }

    // Calculate next reminder time based on frequency
    public static Calendar calculateNextReminderTime(HealthReminder reminder) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, reminder.getHour());
        calendar.set(Calendar.MINUTE, reminder.getMinute());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If the time has already passed today, move to next occurrence
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            switch (reminder.getFrequency()) {
                case "daily":
                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                    break;

                case "weekly":
                    // Find next matching day of week
                    if (reminder.getDaysOfWeek() != null && reminder.getDaysOfWeek().length > 0) {
                        calendar.add(Calendar.DAY_OF_MONTH, 1);
                        int maxDays = 7;
                        while (maxDays > 0 && !isDaySelected(calendar, reminder.getDaysOfWeek())) {
                            calendar.add(Calendar.DAY_OF_MONTH, 1);
                            maxDays--;
                        }
                    } else {
                        calendar.add(Calendar.WEEK_OF_YEAR, 1);
                    }
                    break;

                case "monthly":
                    calendar.add(Calendar.MONTH, 1);
                    if (reminder.getDayOfMonth() > 0) {
                        calendar.set(Calendar.DAY_OF_MONTH,
                                Math.min(reminder.getDayOfMonth(),
                                        calendar.getActualMaximum(Calendar.DAY_OF_MONTH)));
                    }
                    break;

                case "custom":
                    calendar.add(Calendar.DAY_OF_MONTH, reminder.getIntervalDays());
                    break;
            }
        }

        return calendar;
    }

    // Helper method to check if a day is selected
    private static boolean isDaySelected(Calendar calendar, String[] selectedDays) {
        String[] dayNames = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        String currentDay = dayNames[calendar.get(Calendar.DAY_OF_WEEK) - 1];

        for (String day : selectedDays) {
            if (day.equals(currentDay)) {
                return true;
            }
        }
        return false;
    }

    // Show immediate notification (for testing or immediate reminders)
    public static void showNotification(Context context, String title, String message, int notificationId) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

        // Create intent to open app
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        manager.notify(notificationId, builder.build());
    }
}