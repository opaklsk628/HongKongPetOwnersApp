package com.example.hongkongpetownersapp;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.Calendar;

public class LocalNotificationHelper {

    private static final String TAG = "LocalNotificationHelper";
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
            channel.enableLights(true);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created");
            }
        }
    }

    // Schedule a repeating notification
    public static void scheduleRepeatingNotification(Context context, HealthReminder reminder) {
        if (!reminder.isActive()) {
            Log.d(TAG, "Reminder is not active, skipping scheduling");
            return;
        }

        // Check notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Notification permission not granted");
                Toast.makeText(context, "Please enable notification permission", Toast.LENGTH_LONG).show();
                return;
            }
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null");
            return;
        }

        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        intent.setAction("com.example.hongkongpetownersapp.REMINDER_NOTIFICATION");
        intent.putExtra("reminderId", reminder.getId());
        intent.putExtra("title", reminder.getTitle());
        intent.putExtra("petName", reminder.getPetName());
        intent.putExtra("type", reminder.getType());
        intent.putExtra("frequency", reminder.getFrequency());
        intent.putExtra("hour", reminder.getHour());
        intent.putExtra("minute", reminder.getMinute());
        intent.putExtra("intervalDays", reminder.getIntervalDays());

        // Use reminder ID hashcode as request code to ensure uniqueness
        int requestCode = Math.abs(reminder.getId().hashCode());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Calculate first alarm time
        Calendar alarmTime = calculateNextReminderTime(reminder);
        Log.d(TAG, "Next reminder time: " + alarmTime.getTime());

        // Cancel any existing alarm
        alarmManager.cancel(pendingIntent);

        // Schedule alarm
        try {
            if ("daily".equals(reminder.getFrequency())) {
                // For daily reminders
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setRepeating(
                            AlarmManager.RTC_WAKEUP,
                            alarmTime.getTimeInMillis(),
                            AlarmManager.INTERVAL_DAY,
                            pendingIntent
                    );
                } else {
                    alarmManager.setRepeating(
                            AlarmManager.RTC_WAKEUP,
                            alarmTime.getTimeInMillis(),
                            AlarmManager.INTERVAL_DAY,
                            pendingIntent
                    );
                }
                Log.d(TAG, "✓ Scheduled daily reminder: " + reminder.getTitle());
            } else {
                // For other frequencies
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12+ requires special permission for exact alarms
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                alarmTime.getTimeInMillis(),
                                pendingIntent
                        );
                        Log.d(TAG, "✓ Scheduled exact reminder: " + reminder.getTitle());
                    } else {
                        // Fallback to inexact alarm
                        alarmManager.set(
                                AlarmManager.RTC_WAKEUP,
                                alarmTime.getTimeInMillis(),
                                pendingIntent
                        );
                        Log.d(TAG, "✓ Scheduled inexact reminder: " + reminder.getTitle());
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            alarmTime.getTimeInMillis(),
                            pendingIntent
                    );
                    Log.d(TAG, "✓ Scheduled exact reminder: " + reminder.getTitle());
                } else {
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            alarmTime.getTimeInMillis(),
                            pendingIntent
                    );
                    Log.d(TAG, "✓ Scheduled exact reminder: " + reminder.getTitle());
                }
            }

            // Show confirmation
            String timeStr = String.format("%02d:%02d", reminder.getHour(), reminder.getMinute());
            Toast.makeText(context,
                    "Reminder set for " + timeStr,
                    Toast.LENGTH_SHORT).show();

        } catch (SecurityException e) {
            Log.e(TAG, "Failed to schedule alarm - permission denied", e);
            Toast.makeText(context,
                    "Cannot schedule alarm. Please check app permissions.",
                    Toast.LENGTH_LONG).show();
        }
    }

    // Cancel a notification
    public static void cancelNotification(Context context, String reminderId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        intent.setAction("com.example.hongkongpetownersapp.REMINDER_NOTIFICATION");
        int requestCode = Math.abs(reminderId.hashCode());

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
                    int days = reminder.getIntervalDays() > 0 ? reminder.getIntervalDays() : 1;
                    calendar.add(Calendar.DAY_OF_MONTH, days);
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

    // Show immediate notification
    public static void showNotification(Context context, String title, String message, int notificationId) {
        Log.d(TAG, "Showing notification - Title: " + title + ", Message: " + message);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) {
            Log.e(TAG, "NotificationManager is null");
            return;
        }

        // Create intent to open app
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        // Show notification
        try {
            manager.notify(notificationId, builder.build());
            Log.d(TAG, "✓ Notification shown successfully");
        } catch (Exception e) {
            Log.e(TAG, "✗ Failed to show notification", e);
        }
    }

    // Reschedule notification after it's triggered (for non-daily reminders)
    public static void rescheduleNotification(Context context, HealthReminder reminder) {
        if (reminder.isActive() && !"daily".equals(reminder.getFrequency())) {
            scheduleRepeatingNotification(context, reminder);
        }
    }
}