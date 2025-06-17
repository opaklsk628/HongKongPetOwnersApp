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
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.hongkongpetownersapp.databinding.FragmentPetAlbumBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PetAlbumFragment extends Fragment {

    private static final String TAG = "PetAlbumFragment";
    private FragmentPetAlbumBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String petId;
    private String petName;
    private PhotoAdapter adapter;
    private List<Object> photoItems = new ArrayList<>(); // Mix of date headers and photos

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentPetAlbumBinding.inflate(inflater, container, false);

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

        // Set pet name in title
        if (petName != null) {
            binding.textPetName.setText(petName + "'s Album");
        }

        // Setup RecyclerView with GridLayoutManager
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 3);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // Date headers take full width (3 columns), photos take 1 column
                return adapter.getItemViewType(position) == PhotoAdapter.TYPE_DATE ? 3 : 1;
            }
        });
        binding.recyclerPhotos.setLayoutManager(layoutManager);

        // Initialize adapter
        adapter = new PhotoAdapter(photoItems, photo -> {
            // Handle photo click - for now just show toast
            Toast.makeText(getContext(), "Photo clicked", Toast.LENGTH_SHORT).show();
        });
        binding.recyclerPhotos.setAdapter(adapter);

        // Load photos
        loadPhotos();

        // Set click listeners
        binding.buttonAddPhoto.setOnClickListener(v -> navigateToCamera());
        binding.buttonAddFirstPhoto.setOnClickListener(v -> navigateToCamera());

        // Listen for photo result from camera
        getParentFragmentManager().setFragmentResultListener("photoResult",
                getViewLifecycleOwner(), (requestKey, result) -> {
                    String photoPath = result.getString("photoPath");
                    if (photoPath != null) {
                        uploadPhotoToFirebase(photoPath);
                    }
                });
    }

    private void loadPhotos() {
        // Show progress bar
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.layoutEmpty.setVisibility(View.GONE);

        // Query photos from Firestore
        db.collection("pets").document(petId)
                .collection("photos")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    photoItems.clear();
                    String lastDate = "";
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM d, yyyy", Locale.getDefault());

                    // Process each photo document
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Photo photo = document.toObject(Photo.class);
                        photo.setId(document.getId());

                        // Add date header if it's a new date
                        if (photo.getCreatedAt() != null) {
                            String photoDate = dateFormat.format(photo.getCreatedAt().toDate());
                            if (!photoDate.equals(lastDate)) {
                                photoItems.add(photoDate); // Add date string as header
                                lastDate = photoDate;
                            }
                        }

                        // Add photo
                        photoItems.add(photo);
                    }

                    // Update UI
                    adapter.notifyDataSetChanged();
                    binding.progressBar.setVisibility(View.GONE);

                    // Show empty state if no photos
                    if (photoItems.isEmpty()) {
                        binding.layoutEmpty.setVisibility(View.VISIBLE);
                        binding.recyclerPhotos.setVisibility(View.GONE);
                    } else {
                        binding.layoutEmpty.setVisibility(View.GONE);
                        binding.recyclerPhotos.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading photos", e);
                    Toast.makeText(getContext(), "Error loading photos", Toast.LENGTH_SHORT).show();
                });
    }

    private void navigateToCamera() {
        Bundle bundle = new Bundle();
        bundle.putString("petId", petId);
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_petAlbumFragment_to_cameraFragment, bundle);
    }

    private void uploadPhotoToFirebase(String photoPath) {
        // Show progress
        binding.progressBar.setVisibility(View.VISIBLE);

        java.io.File photoFile = new java.io.File(photoPath);
        android.net.Uri photoUri = android.net.Uri.fromFile(photoFile);

        // Create storage reference
        String fileName = "pets/" + petId + "/" + System.currentTimeMillis() + ".jpg";
        com.google.firebase.storage.StorageReference photoRef =
                com.google.firebase.storage.FirebaseStorage.getInstance()
                        .getReference().child(fileName);

        // Upload file
        photoRef.putFile(photoUri)
                .addOnSuccessListener(taskSnapshot -> {
                    // Get download URL
                    photoRef.getDownloadUrl()
                            .addOnSuccessListener(downloadUri -> {
                                savePhotoToFirestore(downloadUri.toString());
                                // Delete local file
                                photoFile.delete();
                            })
                            .addOnFailureListener(e -> {
                                binding.progressBar.setVisibility(View.GONE);
                                Log.e(TAG, "Failed to get download URL", e);
                                Toast.makeText(getContext(),
                                        "Failed to get photo URL",
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Failed to upload photo", e);
                    Toast.makeText(getContext(),
                            "Failed to upload photo",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void savePhotoToFirestore(String photoUrl) {
        // Create new photo object
        Photo photo = new Photo(photoUrl, petId);

        // Save to Firestore
        db.collection("pets").document(petId)
                .collection("photos")
                .add(photo)
                .addOnSuccessListener(documentReference -> {
                    Log.d(TAG, "Photo saved to Firestore");
                    Toast.makeText(getContext(),
                            "Photo added successfully!",
                            Toast.LENGTH_SHORT).show();
                    // Reload photos to show the new one
                    loadPhotos();
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Failed to save photo to Firestore", e);
                    Toast.makeText(getContext(),
                            "Failed to save photo",
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}