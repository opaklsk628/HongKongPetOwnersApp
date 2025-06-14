package com.example.hongkongpetownersapp;

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

import com.example.hongkongpetownersapp.databinding.FragmentFirstBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FirstFragment extends Fragment {

    private static final String TAG = "FirstFragment";
    private FragmentFirstBinding binding;
    private FirebaseAuth mAuth;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // If already signed in, navigate to main screen
            navigateToMainScreen();
        }

        // Set login button click listener
        binding.buttonLogin.setOnClickListener(v -> {
            String email = binding.editEmail.getText().toString().trim();
            String password = binding.editPassword.getText().toString().trim();

            if (validateInput(email, password)) {
                loginUser(email, password);
            }
        });

        // Set register button click listener
        binding.buttonRegister.setOnClickListener(v -> {
            String email = binding.editEmail.getText().toString().trim();
            String password = binding.editPassword.getText().toString().trim();

            if (validateInput(email, password)) {
                registerUser(email, password);
            }
        });

        // Set forgot password click listener
        binding.textForgotPassword.setOnClickListener(v -> {
            String email = binding.editEmail.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                binding.layoutEmail.setError(getString(R.string.email_required_for_reset));
                return;
            }
            resetPassword(email);
        });
    }

    // Validate input fields
    private boolean validateInput(String email, String password) {
        boolean isValid = true;

        // Clear previous errors
        binding.layoutEmail.setError(null);
        binding.layoutPassword.setError(null);

        // Check if email is empty
        if (TextUtils.isEmpty(email)) {
            binding.layoutEmail.setError(getString(R.string.email_required));
            isValid = false;
        }
        // Check email format
        else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.layoutEmail.setError(getString(R.string.email_invalid));
            isValid = false;
        }

        // Check if password is empty
        if (TextUtils.isEmpty(password)) {
            binding.layoutPassword.setError(getString(R.string.password_required));
            isValid = false;
        }
        // Check password length ,Firebase requires at least 6 characters
        else if (password.length() < 6) {
            binding.layoutPassword.setError(getString(R.string.password_too_short));
            isValid = false;
        }

        return isValid;
    }

    // Sign in user
    private void loginUser(String email, String password) {
        // Show progress bar
        showLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        // Sign in success
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        // Check if email is verified
                        if (user != null && !user.isEmailVerified()) {
                            Toast.makeText(getContext(),
                                    getString(R.string.please_verify_email),
                                    Toast.LENGTH_LONG).show();
                            // Optionally, you can send verification email again
                            user.sendEmailVerification();
                        }

                        Toast.makeText(getContext(),
                                getString(R.string.login_success) + " " + user.getEmail(),
                                Toast.LENGTH_SHORT).show();

                        // Navigate to main screen
                        navigateToMainScreen();
                    } else {
                        // Sign in failed
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        String errorMessage = getString(R.string.login_failed) + " ";

                        if (task.getException() != null) {
                            String exceptionMessage = task.getException().getMessage();
                            if (exceptionMessage != null) {
                                if (exceptionMessage.contains("no user record")) {
                                    errorMessage += getString(R.string.user_not_found);
                                } else if (exceptionMessage.contains("password is invalid")) {
                                    errorMessage += getString(R.string.wrong_password);
                                } else if (exceptionMessage.contains("disabled")) {
                                    errorMessage += getString(R.string.account_disabled);
                                } else {
                                    errorMessage += getString(R.string.check_credentials);
                                }
                            }
                        }

                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Register new user
    private void registerUser(String email, String password) {
        // Show progress bar
        showLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity(), task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        // Registration success
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        // Send verification email
                        if (user != null) {
                            user.sendEmailVerification()
                                    .addOnCompleteListener(emailTask -> {
                                        if (emailTask.isSuccessful()) {
                                            Toast.makeText(getContext(),
                                                    getString(R.string.verification_email_sent) + " " + user.getEmail(),
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    });
                        }

                        Toast.makeText(getContext(),
                                getString(R.string.register_success) + " " + user.getEmail(),
                                Toast.LENGTH_SHORT).show();

                        // Navigate to main screen
                        navigateToMainScreen();
                    } else {
                        // Registration failed
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        String errorMessage = getString(R.string.register_failed) + " ";

                        if (task.getException() != null) {
                            String exceptionMessage = task.getException().getMessage();
                            if (exceptionMessage != null) {
                                if (exceptionMessage.contains("email address is already in use")) {
                                    errorMessage += getString(R.string.email_already_used);
                                } else if (exceptionMessage.contains("email address is badly formatted")) {
                                    errorMessage += getString(R.string.email_invalid);
                                } else {
                                    errorMessage += getString(R.string.try_again_later);
                                }
                            }
                        }

                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Reset password
    private void resetPassword(String email) {
        showLoading(true);

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    showLoading(false);

                    if (task.isSuccessful()) {
                        Log.d(TAG, "Password reset email sent");
                        Toast.makeText(getContext(),
                                getString(R.string.password_reset_email_sent),
                                Toast.LENGTH_LONG).show();
                    } else {
                        Log.e(TAG, "Failed to send reset email", task.getException());
                        Toast.makeText(getContext(),
                                getString(R.string.password_reset_failed),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Show/hide loading progress
    private void showLoading(boolean show) {
        if (show) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.buttonLogin.setEnabled(false);
            binding.buttonRegister.setEnabled(false);
            binding.editEmail.setEnabled(false);
            binding.editPassword.setEnabled(false);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.buttonLogin.setEnabled(true);
            binding.buttonRegister.setEnabled(true);
            binding.editEmail.setEnabled(true);
            binding.editPassword.setEnabled(true);
        }
    }

    // Navigate to main screen
    private void navigateToMainScreen() {
        NavHostFragment.findNavController(FirstFragment.this)
                .navigate(R.id.action_FirstFragment_to_SecondFragment);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}