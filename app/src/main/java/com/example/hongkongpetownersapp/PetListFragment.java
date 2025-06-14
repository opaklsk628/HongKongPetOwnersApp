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

import com.example.hongkongpetownersapp.databinding.FragmentPetListBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class PetListFragment extends Fragment {

    private static final String TAG = "PetListFragment";
    private FragmentPetListBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private PetAdapter petAdapter;
    private List<Pet> petList = new ArrayList<>();

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentPetListBinding.inflate(inflater, container, false);

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

        // Setup RecyclerView
        setupRecyclerView();

        // Setup click listeners
        binding.fabAddPet.setOnClickListener(v -> navigateToAddPet());
        binding.buttonAddFirstPet.setOnClickListener(v -> navigateToAddPet());

        // Load pets
        loadPets();
    }

    private void setupRecyclerView() {
        petAdapter = new PetAdapter(petList);
        binding.recyclerViewPets.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewPets.setAdapter(petAdapter);
    }

    private void loadPets() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        binding.progressBar.setVisibility(View.VISIBLE);

        db.collection("pets")
                .whereEqualTo("ownerId", currentUser.getUid())
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    binding.progressBar.setVisibility(View.GONE);
                    petList.clear();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Pet pet = document.toObject(Pet.class);
                        pet.setId(document.getId());
                        petList.add(pet);
                    }

                    petAdapter.notifyDataSetChanged();
                    updateEmptyState();
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading pets", e);
                    Toast.makeText(getContext(),
                            getString(R.string.error_loading_pets),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void updateEmptyState() {
        if (petList.isEmpty()) {
            binding.emptyState.setVisibility(View.VISIBLE);
            binding.recyclerViewPets.setVisibility(View.GONE);
        } else {
            binding.emptyState.setVisibility(View.GONE);
            binding.recyclerViewPets.setVisibility(View.VISIBLE);
        }
    }

    private void navigateToAddPet() {
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_petListFragment_to_addPetFragment);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}