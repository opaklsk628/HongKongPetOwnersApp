package com.example.hongkongpetownersapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.hongkongpetownersapp.databinding.FragmentSecondBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SecondFragment extends Fragment {

    private FragmentSecondBinding binding;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentSecondBinding.inflate(inflater, container, false);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Check if user is signed in
        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // If not signed in, navigate back to login
            navigateToLogin();
            return;
        }

        // Display user info
        displayUserInfo();

        // Set verify email button click listener
        binding.buttonVerifyEmail.setOnClickListener(v -> sendVerificationEmail());

        // Set add my pet button click listener
        binding.buttonAddMyPet.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_SecondFragment_to_addPetFragment);
        });

        // Set my pets button click listener - Navigate with details mode
        binding.buttonMyPets.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("mode", "details"); // Mark as details mode
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_SecondFragment_to_petListFragment, bundle);
        });

        // Set feature button click listeners
        setupFeatureButtons();
    }

    private void displayUserInfo() {
        if (currentUser != null) {
            // Display email
            binding.textUserEmail.setText(currentUser.getEmail());

            // Check email verification status
            if (!currentUser.isEmailVerified()) {
                binding.textVerificationStatus.setVisibility(View.VISIBLE);
                binding.buttonVerifyEmail.setVisibility(View.VISIBLE);
            } else {
                binding.textVerificationStatus.setVisibility(View.GONE);
                binding.buttonVerifyEmail.setVisibility(View.GONE);
            }
        }
    }

    private void setupFeatureButtons() {
        // Pet Parks - Navigate to pet parks map
        binding.buttonPetParks.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_SecondFragment_to_petParksFragment);
        });

        // Pet Album - Navigate to pet list with album mode
        binding.buttonPetAlbum.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("mode", "album"); // Mark as album mode
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_SecondFragment_to_petListFragment, bundle);
        });

        // Vaccine Records - Navigate to pet list with vaccine mode
        binding.buttonVaccineRecords.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            bundle.putString("mode", "vaccine"); // Mark as vaccine mode
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_SecondFragment_to_petListFragment, bundle);
        });

        // Walking Routes - Navigate to walking routes
        binding.buttonWalkingRoutes.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_SecondFragment_to_walkingRoutesFragment);
        });

        // Health Reminders - Navigate to health reminders list
        binding.buttonHealthReminders.setOnClickListener(v -> {
            // Navigate without pet selection to show all reminders
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_SecondFragment_to_healthRemindersFragment);
        });

        // Pet AI Assistant - Navigate to AI chat
        binding.buttonPetAi.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_SecondFragment_to_petAIChatFragment);
        });
    }

    private void sendVerificationEmail() {
        if (currentUser != null) {
            currentUser.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(),
                                    getString(R.string.verification_email_sent) + " " + currentUser.getEmail(),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getContext(),
                                    getString(R.string.verification_email_failed),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    // Navigate to login screen
    private void navigateToLogin() {
        NavHostFragment.findNavController(SecondFragment.this)
                .navigate(R.id.action_SecondFragment_to_FirstFragment);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}