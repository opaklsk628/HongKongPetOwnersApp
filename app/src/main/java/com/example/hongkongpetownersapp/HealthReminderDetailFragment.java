package com.example.hongkongpetownersapp;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.hongkongpetownersapp.databinding.FragmentHealthReminderDetailBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HealthReminderDetailFragment extends Fragment {

    private static final String TAG = "ReminderDetail";
    private FragmentHealthReminderDetailBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String reminderId;
    private String petId;
    private String petName;
    private HealthReminder currentReminder;
    private int selectedHour;
    private int selectedMinute;
    private List<String> selectedDays = new ArrayList<>();

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentHealthReminderDetailBinding.inflate(inflater, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get arguments
        if (getArguments() != null) {
            reminderId = getArguments().getString("reminderId");
            petId = getArguments().getString("petId");
            petName = getArguments().getString("petName");
        }

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set title
        binding.textTitle.setText("Edit Reminder");

        // Load reminder details
        if (reminderId != null) {
            loadReminderDetails();
        }

        // Setup UI components
        setupReminderTypeSpinner();
        setupFrequencyOptions();
        setupTimePicker();
        setupWeekdayCheckboxes();

        // Setup buttons
        binding.buttonSave.setOnClickListener(v -> updateReminder());
        binding.buttonDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void loadReminderDetails() {
        binding.progressBar.setVisibility(View.VISIBLE);

        db.collection("reminders").document(reminderId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    currentReminder = documentSnapshot.toObject(HealthReminder.class);
                    if (currentReminder != null) {
                        displayReminderData();
                    }
                    binding.progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading reminder", e);
                    Toast.makeText(getContext(), "Error loading reminder", Toast.LENGTH_SHORT).show();
                });
    }

    private void displayReminderData() {
        // Display pet name
        binding.textPetName.setText("For: " + currentReminder.getPetName());

        // Display title
        binding.editTitle.setText(currentReminder.getTitle());

        // Display description
        if (currentReminder.getDescription() != null) {
            binding.editDescription.setText(currentReminder.getDescription());
        }

        // Display type
        String[] typeValues = {"feeding", "medication", "exercise", "checkup", "grooming", "other"};
        for (int i = 0; i < typeValues.length; i++) {
            if (typeValues[i].equals(currentReminder.getType())) {
                binding.spinnerType.setSelection(i);
                break;
            }
        }

        // Display time
        selectedHour = currentReminder.getHour();
        selectedMinute = currentReminder.getMinute();
        updateTimeDisplay();

        // Display frequency
        switch (currentReminder.getFrequency()) {
            case "daily":
                binding.radioDaily.setChecked(true);
                break;
            case "weekly":
                binding.radioWeekly.setChecked(true);
                binding.layoutWeekdays.setVisibility(View.VISIBLE);
                // Check the appropriate weekdays
                if (currentReminder.getDaysOfWeek() != null) {
                    selectedDays.addAll(Arrays.asList(currentReminder.getDaysOfWeek()));
                    for (String day : selectedDays) {
                        checkWeekdayBox(day);
                    }
                }
                break;
            case "monthly":
                binding.radioMonthly.setChecked(true);
                binding.layoutMonthDay.setVisibility(View.VISIBLE);
                binding.editMonthDay.setText(String.valueOf(currentReminder.getDayOfMonth()));
                break;
            case "custom":
                binding.radioCustom.setChecked(true);
                binding.layoutCustomDays.setVisibility(View.VISIBLE);
                binding.editCustomDays.setText(String.valueOf(currentReminder.getIntervalDays()));
                break;
        }

        // Display active status
        binding.switchActive.setChecked(currentReminder.isActive());
    }

    private void checkWeekdayBox(String day) {
        switch (day) {
            case "Mon": binding.checkboxMon.setChecked(true); break;
            case "Tue": binding.checkboxTue.setChecked(true); break;
            case "Wed": binding.checkboxWed.setChecked(true); break;
            case "Thu": binding.checkboxThu.setChecked(true); break;
            case "Fri": binding.checkboxFri.setChecked(true); break;
            case "Sat": binding.checkboxSat.setChecked(true); break;
            case "Sun": binding.checkboxSun.setChecked(true); break;
        }
    }

    private void setupReminderTypeSpinner() {
        String[] types = {"Feeding", "Medication", "Exercise", "Checkup", "Grooming", "Other"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                types
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerType.setAdapter(adapter);
    }

    private void setupFrequencyOptions() {
        binding.radioDaily.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) hideAllFrequencyLayouts();
        });

        binding.radioWeekly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                hideAllFrequencyLayouts();
                binding.layoutWeekdays.setVisibility(View.VISIBLE);
            }
        });

        binding.radioMonthly.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                hideAllFrequencyLayouts();
                binding.layoutMonthDay.setVisibility(View.VISIBLE);
            }
        });

        binding.radioCustom.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                hideAllFrequencyLayouts();
                binding.layoutCustomDays.setVisibility(View.VISIBLE);
            }
        });
    }

    private void hideAllFrequencyLayouts() {
        binding.layoutWeekdays.setVisibility(View.GONE);
        binding.layoutMonthDay.setVisibility(View.GONE);
        binding.layoutCustomDays.setVisibility(View.GONE);
    }

    private void setupTimePicker() {
        binding.layoutTime.setEndIconOnClickListener(v -> {
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    requireContext(),
                    (view, hourOfDay, minute) -> {
                        selectedHour = hourOfDay;
                        selectedMinute = minute;
                        updateTimeDisplay();
                    },
                    selectedHour,
                    selectedMinute,
                    true
            );
            timePickerDialog.show();
        });
    }

    private void updateTimeDisplay() {
        String time = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
        binding.editTime.setText(time);
    }

    private void setupWeekdayCheckboxes() {
        CompoundButton.OnCheckedChangeListener listener = (buttonView, isChecked) -> {
            String day = "";
            int id = buttonView.getId();

            if (id == R.id.checkbox_mon) day = "Mon";
            else if (id == R.id.checkbox_tue) day = "Tue";
            else if (id == R.id.checkbox_wed) day = "Wed";
            else if (id == R.id.checkbox_thu) day = "Thu";
            else if (id == R.id.checkbox_fri) day = "Fri";
            else if (id == R.id.checkbox_sat) day = "Sat";
            else if (id == R.id.checkbox_sun) day = "Sun";

            if (isChecked && !selectedDays.contains(day)) {
                selectedDays.add(day);
            } else if (!isChecked) {
                selectedDays.remove(day);
            }
        };

        binding.checkboxMon.setOnCheckedChangeListener(listener);
        binding.checkboxTue.setOnCheckedChangeListener(listener);
        binding.checkboxWed.setOnCheckedChangeListener(listener);
        binding.checkboxThu.setOnCheckedChangeListener(listener);
        binding.checkboxFri.setOnCheckedChangeListener(listener);
        binding.checkboxSat.setOnCheckedChangeListener(listener);
        binding.checkboxSun.setOnCheckedChangeListener(listener);
    }

    private void updateReminder() {
        // Validate inputs
        String title = binding.editTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            binding.layoutTitle.setError("Title is required");
            return;
        }

        // Get values
        String description = binding.editDescription.getText().toString().trim();
        String[] typeValues = {"feeding", "medication", "exercise", "checkup", "grooming", "other"};
        String type = typeValues[binding.spinnerType.getSelectedItemPosition()];
        String frequency = getSelectedFrequency();
        boolean isActive = binding.switchActive.isChecked();

        // Validate frequency-specific inputs
        if (!validateFrequencyInputs(frequency)) {
            return;
        }

        // Show progress
        showLoading(true);

        // Prepare updates
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("description", description);
        updates.put("type", type);
        updates.put("frequency", frequency);
        updates.put("hour", selectedHour);
        updates.put("minute", selectedMinute);
        updates.put("isActive", isActive);

        // Add frequency-specific fields
        addFrequencySpecificUpdates(updates, frequency);

        // Calculate next reminder time
        currentReminder.setFrequency(frequency);
        currentReminder.setHour(selectedHour);
        currentReminder.setMinute(selectedMinute);
        setFrequencySpecificFields(currentReminder, frequency);

        Calendar nextTime = NotificationHelper.calculateNextReminderTime(currentReminder);
        updates.put("nextReminder", new com.google.firebase.Timestamp(nextTime.getTime()));

        // Update in Firestore
        db.collection("reminders").document(reminderId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);

                    // Update notification
                    currentReminder.setTitle(title);
                    currentReminder.setActive(isActive);

                    if (isActive) {
                        LocalNotificationHelper.scheduleRepeatingNotification(getContext(), currentReminder);
                    } else {
                        LocalNotificationHelper.cancelNotification(getContext(), reminderId);
                    }

                    Toast.makeText(getContext(), "Reminder updated successfully", Toast.LENGTH_SHORT).show();
                    navigateBack();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error updating reminder", e);
                    Toast.makeText(getContext(), "Error updating reminder", Toast.LENGTH_SHORT).show();
                });
    }

    private String getSelectedFrequency() {
        if (binding.radioDaily.isChecked()) return "daily";
        if (binding.radioWeekly.isChecked()) return "weekly";
        if (binding.radioMonthly.isChecked()) return "monthly";
        if (binding.radioCustom.isChecked()) return "custom";
        return "daily";
    }

    private boolean validateFrequencyInputs(String frequency) {
        switch (frequency) {
            case "weekly":
                if (selectedDays.isEmpty()) {
                    Toast.makeText(getContext(), "Please select at least one day", Toast.LENGTH_SHORT).show();
                    return false;
                }
                break;
            case "monthly":
                String dayText = binding.editMonthDay.getText().toString().trim();
                if (TextUtils.isEmpty(dayText)) {
                    binding.layoutMonthDay.setError("Day is required");
                    return false;
                }
                try {
                    int day = Integer.parseInt(dayText);
                    if (day < 1 || day > 31) {
                        binding.layoutMonthDay.setError("Day must be between 1 and 31");
                        return false;
                    }
                } catch (NumberFormatException e) {
                    binding.layoutMonthDay.setError("Invalid day");
                    return false;
                }
                break;
            case "custom":
                String intervalText = binding.editCustomDays.getText().toString().trim();
                if (TextUtils.isEmpty(intervalText)) {
                    binding.layoutCustomDays.setError("Interval is required");
                    return false;
                }
                try {
                    int interval = Integer.parseInt(intervalText);
                    if (interval < 1) {
                        binding.layoutCustomDays.setError("Interval must be at least 1");
                        return false;
                    }
                } catch (NumberFormatException e) {
                    binding.layoutCustomDays.setError("Invalid interval");
                    return false;
                }
                break;
        }
        return true;
    }

    private void addFrequencySpecificUpdates(Map<String, Object> updates, String frequency) {
        switch (frequency) {
            case "weekly":
                updates.put("daysOfWeek", selectedDays.toArray(new String[0]));
                updates.put("dayOfMonth", null);
                updates.put("intervalDays", 1);
                break;
            case "monthly":
                int day = Integer.parseInt(binding.editMonthDay.getText().toString().trim());
                updates.put("dayOfMonth", day);
                updates.put("daysOfWeek", null);
                updates.put("intervalDays", 1);
                break;
            case "custom":
                int interval = Integer.parseInt(binding.editCustomDays.getText().toString().trim());
                updates.put("intervalDays", interval);
                updates.put("daysOfWeek", null);
                updates.put("dayOfMonth", null);
                break;
            default: // daily
                updates.put("intervalDays", 1);
                updates.put("daysOfWeek", null);
                updates.put("dayOfMonth", null);
                break;
        }
    }

    private void setFrequencySpecificFields(HealthReminder reminder, String frequency) {
        switch (frequency) {
            case "weekly":
                reminder.setDaysOfWeek(selectedDays.toArray(new String[0]));
                reminder.setDayOfMonth(0);
                reminder.setIntervalDays(1);
                break;
            case "monthly":
                int day = Integer.parseInt(binding.editMonthDay.getText().toString().trim());
                reminder.setDayOfMonth(day);
                reminder.setDaysOfWeek(null);
                reminder.setIntervalDays(1);
                break;
            case "custom":
                int interval = Integer.parseInt(binding.editCustomDays.getText().toString().trim());
                reminder.setIntervalDays(interval);
                reminder.setDaysOfWeek(null);
                reminder.setDayOfMonth(0);
                break;
            default: // daily
                reminder.setIntervalDays(1);
                reminder.setDaysOfWeek(null);
                reminder.setDayOfMonth(0);
                break;
        }
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Reminder")
                .setMessage("Are you sure you want to delete this reminder?")
                .setPositiveButton("Delete", (dialog, which) -> deleteReminder())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteReminder() {
        showLoading(true);

        db.collection("reminders").document(reminderId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Cancel notification
                    LocalNotificationHelper.cancelNotification(getContext(), reminderId);

                    showLoading(false);
                    Toast.makeText(getContext(), "Reminder deleted", Toast.LENGTH_SHORT).show();
                    navigateBack();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error deleting reminder", e);
                    Toast.makeText(getContext(), "Error deleting reminder", Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean show) {
        if (show) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.buttonSave.setEnabled(false);
            binding.buttonDelete.setEnabled(false);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.buttonSave.setEnabled(true);
            binding.buttonDelete.setEnabled(true);
        }
    }

    private void navigateBack() {
        NavHostFragment.findNavController(this).navigateUp();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}