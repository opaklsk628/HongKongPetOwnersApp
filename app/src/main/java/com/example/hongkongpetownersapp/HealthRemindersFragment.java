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

import com.example.hongkongpetownersapp.databinding.FragmentHealthRemindersBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HealthRemindersFragment extends Fragment {

    private static final String TAG = "HealthRemindersFragment";
    private FragmentHealthRemindersBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private HealthReminderAdapter adapter;
    private List<HealthReminder> reminders = new ArrayList<>();
    private String petId;
    private String petName;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentHealthRemindersBinding.inflate(inflater, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        // Get arguments
        if (getArguments() != null) {
            petId = getArguments().getString("petId");
            petName = getArguments().getString("petName");
        }

        Log.d(TAG, "onCreateView - petId: " + petId + ", petName: " + petName);

        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Check authentication
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "User not authenticated");
            NavHostFragment.findNavController(this)
                    .navigate(R.id.FirstFragment);
            return;
        }

        Log.d(TAG, "Current user: " + currentUser.getUid());

        // Set title
        if (petName != null) {
            binding.textTitle.setText(petName + "'s Health Reminders");
        } else {
            binding.textTitle.setText("All Health Reminders");
        }

        // Setup RecyclerView
        setupRecyclerView();

        // Setup FAB
        binding.fabAddReminder.setOnClickListener(v -> {
            Bundle bundle = new Bundle();
            if (petId != null) {
                bundle.putString("petId", petId);
                bundle.putString("petName", petName);
            }
            NavHostFragment.findNavController(this)
                    .navigate(R.id.action_healthRemindersFragment_to_addHealthReminderFragment, bundle);
        });


        // Load reminders
        loadReminders();
    }

    private void setupRecyclerView() {
        adapter = new HealthReminderAdapter(reminders,
                // Click listener
                reminder -> {
                    Bundle bundle = new Bundle();
                    bundle.putString("reminderId", reminder.getId());
                    bundle.putString("petId", reminder.getPetId());
                    bundle.putString("petName", reminder.getPetName());
                    NavHostFragment.findNavController(this)
                            .navigate(R.id.action_healthRemindersFragment_to_healthReminderDetailFragment, bundle);
                },
                // Toggle listener
                (reminder, isActive) -> {
                    updateReminderStatus(reminder, isActive);
                }
        );

        binding.recyclerReminders.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerReminders.setAdapter(adapter);
    }

    private void loadReminders() {
        Log.d(TAG, "Starting to load reminders...");
        binding.progressBar.setVisibility(View.VISIBLE);

        // Clear existing reminders
        reminders.clear();

        if (petId != null) {
            loadRemindersForSpecificPet();
        } else {
            loadAllUserReminders();
        }
    }


    private void loadRemindersForSpecificPet() {
        Log.d(TAG, "Loading reminders for specific pet: " + petId);

        db.collection("reminders")
                .whereEqualTo("petId", petId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d(TAG, "✓ Pet reminders query successful");
                    Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " reminders for pet");

                    reminders.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {


                            HealthReminder reminder = document.toObject(HealthReminder.class);
                            reminder.setId(document.getId());

                            // Ensure pet name is set
                            if ((reminder.getPetName() == null || reminder.getPetName().isEmpty()) && petName != null) {
                                reminder.setPetName(petName);
                            }

                            reminders.add(reminder);

                        } catch (Exception e) {
                            Log.e(TAG, "✗ Error parsing reminder: " + document.getId(), e);
                        }
                    }

                    sortRemindersByTime();
                    adapter.notifyDataSetChanged();
                    binding.progressBar.setVisibility(View.GONE);
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Failed to load pet reminders", e);
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(),
                            "Failed to load reminders: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    updateEmptyState();
                });
    }

    private void loadAllUserReminders() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "Current user is null");
            binding.progressBar.setVisibility(View.GONE);
            return;
        }

        final String userId = currentUser.getUid();
        Log.d(TAG, "Loading all reminders for user: " + userId);

        // First get user's pets
        db.collection("pets")
                .whereEqualTo("ownerId", userId)
                .get()
                .addOnSuccessListener(petSnapshots -> {
                    Log.d(TAG, "✓ Pets query successful");
                    Log.d(TAG, "Found " + petSnapshots.size() + " pets for user");

                    if (petSnapshots.isEmpty()) {
                        Log.d(TAG, "No pets found for user");
                        binding.progressBar.setVisibility(View.GONE);
                        updateEmptyState();
                        return;
                    }

                    // Create list of pet IDs and names
                    List<String> petIds = new ArrayList<>();
                    Map<String, String> petNamesMap = new HashMap<>();

                    for (QueryDocumentSnapshot doc : petSnapshots) {
                        try {
                            String id = doc.getId();
                            Pet pet = doc.toObject(Pet.class);
                            petIds.add(id);
                            petNamesMap.put(id, pet.getName());

                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing pet", e);
                        }
                    }

                    // Now load reminders for these pets
                    loadRemindersForPetList(petIds, petNamesMap);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "✗ Failed to load user pets", e);
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(),
                            "Failed to load pets: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    updateEmptyState();
                });
    }

    private void loadRemindersForPetList(List<String> petIds, Map<String, String> petNamesMap) {
        Log.d(TAG, "Loading reminders for " + petIds.size() + " pets");

        reminders.clear();
        final int[] completedQueries = {0};

        // If no pets, just update empty state
        if (petIds.isEmpty()) {
            binding.progressBar.setVisibility(View.GONE);
            updateEmptyState();
            return;
        }

        // Load reminders for each pet
        for (String petId : petIds) {
            db.collection("reminders")
                    .whereEqualTo("petId", petId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && task.getResult() != null) {
                            Log.d(TAG, "✓ Loaded reminders for pet: " + petId);

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    HealthReminder reminder = document.toObject(HealthReminder.class);
                                    reminder.setId(document.getId());

                                    // Ensure pet name is set
                                    if (reminder.getPetName() == null || reminder.getPetName().isEmpty()) {
                                        reminder.setPetName(petNamesMap.get(petId));
                                    }

                                    reminders.add(reminder);
                                    Log.d(TAG, "Added reminder: " + reminder.getTitle());
                                } catch (Exception e) {
                                    Log.e(TAG, "Error parsing reminder", e);
                                }
                            }
                        } else {
                            Log.e(TAG, "✗ Failed to load reminders for pet: " + petId);
                        }

                        completedQueries[0]++;

                        // Check if all queries are complete
                        if (completedQueries[0] == petIds.size()) {
                            Log.d(TAG, "All queries completed. Total reminders: " + reminders.size());
                            sortRemindersByTime();
                            adapter.notifyDataSetChanged();
                            binding.progressBar.setVisibility(View.GONE);
                            updateEmptyState();
                        }
                    });
        }
    }

    private void sortRemindersByTime() {
        reminders.sort((r1, r2) -> {
            int hourCompare = Integer.compare(r1.getHour(), r2.getHour());
            if (hourCompare != 0) return hourCompare;
            return Integer.compare(r1.getMinute(), r2.getMinute());
        });
        Log.d(TAG, "Reminders sorted by time");
    }

    private void updateReminderStatus(HealthReminder reminder, boolean isActive) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("isActive", isActive);

        db.collection("reminders").document(reminder.getId())
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    reminder.setActive(isActive);
                    Toast.makeText(getContext(),
                            isActive ? "Reminder activated" : "Reminder deactivated",
                            Toast.LENGTH_SHORT).show();

                    // Update local notification
                    if (isActive) {
                        LocalNotificationHelper.scheduleRepeatingNotification(getContext(), reminder);
                    } else {
                        LocalNotificationHelper.cancelNotification(getContext(), reminder.getId());
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error updating reminder status", e);
                    Toast.makeText(getContext(),
                            "Failed to update status",
                            Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged();
                });
    }

    private void updateEmptyState() {
        if (reminders.isEmpty()) {
            binding.layoutEmpty.setVisibility(View.VISIBLE);
            binding.recyclerReminders.setVisibility(View.GONE);
            Log.d(TAG, "Showing empty state");
        } else {
            binding.layoutEmpty.setVisibility(View.GONE);
            binding.recyclerReminders.setVisibility(View.VISIBLE);
            Log.d(TAG, "Showing " + reminders.size() + " reminders");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}