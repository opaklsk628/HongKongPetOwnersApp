package com.example.hongkongpetownersapp;

import android.app.AlertDialog;
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

import com.example.hongkongpetownersapp.databinding.FragmentVaccineDetailBinding;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class VaccineDetailFragment extends Fragment {

    private static final String TAG = "VaccineDetailFragment";
    private FragmentVaccineDetailBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String petId;
    private String petName;
    private String recordId;
    private VaccineRecord currentRecord;

    private Calendar vaccinationCalendar = Calendar.getInstance();
    private Calendar nextDueCalendar = Calendar.getInstance();
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentVaccineDetailBinding.inflate(inflater, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get arguments
        if (getArguments() != null) {
            petId = getArguments().getString("petId");
            petName = getArguments().getString("petName");
            recordId = getArguments().getString("recordId");
        }

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set title
        if (petName != null) {
            binding.textTitle.setText(petName + "'s Vaccine Record");
        }

        // Load vaccine record
        if (recordId != null) {
            loadVaccineRecord();
        }

        // Setup date pickers
        setupDatePickers();

        // Setup buttons
        binding.buttonSave.setOnClickListener(v -> updateVaccineRecord());
        binding.buttonDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void loadVaccineRecord() {
        binding.progressBar.setVisibility(View.VISIBLE);

        db.collection("pets").document(petId)
                .collection("vaccines").document(recordId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    currentRecord = documentSnapshot.toObject(VaccineRecord.class);
                    if (currentRecord != null) {
                        displayRecordData();
                    }
                    binding.progressBar.setVisibility(View.GONE);
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading vaccine record", e);
                    Toast.makeText(getContext(),
                            "Error loading record",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void displayRecordData() {
        binding.editVaccineName.setText(currentRecord.getVaccineName());

        if (currentRecord.getVeterinarian() != null) {
            binding.editVeterinarian.setText(currentRecord.getVeterinarian());
        }

        if (currentRecord.getClinic() != null) {
            binding.editClinic.setText(currentRecord.getClinic());
        }

        if (currentRecord.getNotes() != null) {
            binding.editNotes.setText(currentRecord.getNotes());
        }

        if (currentRecord.getVaccinationDate() != null) {
            vaccinationCalendar.setTime(currentRecord.getVaccinationDate().toDate());
            binding.editVaccinationDate.setText(dateFormat.format(vaccinationCalendar.getTime()));
        }

        if (currentRecord.getNextDueDate() != null) {
            nextDueCalendar.setTime(currentRecord.getNextDueDate().toDate());
            binding.editNextDueDate.setText(dateFormat.format(nextDueCalendar.getTime()));
        }
    }

    private void setupDatePickers() {
        // Vaccination date picker
        binding.layoutVaccinationDate.setEndIconOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        vaccinationCalendar.set(year, month, dayOfMonth);
                        binding.editVaccinationDate.setText(dateFormat.format(vaccinationCalendar.getTime()));
                    },
                    vaccinationCalendar.get(Calendar.YEAR),
                    vaccinationCalendar.get(Calendar.MONTH),
                    vaccinationCalendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
            datePickerDialog.show();
        });

        // Next due date picker
        binding.layoutNextDueDate.setEndIconOnClickListener(v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        nextDueCalendar.set(year, month, dayOfMonth);
                        binding.editNextDueDate.setText(dateFormat.format(nextDueCalendar.getTime()));
                    },
                    nextDueCalendar.get(Calendar.YEAR),
                    nextDueCalendar.get(Calendar.MONTH),
                    nextDueCalendar.get(Calendar.DAY_OF_MONTH)
            );
            datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
            datePickerDialog.show();
        });
    }

    private void updateVaccineRecord() {
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

        // Prepare update data
        Map<String, Object> updates = new HashMap<>();
        updates.put("vaccineName", vaccineName);
        updates.put("veterinarian", veterinarian);
        updates.put("clinic", clinic);
        updates.put("notes", notes);
        updates.put("vaccinationDate", new Timestamp(vaccinationCalendar.getTime()));
        updates.put("nextDueDate", new Timestamp(nextDueCalendar.getTime()));

        // Update in Firestore
        db.collection("pets").document(petId)
                .collection("vaccines").document(recordId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(getContext(),
                            "Vaccine record updated successfully",
                            Toast.LENGTH_SHORT).show();
                    navigateBack();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error updating vaccine record", e);
                    Toast.makeText(getContext(),
                            "Error updating record",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Vaccine Record")
                .setMessage("Are you sure you want to delete this vaccine record?")
                .setPositiveButton("Delete", (dialog, which) -> deleteRecord())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRecord() {
        showLoading(true);

        db.collection("pets").document(petId)
                .collection("vaccines").document(recordId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(getContext(),
                            "Vaccine record deleted",
                            Toast.LENGTH_SHORT).show();
                    navigateBack();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error deleting vaccine record", e);
                    Toast.makeText(getContext(),
                            "Error deleting record",
                            Toast.LENGTH_SHORT).show();
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