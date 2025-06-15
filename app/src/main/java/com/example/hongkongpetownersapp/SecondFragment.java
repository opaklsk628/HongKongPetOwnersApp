package com.example.hongkongpetownersapp;

import android.app.AlertDialog;
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

        // Set logout button click listener
        binding.buttonLogout.setOnClickListener(v -> showLogoutConfirmation());

        // Set account settings button click listener
        binding.buttonAccountSettings.setOnClickListener(v -> showAccountSettings());

        // Set verify email button click listener
        binding.buttonVerifyEmail.setOnClickListener(v -> sendVerificationEmail());

        // Set add my pet button click listener
        binding.buttonAddMyPet.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_SecondFragment_to_addPetFragment);
        });

        // Set my pets button click listener
        binding.buttonMyPets.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_SecondFragment_to_petListFragment);
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
        binding.buttonPetParks.setOnClickListener(v -> {
            Toast.makeText(getContext(), getString(R.string.feature_coming_soon), Toast.LENGTH_SHORT).show();
        });

        binding.buttonPetAlbum.setOnClickListener(v -> {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_SecondFragment_to_petListFragment);
        });

        binding.buttonVaccineRecords.setOnClickListener(v -> {
            Toast.makeText(getContext(), getString(R.string.feature_coming_soon), Toast.LENGTH_SHORT).show();
        });

        binding.buttonHealthReminders.setOnClickListener(v -> {
            Toast.makeText(getContext(), getString(R.string.feature_coming_soon), Toast.LENGTH_SHORT).show();
        });
    }

    private void showAccountSettings() {
        String[] options = {
                getString(R.string.change_password),
                getString(R.string.change_email),
                getString(R.string.delete_account)
        };

        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.account_settings))
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            changePassword();
                            break;
                        case 1:
                            changeEmail();
                            break;
                        case 2:
                            deleteAccount();
                            break;
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
    }

    private void changePassword() {
        if (currentUser != null && currentUser.getEmail() != null) {
            mAuth.sendPasswordResetEmail(currentUser.getEmail())
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(getContext(),
                                    getString(R.string.password_reset_email_sent),
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getContext(),
                                    getString(R.string.password_reset_failed),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void changeEmail() {
        Toast.makeText(getContext(), getString(R.string.feature_coming_soon), Toast.LENGTH_SHORT).show();
        // Implementation for email change would require re-authentication
    }

    private void deleteAccount() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete_account))
                .setMessage(getString(R.string.delete_account_confirmation))
                .setPositiveButton(getString(R.string.delete), (dialog, which) -> {
                    if (currentUser != null) {
                        currentUser.delete()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(getContext(),
                                                getString(R.string.account_deleted),
                                                Toast.LENGTH_SHORT).show();
                                        navigateToLogin();
                                    } else {
                                        Toast.makeText(getContext(),
                                                getString(R.string.delete_account_failed),
                                                Toast.LENGTH_LONG).show();
                                    }
                                });
                    }
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
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

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.logout_button))
                .setMessage(getString(R.string.logout_confirmation))
                .setPositiveButton(getString(R.string.logout_button), (dialog, which) -> {
                    mAuth.signOut();
                    Toast.makeText(getContext(), getString(R.string.logout_success), Toast.LENGTH_SHORT).show();
                    navigateToLogin();
                })
                .setNegativeButton(getString(R.string.cancel), null)
                .show();
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