package com.example.hongkongpetownersapp;

import android.app.AlertDialog;
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

import com.example.hongkongpetownersapp.databinding.FragmentPetDetailBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PetDetailFragment extends Fragment {

    private static final String TAG = "PetDetailFragment";
    private FragmentPetDetailBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String petId;
    private Pet currentPet;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentPetDetailBinding.inflate(inflater, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get pet ID from arguments
        if (getArguments() != null) {
            petId = getArguments().getString("petId");
        }

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (petId != null) {
            loadPetData();
        }

        // Set click listeners
        binding.buttonSave.setOnClickListener(v -> savePetData());
        binding.buttonDelete.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void loadPetData() {
        db.collection("pets").document(petId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    currentPet = documentSnapshot.toObject(Pet.class);
                    if (currentPet != null) {
                        currentPet.setId(documentSnapshot.getId());
                        displayPetData();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading pet", e);
                    Toast.makeText(getContext(),
                            "Error loading pet data",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void displayPetData() {
        // Display pet name
        binding.editPetName.setText(currentPet.getName());

        // Display pet type and icon
        String petType = currentPet.getType();
        if (petType != null) {
            switch (petType.toLowerCase()) {
                case "dog":
                    binding.radioDog.setChecked(true);
                    binding.textPetIcon.setText("🐕 Dog");
                    break;
                case "cat":
                    binding.radioCat.setChecked(true);
                    binding.textPetIcon.setText("🐈 Cat");
                    break;
                default:
                    binding.radioOther.setChecked(true);
                    binding.textPetIcon.setText("🐾 Pet");
                    break;
            }
        }

        // Display age if available
        if (currentPet.getAge() > 0) {
            binding.editPetAge.setText(String.valueOf(currentPet.getAge()));
        }

        // Display breed if available
        if (currentPet.getBreed() != null) {
            binding.editPetBreed.setText(currentPet.getBreed());
        }

        // Display gender if available
        if (currentPet.getGender() != null) {
            if (currentPet.getGender().equals("Male")) {
                binding.radioMale.setChecked(true);
            } else if (currentPet.getGender().equals("Female")) {
                binding.radioFemale.setChecked(true);
            }
        }
    }

    private void savePetData() {
        // Get input values
        String petName = binding.editPetName.getText().toString().trim();
        String ageText = binding.editPetAge.getText().toString().trim();
        String breed = binding.editPetBreed.getText().toString().trim();

        // Validate name
        if (TextUtils.isEmpty(petName)) {
            binding.layoutPetName.setError(getString(R.string.pet_name_required));
            return;
        }

        // Get selected pet type
        String petType = getSelectedPetType();

        // Get selected gender
        String gender = getSelectedGender();

        // Prepare update data
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", petName);
        updates.put("type", petType);

        // Add age if provided
        if (!TextUtils.isEmpty(ageText)) {
            try {
                int age = Integer.parseInt(ageText);
                updates.put("age", age);
            } catch (NumberFormatException e) {
                binding.layoutPetAge.setError("Please enter a valid age");
                return;
            }
        }

        // Add breed if provided
        if (!TextUtils.isEmpty(breed)) {
            updates.put("breed", breed);
        }

        // Add gender if selected
        if (!TextUtils.isEmpty(gender)) {
            updates.put("gender", gender);
        }

        // Update in Firestore..
        db.collection("pets").document(petId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(),
                            getString(R.string.pet_updated_success),
                            Toast.LENGTH_SHORT).show();
                    // Navigate back
                    NavHostFragment.findNavController(this).navigateUp();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating pet", e);
                    Toast.makeText(getContext(),
                            "Error updating pet data",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private String getSelectedPetType() {
        int selectedId = binding.radioGroupPetType.getCheckedRadioButtonId();

        if (selectedId == R.id.radio_dog) return "Dog";
        else if (selectedId == R.id.radio_cat) return "Cat";
        else return "Other";
    }

    private String getSelectedGender() {
        int selectedId = binding.radioGroupGender.getCheckedRadioButtonId();

        if (selectedId == R.id.radio_male) return "Male";
        else if (selectedId == R.id.radio_female) return "Female";
        else return "";
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete_pet))
                .setMessage(getString(R.string.delete_pet_confirmation))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> deletePet())
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void deletePet() {
        db.collection("pets").document(petId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(),
                            getString(R.string.pet_deleted_success),
                            Toast.LENGTH_SHORT).show();
                    // Navigate back to pet list
                    NavHostFragment.findNavController(this).navigateUp();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting pet", e);
                    Toast.makeText(getContext(),
                            "Error deleting pet",
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}