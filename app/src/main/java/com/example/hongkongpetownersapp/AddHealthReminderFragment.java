package com.example.hongkongpetownersapp;

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

import com.example.hongkongpetownersapp.databinding.FragmentAddHealthReminderBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddHealthReminderFragment extends Fragment {

    private static final String TAG = "AddHealthReminder";
    private FragmentAddHealthReminderBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String preselectedPetId;
    private String preselectedPetName;
    private List<Pet> userPets = new ArrayList<>();
    private int selectedHour = 9; // Default 9 AM
    private int selectedMinute = 0;
    private List<String> selectedDays = new ArrayList<>();

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentAddHealthReminderBinding.inflate(inflater, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get arguments
        if (getArguments() != null) {
            preselectedPetId = getArguments().getString("petId");
            preselectedPetName = getArguments().getString("petName");
        }

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Load user's pets for spinner
        loadUserPets();

        // Setup reminder type spinner
        setupReminderTypeSpinner();

        // Setup frequency radio buttons
        setupFrequencyOptions();

        // Setup time picker
        setupTimePicker();

        // Setup weekday checkboxes
        setupWeekdayCheckboxes();

        // Setup buttons
        binding.buttonSave.setOnClickListener(v -> saveReminder());
        binding.buttonCancel.setOnClickListener(v -> navigateBack());

    }

    private void loadUserPets() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        Log.d(TAG, "Loading pets for user: " + currentUser.getUid());

        db.collection("pets")
                .whereEqualTo("ownerId", currentUser.getUid())
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    userPets.clear();
                    List<String> petNames = new ArrayList<>();
                    int selectedPosition = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Pet pet = document.toObject(Pet.class);
                        pet.setId(document.getId());
                        userPets.add(pet);
                        petNames.add(pet.getName());

                        // Check if this is the preselected pet
                        if (preselectedPetId != null && preselectedPetId.equals(pet.getId())) {
                            selectedPosition = userPets.size() - 1;
                        }
                    }

                    // Setup pet spinner
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            petNames
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    binding.spinnerPet.setAdapter(adapter);

                    if (preselectedPetId != null) {
                        binding.spinnerPet.setSelection(selectedPosition);
                        binding.spinnerPet.setEnabled(false); // Disable if preselected
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading pets", e);
                    Toast.makeText(getContext(), "Error loading pets", Toast.LENGTH_SHORT).show();
                });
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
        // Set default to daily
        binding.radioDaily.setChecked(true);
        binding.layoutWeekdays.setVisibility(View.GONE);
        binding.layoutMonthDay.setVisibility(View.GONE);
        binding.layoutCustomDays.setVisibility(View.GONE);

        // Radio button listeners
        binding.radioDaily.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                hideAllFrequencyLayouts();
            }
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
        updateTimeDisplay();

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

        binding.editTime.setOnClickListener(v ->
                binding.layoutTime.getEndIconDrawable().setVisible(true, true));
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

            if (isChecked) {
                selectedDays.add(day);
            } else {
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

    private void saveReminder() {
        // Validate inputs
        String title = binding.editTitle.getText().toString().trim();
        if (TextUtils.isEmpty(title)) {
            binding.layoutTitle.setError("Title is required");
            return;
        }

        String description = binding.editDescription.getText().toString().trim();

        // Get selected pet
        if (userPets.isEmpty()) {
            Toast.makeText(getContext(), "Please add a pet first", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedPetIndex = binding.spinnerPet.getSelectedItemPosition();
        Pet selectedPet = userPets.get(selectedPetIndex);

        // Get reminder type
        String[] typeValues = {"feeding", "medication", "exercise", "checkup", "grooming", "other"};
        String type = typeValues[binding.spinnerType.getSelectedItemPosition()];

        // Get frequency
        String frequency = getSelectedFrequency();

        // Validate frequency-specific inputs
        if (!validateFrequencyInputs(frequency)) {
            return;
        }

        // Show progress
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.buttonSave.setEnabled(false);

        // Create reminder
        HealthReminder reminder = new HealthReminder(
                selectedPet.getId(),
                selectedPet.getName(),
                type,
                title,
                description,
                frequency,
                selectedHour,
                selectedMinute
        );

        // Set frequency-specific fields
        setFrequencySpecificFields(reminder, frequency);

        // Calculate next reminder time
        Calendar nextTime = LocalNotificationHelper.calculateNextReminderTime(reminder);
        reminder.setNextReminder(new com.google.firebase.Timestamp(nextTime.getTime()));

        Log.d(TAG, "Saving reminder - PetId: " + selectedPet.getId() + ", PetName: " + selectedPet.getName());

        // Save to Firestore
        db.collection("reminders")
                .add(reminder)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "✓ Reminder saved successfully with ID: " + documentReference.getId());
                    Log.d(TAG, "Reminder details - Pet ID: " + selectedPet.getId());
                    Log.d(TAG, "Reminder details - Pet Name: " + selectedPet.getName());
                    Log.d(TAG, "Reminder details - Title: " + title);
                    Log.d(TAG, "Reminder details - Type: " + type);
                    Log.d(TAG, "Reminder details - Frequency: " + frequency);
                    Log.d(TAG, "Reminder details - Time: " + selectedHour + ":" + selectedMinute);

                    reminder.setId(documentReference.getId());

                    // Schedule local notification
                    LocalNotificationHelper.scheduleRepeatingNotification(getContext(), reminder);

                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Reminder created successfully", Toast.LENGTH_SHORT).show();
                    navigateBack();
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.buttonSave.setEnabled(true);
                    Log.e(TAG, "✗ Error creating reminder", e);
                    Toast.makeText(getContext(),
                            "Error creating reminder: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
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

    private void setFrequencySpecificFields(HealthReminder reminder, String frequency) {
        switch (frequency) {
            case "weekly":
                reminder.setDaysOfWeek(selectedDays.toArray(new String[0]));
                break;
            case "monthly":
                int day = Integer.parseInt(binding.editMonthDay.getText().toString().trim());
                reminder.setDayOfMonth(day);
                break;
            case "custom":
                int interval = Integer.parseInt(binding.editCustomDays.getText().toString().trim());
                reminder.setIntervalDays(interval);
                break;
            default:
                reminder.setIntervalDays(1); // Daily
                break;
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