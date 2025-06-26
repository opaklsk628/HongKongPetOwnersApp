package com.example.hongkongpetownersapp;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);

        // Create notification channel for local notifications
        LocalNotificationHelper.createNotificationChannel(this);
    }
}