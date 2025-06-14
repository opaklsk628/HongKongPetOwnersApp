package com.example.hongkongpetownersapp;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.hongkongpetownersapp.databinding.FragmentAddPetBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class AddPetFragment extends Fragment {

    private static final String TAG = "AddPetFragment";
    private FragmentAddPetBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentAddPetBinding.inflate(inflater, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Check authentication
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.FirstFragment);
            return;
        }

        // Set default selection
        binding.radioDog.setChecked(true);

        // Setup click listeners
        binding.buttonSave.setOnClickListener(v -> savePet());
        binding.buttonCancel.setOnClickListener(v -> navigateBack());
    }

    private void savePet() {
        // Get pet name
        String petName = binding.editPetName.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(petName)) {
            binding.layoutPetName.setError(getString(R.string.pet_name_required));
            return;
        }

        // Get selected pet type
        String petType = getSelectedPetType();

        // Get current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        // Show progress
        showLoading(true);

        // Create new pet
        Pet newPet = new Pet(petName, petType, currentUser.getUid());

        // Save to Firestore
        db.collection("pets")
                .add(newPet)
                .addOnSuccessListener(documentReference -> {
                    showLoading(false);
                    Log.d(TAG, "Pet added with ID: " + documentReference.getId());
                    Toast.makeText(getContext(),
                            getString(R.string.pet_added_success),
                            Toast.LENGTH_SHORT).show();
                    navigateBack();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Log.e(TAG, "Error adding pet", e);
                    Toast.makeText(getContext(),
                            getString(R.string.error_adding_pet),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private String getSelectedPetType() {
        int selectedId = binding.radioGroupPetType.getCheckedRadioButtonId();

        if (selectedId == R.id.radio_dog) return "Dog";
        else if (selectedId == R.id.radio_cat) return "Cat";
        else if (selectedId == R.id.radio_bird) return "Bird";
        else if (selectedId == R.id.radio_fish) return "Fish";
        else if (selectedId == R.id.radio_rabbit) return "Rabbit";
        else return "Other";
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