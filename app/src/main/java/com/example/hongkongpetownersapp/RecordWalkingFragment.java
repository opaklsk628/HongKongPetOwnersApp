package com.example.hongkongpetownersapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.hongkongpetownersapp.databinding.FragmentRecordWalkingBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RecordWalkingFragment extends Fragment implements SensorEventListener {

    private static final String TAG = "RecordWalkingFragment";
    private static final float STEP_LENGTH_METERS = 0.7f; // Average step length

    private FragmentRecordWalkingBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String petId;
    private String petName;

    // Sensor variables
    private SensorManager sensorManager;
    private Sensor stepCounterSensor;
    private boolean isRecording = false;
    private int initialSteps = -1;
    private int currentSteps = 0;

    // Recording variables
    private long startTime;
    private WalkingRecord currentRecord;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentRecordWalkingBinding.inflate(inflater, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get arguments
        if (getArguments() != null) {
            petId = getArguments().getString("petId");
            petName = getArguments().getString("petName");
        }

        // Initialize sensor manager
        sensorManager = (SensorManager) requireActivity().getSystemService(Context.SENSOR_SERVICE);
        stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set pet name
        binding.textPetName.setText("Walking with " + petName);

        // Check if step counter is available
        if (stepCounterSensor == null) {
            Toast.makeText(getContext(),
                    "Step counter not available on this device",
                    Toast.LENGTH_LONG).show();
            binding.textSteps.setText("Step counter not available");
        }

        // Setup buttons
        binding.buttonStartStop.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            } else {
                startRecording();
            }
        });

        binding.buttonCancel.setOnClickListener(v -> {
            if (isRecording) {
                stopRecording();
            }
            navigateBack();
        });

        // Setup timer runnable
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRecording) {
                    updateTimer();
                    timerHandler.postDelayed(this, 1000); // Update every second
                }
            }
        };
    }

    private void startRecording() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        // Check permission for activity recognition (needed for step counter)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACTIVITY_RECOGNITION)
                    != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(),
                        "Activity recognition permission required",
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        isRecording = true;
        startTime = System.currentTimeMillis();

        // Create new record
        currentRecord = new WalkingRecord(petId, petName, currentUser.getUid());

        // Reset counters
        currentSteps = 0;
        initialSteps = -1;

        // Update UI
        binding.buttonStartStop.setText("Stop Walking");
        binding.buttonStartStop.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
        binding.buttonCancel.setEnabled(false);

        // Start step counter
        if (stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI);
        }

        // Start timer
        timerHandler.postDelayed(timerRunnable, 0);

        Toast.makeText(getContext(), "Recording started", Toast.LENGTH_SHORT).show();
    }

    private void stopRecording() {
        isRecording = false;

        // Stop sensors
        sensorManager.unregisterListener(this);

        // Calculate final values
        long endTime = System.currentTimeMillis();
        currentRecord.setEndTime(Timestamp.now());
        currentRecord.setDuration(endTime - startTime);
        currentRecord.setSteps(currentSteps);

        // Calculate distance (steps * average step length)
        double distanceKm = (currentSteps * STEP_LENGTH_METERS) / 1000.0;
        currentRecord.setDistance(distanceKm);

        // Calculate average speed
        currentRecord.calculateAvgSpeed();

        // Update UI
        binding.buttonStartStop.setText("Save Record");
        binding.buttonStartStop.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
        binding.buttonCancel.setEnabled(true);

        // Show summary
        showSummary();

        // Set save action
        binding.buttonStartStop.setOnClickListener(v -> saveRecord());
    }

    private void updateTimer() {
        long elapsedMillis = System.currentTimeMillis() - startTime;
        long seconds = (elapsedMillis / 1000) % 60;
        long minutes = (elapsedMillis / (1000 * 60)) % 60;
        long hours = elapsedMillis / (1000 * 60 * 60);

        String time = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        binding.textTime.setText(time);
    }

    private void showSummary() {
        binding.layoutSummary.setVisibility(View.VISIBLE);
        binding.textSummarySteps.setText(String.format("Steps: %,d", currentRecord.getSteps()));
        binding.textSummaryDistance.setText(String.format("Distance: %.2f km", currentRecord.getDistance()));
        binding.textSummaryDuration.setText("Duration: " + currentRecord.getFormattedDuration());
        binding.textSummarySpeed.setText(String.format("Avg Speed: %.1f km/h", currentRecord.getAvgSpeed()));
    }

    private void saveRecord() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.buttonStartStop.setEnabled(false);

        db.collection("walking_records")
                .add(currentRecord)
                .addOnSuccessListener(documentReference -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(),
                            "Walking record saved successfully!",
                            Toast.LENGTH_SHORT).show();
                    navigateBack();
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.buttonStartStop.setEnabled(true);
                    Log.e(TAG, "Error saving record", e);
                    Toast.makeText(getContext(),
                            "Error saving record",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateBack() {
        NavHostFragment.findNavController(this).navigateUp();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
            int totalSteps = (int) event.values[0];

            if (initialSteps < 0) {
                // First reading
                initialSteps = totalSteps;
            }

            // Calculate steps since start
            currentSteps = totalSteps - initialSteps;

            // Update UI
            binding.textSteps.setText(String.format("%,d", currentSteps));

            // Update distance
            double distanceKm = (currentSteps * STEP_LENGTH_METERS) / 1000.0;
            binding.textDistance.setText(String.format("%.2f km", distanceKm));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isRecording) {
            sensorManager.unregisterListener(this);
        }
        timerHandler.removeCallbacks(timerRunnable);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isRecording && stepCounterSensor != null) {
            sensorManager.registerListener(this, stepCounterSensor, SensorManager.SENSOR_DELAY_UI);
            timerHandler.postDelayed(timerRunnable, 0);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        sensorManager.unregisterListener(this);
        timerHandler.removeCallbacks(timerRunnable);
        binding = null;
    }
}