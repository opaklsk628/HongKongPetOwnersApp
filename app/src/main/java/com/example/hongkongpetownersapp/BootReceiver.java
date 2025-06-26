package com.example.hongkongpetownersapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Device boot completed - rescheduling reminders");

            // Re-schedule all active reminders
            rescheduleAllReminders(context);
        }
    }

    private void rescheduleAllReminders(Context context) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Log.d(TAG, "No user logged in, skipping reminder rescheduling");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Get all active reminders
        db.collection("reminders")
                .whereEqualTo("isActive", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            HealthReminder reminder = document.toObject(HealthReminder.class);
                            reminder.setId(document.getId());

                            // Reschedule the reminder
                            LocalNotificationHelper.scheduleRepeatingNotification(context, reminder);
                            Log.d(TAG, "Rescheduled reminder: " + reminder.getTitle());
                        } catch (Exception e) {
                            Log.e(TAG, "Error rescheduling reminder", e);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load reminders for rescheduling", e);
                });
    }
}