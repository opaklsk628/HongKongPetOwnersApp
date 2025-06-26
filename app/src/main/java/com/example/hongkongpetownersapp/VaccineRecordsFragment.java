package com.example.hongkongpetownersapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hongkongpetownersapp.databinding.FragmentVaccineRecordsBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class VaccineRecordsFragment extends Fragment {

    private static final String TAG = "VaccineRecordsFragment";
    private FragmentVaccineRecordsBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String petId;
    private String petName;
    private VaccineRecordAdapter adapter;
    private List<VaccineRecord> vaccineRecords = new ArrayList<>();
    private boolean isAuthenticated = false;

    // Biometric authentication
    private Executor executor;
    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentVaccineRecordsBinding.inflate(inflater, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get arguments
        if (getArguments() != null) {
            petId = getArguments().getString("petId");
            petName = getArguments().getString("petName");
        }

        // Setup biometric authentication
        setupBiometricAuth();

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

        // Set pet name in title
        if (petName != null) {
            binding.textTitle.setText(petName + "'s Vaccine Records");
        }

        // Setup RecyclerView
        setupRecyclerView();

        // Hide content initially
        binding.layoutContent.setVisibility(View.GONE);
        binding.layoutLocked.setVisibility(View.VISIBLE);

        // Setup unlock button
        binding.buttonUnlock.setOnClickListener(v -> authenticateUser());

        // Setup add button (only visible after authentication)
        binding.buttonAddVaccine.setOnClickListener(v -> {
            if (isAuthenticated) {
                navigateToAddVaccine();
            } else {
                Toast.makeText(getContext(),
                        "Please authenticate first",
                        Toast.LENGTH_SHORT).show();
            }
        });

        // Automatically prompt for authentication
        authenticateUser();
    }

    private void setupBiometricAuth() {
        executor = ContextCompat.getMainExecutor(requireContext());

        biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode,
                                                      @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        Log.e(TAG, "Authentication error: " + errString);
                        Toast.makeText(getContext(),
                                "Authentication error: " + errString,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        Log.d(TAG, "Authentication succeeded!");
                        isAuthenticated = true;
                        showVaccineRecords();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        Log.e(TAG, "Authentication failed");
                        Toast.makeText(getContext(),
                                "Authentication failed",
                                Toast.LENGTH_SHORT).show();
                    }
                });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Authenticate to view vaccine records")
                .setSubtitle("Use your fingerprint or face to access pet health data")
                .setNegativeButtonText("Cancel")
                .build();
    }

    private void authenticateUser() {
        biometricPrompt.authenticate(promptInfo);
    }

    private void showVaccineRecords() {
        // Show content, hide locked screen
        binding.layoutContent.setVisibility(View.VISIBLE);
        binding.layoutLocked.setVisibility(View.GONE);

        // Show add vaccine button
        binding.buttonAddVaccine.setVisibility(View.VISIBLE);

        // Load vaccine records
        loadVaccineRecords();
    }

    private void setupRecyclerView() {
        adapter = new VaccineRecordAdapter(vaccineRecords, record -> {
            // Handle record click - navigate to edit
            if (isAuthenticated) {
                Bundle bundle = new Bundle();
                bundle.putString("petId", petId);
                bundle.putString("petName", petName);
                bundle.putString("recordId", record.getId());
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_vaccineRecordsFragment_to_vaccineDetailFragment, bundle);
            }
        });

        binding.recyclerVaccineRecords.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerVaccineRecords.setAdapter(adapter);
    }

    private void loadVaccineRecords() {
        if (petId == null) return;

        binding.progressBar.setVisibility(View.VISIBLE);

        db.collection("pets").document(petId)
                .collection("vaccines")
                .orderBy("vaccinationDate", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    vaccineRecords.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        VaccineRecord record = document.toObject(VaccineRecord.class);
                        vaccineRecords.add(record);
                    }

                    adapter.notifyDataSetChanged();
                    binding.progressBar.setVisibility(View.GONE);

                    // Update empty state
                    if (vaccineRecords.isEmpty()) {
                        binding.layoutEmpty.setVisibility(View.VISIBLE);
                        binding.recyclerVaccineRecords.setVisibility(View.GONE);
                    } else {
                        binding.layoutEmpty.setVisibility(View.GONE);
                        binding.recyclerVaccineRecords.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading vaccine records", e);
                    Toast.makeText(getContext(),
                            "Error loading records",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToAddVaccine() {
        Bundle bundle = new Bundle();
        bundle.putString("petId", petId);
        bundle.putString("petName", petName);
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_vaccineRecordsFragment_to_addVaccineFragment, bundle);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}