package com.example.hongkongpetownersapp;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.hongkongpetownersapp.databinding.FragmentAddVaccineBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddVaccineFragment extends Fragment {

    private static final String TAG = "AddVaccineFragment";
    private FragmentAddVaccineBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String petId;
    private String petName;

    private Calendar vaccinationCalendar = Calendar.getInstance();
    private Calendar nextDueCalendar = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentAddVaccineBinding.inflate(inflater, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get arguments
        if (getArguments() != null) {
            petId = getArguments().getString("petId");
            petName = getArguments().getString("petName");
        }

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set title
        if (petName != null) {
            binding.textTitle.setText("Add Vaccine for " + petName);
        }

        // Setup date pickers
        setupDatePickers();

        // Setup buttons
        binding.buttonSave.setOnClickListener(v -> saveVaccineRecord());
        binding.buttonCancel.setOnClickListener(v -> navigateBack());
    }

    private void setupDatePickers() {
        // Set current date as default
        updateDateDisplay();

        // Vaccination date picker
        binding.layoutVaccinationDate.setEndIconOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        vaccinationCalendar.set(year, month, dayOfMonth);
                        updateDateDisplay();
                    },
                    vaccinationCalendar.get(Calendar.YEAR),
                    vaccinationCalendar.get(Calendar.MONTH),
                    vaccinationCalendar.get(Calendar.DAY_OF_MONTH)
            );
            // Don't allow future dates for vaccination
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            datePickerDialog.show();
        });

        // Next due date picker
        binding.layoutNextDueDate.setEndIconOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        nextDueCalendar.set(year, month, dayOfMonth);
                        updateDateDisplay();
                    },
                    nextDueCalendar.get(Calendar.YEAR),
                    nextDueCalendar.get(Calendar.MONTH),
                    nextDueCalendar.get(Calendar.DAY_OF_MONTH)
            );
            // Only allow future dates for next due
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            datePickerDialog.show();
        });

        // Make date fields clickable
        binding.editVaccinationDate.setOnClickListener(v ->
                binding.layoutVaccinationDate.getEndIconDrawable().setVisible(true, true));
        binding.editNextDueDate.setOnClickListener(v ->
                binding.layoutNextDueDate.getEndIconDrawable().setVisible(true, true));
    }

    private void updateDateDisplay() {
        binding.editVaccinationDate.setText(dateFormat.format(vaccinationCalendar.getTime()));

        // Set next due date to one year later by default
        if (nextDueCalendar.before(vaccinationCalendar)) {
            nextDueCalendar.setTime(vaccinationCalendar.getTime());
            nextDueCalendar.add(Calendar.YEAR, 1);
        }
        binding.editNextDueDate.setText(dateFormat.format(nextDueCalendar.getTime()));
    }

    private void saveVaccineRecord() {
        // Get input values
        String vaccineName = binding.editVaccineName.getText().toString().trim();
        String veterinarian = binding.editVeterinarian.getText().toString().trim();
        String clinic = binding.editClinic.getText().toString().trim();
        String notes = binding.editNotes.getText().toString().trim();

        // Validate required fields
        if (TextUtils.isEmpty(vaccineName)) {
            binding.layoutVaccineName.setError("Vaccine name is required");
            return;
        }

        // Show progress
        showLoading(true);

        // Create vaccine record
        VaccineRecord record = new VaccineRecord(
                petId,
                vaccineName,
                veterinarian,
                clinic,
                new Timestamp(vaccinationCalendar.getTime()),
                new Timestamp(nextDueCalendar.getTime()),
                notes
        );

        // Save to Firestore
        db.collection("pets").document(petId)
                .collection("vaccines")
                .add(record)
                .addOnSuccessListener(documentReference -> {
                    showLoading(false);
                    Log.d(TAG, "Vaccine record added with ID: " + documentReference.getId());
                    Toast.makeText(getContext(),
                            "Vaccine record added successfully",
                            Toast.LENGTH_SHORT).show();

                    navigateBack();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error adding vaccine record", e);
                    Toast.makeText(getContext(),
                            "Error adding vaccine record",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean show) {
        if (show) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.buttonSave.setEnabled(false);
            binding.buttonCancel.setEnabled(false);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.buttonSave.setEnabled(true);
            binding.buttonCancel.setEnabled(true);
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