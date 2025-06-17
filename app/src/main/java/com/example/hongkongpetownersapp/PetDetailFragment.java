package com.example.hongkongpetownersapp;

import android.app.AlertDialog;
import android.net.Uri;
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

import com.bumptech.glide.Glide;
import com.example.hongkongpetownersapp.databinding.FragmentPetDetailBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class PetDetailFragment extends Fragment {

    private static final String TAG = "PetDetailFragment";
    private FragmentPetDetailBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;
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
        storage = FirebaseStorage.getInstance();

        // Get pet ID from arguments
        if (getArguments() != null) {
            petId = getArguments().getString("petId");
        }

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Load pet data
        if (petId != null) {
            loadPetData();
        }

        // Set click listeners
        binding.buttonSave.setOnClickListener(v -> savePetData());
        binding.buttonDelete.setOnClickListener(v -> showDeleteConfirmation());

        // Take photo button click
        binding.buttonTakePhoto.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("petId", petId);
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_petDetailFragment_to_cameraFragment, bundle);
        });

        // View album button click
        binding.buttonViewAlbum.setOnClickListener(v -> {
            if (currentPet != null) {
                Bundle bundle = new Bundle();
                bundle.putString("petId", petId);
                bundle.putString("petName", currentPet.getName());
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_petDetailFragment_to_petAlbumFragment, bundle);
            }
        });

        // Listen for photo result
        getParentFragmentManager().setFragmentResultListener("photoResult",
                getViewLifecycleOwner(), (requestKey, result) -> {
                    String photoPath = result.getString("photoPath");
                    if (photoPath != null) {
                        uploadPhotoToFirebase(photoPath);
                    }
                });
    }

    // Upload photo to Firebase Storage
    private void uploadPhotoToFirebase(String photoPath) {
        // Show upload progress
        binding.uploadProgress.setVisibility(View.VISIBLE);
        binding.buttonTakePhoto.setEnabled(false);

        File photoFile = new File(photoPath);
        Uri photoUri = Uri.fromFile(photoFile);

        // Create storage path
        String fileName = "pets/" + petId + "/" + System.currentTimeMillis() + ".jpg";
        StorageReference photoRef = storage.getReference().child(fileName);

        Log.d(TAG, "Uploading photo to: " + fileName);

        // Upload file
        photoRef.putFile(photoUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "Upload successful, getting download URL...");
                    // Get download URL
                    photoRef.getDownloadUrl()
                            .addOnSuccessListener(downloadUri -> {
                                Log.d(TAG, "Got download URL: " + downloadUri);
                                updatePetPhotoUrl(downloadUri.toString());

                                // Delete local file
                                photoFile.delete();
                            })
                            .addOnFailureListener(e -> {
                                binding.uploadProgress.setVisibility(View.GONE);
                                binding.buttonTakePhoto.setEnabled(true);
                                Log.e(TAG, "Failed to get download URL", e);
                                Toast.makeText(getContext(),
                                        "Failed to get photo URL: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    binding.uploadProgress.setVisibility(View.GONE);
                    binding.buttonTakePhoto.setEnabled(true);
                    Log.e(TAG, "Photo upload failed", e);
                    Toast.makeText(getContext(),
                            "Failed to upload photo: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                })
                .addOnProgressListener(snapshot -> {
                    // Calculate upload progress
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    Log.d(TAG, "Upload progress: " + Math.round(progress) + "%");
                });
    }

    // Update pet photo URL in Firestore
    private void updatePetPhotoUrl(String photoUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("photoUrl", photoUrl);

        Log.d(TAG, "Updating pet photo URL in Firestore...");

        db.collection("pets").document(petId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Update local data
                    if (currentPet != null) {
                        currentPet.setPhotoUrl(photoUrl);
                        displayPetPhoto();
                    }

                    // Create photo record in photos subcollection
                    Photo photo = new Photo(photoUrl, petId);
                    db.collection("pets").document(petId)
                            .collection("photos")
                            .add(photo)
                            .addOnSuccessListener(documentReference -> {
                                binding.uploadProgress.setVisibility(View.GONE);
                                binding.buttonTakePhoto.setEnabled(true);

                                Log.d(TAG, "Photo record created successfully");
                                Toast.makeText(getContext(),
                                        "Photo uploaded successfully!",
                                        Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                binding.uploadProgress.setVisibility(View.GONE);
                                binding.buttonTakePhoto.setEnabled(true);

                                Log.e(TAG, "Failed to create photo record", e);
                                // Photo uploaded but record creation failed
                                Toast.makeText(getContext(),
                                        "Photo uploaded but failed to save record",
                                        Toast.LENGTH_LONG).show();
                            });
                })
                .addOnFailureListener(e -> {
                    binding.uploadProgress.setVisibility(View.GONE);
                    binding.buttonTakePhoto.setEnabled(true);

                    Log.e(TAG, "Error updating photo URL", e);
                    Toast.makeText(getContext(),
                            "Failed to save photo info: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void loadPetData() {
        db.collection("pets").document(petId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    currentPet = documentSnapshot.toObject(Pet.class);
                    if (currentPet != null) {
                        currentPet.setId(documentSnapshot.getId());
                        displayPetData();
                        displayPetPhoto();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading pet", e);
                    Toast.makeText(getContext(),
                            "Error loading pet data",
                            Toast.LENGTH_SHORT).show();
                });
    }

    // Display pet photo
    private void displayPetPhoto() {
        if (currentPet != null && currentPet.getPhotoUrl() != null && !currentPet.getPhotoUrl().isEmpty()) {
            // Hide emoji icon
            binding.textPetIcon.setVisibility(View.GONE);
            // Show photo
            binding.imagePetPhoto.setVisibility(View.VISIBLE);

            // Use Glide to load photo
            Glide.with(this)
                    .load(currentPet.getPhotoUrl())
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .centerCrop()
                    .into(binding.imagePetPhoto);

            Log.d(TAG, "Displaying photo: " + currentPet.getPhotoUrl());
        } else {
            // Show emoji icon
            binding.textPetIcon.setVisibility(View.VISIBLE);
            binding.imagePetPhoto.setVisibility(View.GONE);
        }
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
                    binding.textPetIcon.setText("🐕");
                    break;
                case "cat":
                    binding.radioCat.setChecked(true);
                    binding.textPetIcon.setText("🐈");
                    break;
                default:
                    binding.radioOther.setChecked(true);
                    binding.textPetIcon.setText("🐾");
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

        // Update in Firestore
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