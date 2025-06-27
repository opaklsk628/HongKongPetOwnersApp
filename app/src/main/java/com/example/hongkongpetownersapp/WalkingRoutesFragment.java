package com.example.hongkongpetownersapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hongkongpetownersapp.databinding.FragmentWalkingRoutesBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class WalkingRoutesFragment extends Fragment {

    private static final String TAG = "WalkingRoutesFragment";
    private FragmentWalkingRoutesBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private WalkingRecordAdapter adapter;
    private List<WalkingRecord> records = new ArrayList<>();
    private String petId;
    private String petName;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentWalkingRoutesBinding.inflate(inflater, container, false);

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

        // Check authentication
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            NavHostFragment.findNavController(this)
                    .navigate(R.id.FirstFragment);
            return;
        }

        // Set title
        if (petName != null) {
            binding.textTitle.setText(petName + "'s Walking Records");
        } else {
            binding.textTitle.setText("All Walking Records");
        }

        // Setup RecyclerView
        setupRecyclerView();

        // Setup FAB
        binding.fabAddRecord.setOnClickListener(v -> {
            // Navigate to pet selection if no pet selected
            if (petId == null) {
                Bundle bundle = new Bundle();
                bundle.putString("mode", "walking");
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_walkingRoutesFragment_to_petListFragment, bundle);
            } else {
                // Start recording for selected pet
                Bundle bundle = new Bundle();
                bundle.putString("petId", petId);
                bundle.putString("petName", petName);
                NavHostFragment.findNavController(this)
                        .navigate(R.id.action_walkingRoutesFragment_to_recordWalkingFragment, bundle);
            }
        });

        // Load records
        loadWalkingRecords();
    }

    private void setupRecyclerView() {
        adapter = new WalkingRecordAdapter(records, record -> {
            // Handle record click - show details
            Toast.makeText(getContext(),
                    "Distance: " + String.format("%.2f km", record.getDistance()) +
                            "\nSteps: " + record.getSteps(),
                    Toast.LENGTH_LONG).show();
        });

        binding.recyclerRecords.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerRecords.setAdapter(adapter);
    }

    private void loadWalkingRecords() {
        binding.progressBar.setVisibility(View.VISIBLE);

        Query query;
        if (petId != null) {
            // Load records for specific pet
            query = db.collection("walking_records")
                    .whereEqualTo("petId", petId)
                    .orderBy("startTime", Query.Direction.DESCENDING);
        } else {
            // Load all records for current user
            FirebaseUser currentUser = mAuth.getCurrentUser();
            query = db.collection("walking_records")
                    .whereEqualTo("ownerId", currentUser.getUid())
                    .orderBy("startTime", Query.Direction.DESCENDING);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    records.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            WalkingRecord record = document.toObject(WalkingRecord.class);
                            records.add(record);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing record", e);
                        }
                    }

                    adapter.notifyDataSetChanged();
                    binding.progressBar.setVisibility(View.GONE);
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading records", e);

                    // Try without ordering
                    loadRecordsSimple();
                });
    }

    private void loadRecordsSimple() {
        Query query;
        if (petId != null) {
            query = db.collection("walking_records")
                    .whereEqualTo("petId", petId);
        } else {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            query = db.collection("walking_records")
                    .whereEqualTo("ownerId", currentUser.getUid());
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    records.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            WalkingRecord record = document.toObject(WalkingRecord.class);
                            records.add(record);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing record", e);
                        }
                    }

                    // Sort locally by start time
                    records.sort((r1, r2) -> {
                        if (r1.getStartTime() != null && r2.getStartTime() != null) {
                            return r2.getStartTime().compareTo(r1.getStartTime());
                        }
                        return 0;
                    });

                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading records (simple)", e);
                    Toast.makeText(getContext(), "Error loading records", Toast.LENGTH_SHORT).show();
                    updateEmptyState();
                });
    }

    private void updateEmptyState() {
        if (records.isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.recyclerRecords.setVisibility(View.GONE);
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.recyclerRecords.setVisibility(View.VISIBLE);

            // Calculate totals
            calculateTotals();
        }
    }

    private void calculateTotals() {
        double totalDistance = 0;
        int totalSteps = 0;
        long totalDuration = 0;

        for (WalkingRecord record : records) {
            totalDistance += record.getDistance();
            totalSteps += record.getSteps();
            totalDuration += record.getDuration();
        }

        // Update summary
        binding.textTotalDistance.setText(String.format("Total: %.2f km", totalDistance));
        binding.textTotalSteps.setText(String.format("%,d steps", totalSteps));

        // Format total time
        long hours = totalDuration / (1000 * 60 * 60);
        long minutes = (totalDuration / (1000 * 60)) % 60;
        if (hours > 0) {
            binding.textTotalTime.setText(String.format("%d h %d min", hours, minutes));
        } else {
            binding.textTotalTime.setText(String.format("%d min", minutes));
        }

        binding.layoutSummary.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}