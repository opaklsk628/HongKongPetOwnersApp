package com.example.hongkongpetownersapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.hongkongpetownersapp.databinding.FragmentPetAlbumBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PetAlbumFragment extends Fragment {

    private static final String TAG = "PetAlbumFragment";
    private FragmentPetAlbumBinding binding;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;
    private String petId;
    private String petName;
    private PhotoAdapter adapter;
    private List<Object> photoItems = new ArrayList<>(); // Mix of date headers and photos

    // Gallery picker launcher
    private ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        uploadImageFromGallery(imageUri);
                    }
                }
            }
    );

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentPetAlbumBinding.inflate(inflater, container, false);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

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

        // Set click listeners to show image source dialog
        binding.buttonAddPhoto.setOnClickListener(v -> showImageSourceDialog());
        binding.buttonAddFirstPhoto.setOnClickListener(v -> showImageSourceDialog());

        // Listen for photo result from camera
        getParentFragmentManager().setFragmentResultListener("photoResult",
                getViewLifecycleOwner(), (requestKey, result) -> {
                    String photoPath = result.getString("photoPath");
                    if (photoPath != null) {
                        uploadPhotoFromCamera(photoPath);
                    }
                });
    }

    // Show dialog to choose image source
    private void showImageSourceDialog() {
        String[] options = {"Take Photo", "Choose from Gallery"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Add Photo to Album")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Take photo
                        navigateToCamera();
                    } else if (which == 1) {
                        // Choose from gallery
                        openGallery();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Navigate to camera fragment
    private void navigateToCamera() {
        Bundle bundle = new Bundle();
        bundle.putString("petId", petId);
        NavHostFragment.findNavController(this)
                .navigate(R.id.action_petAlbumFragment_to_cameraFragment, bundle);
    }

    // Open gallery to pick image
    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        galleryLauncher.launch(intent);
    }

    // Upload image from gallery
    private void uploadImageFromGallery(Uri imageUri) {
        // Show progress
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.buttonAddPhoto.setEnabled(false);

        // Create storage reference
        String fileName = "pets/" + petId + "/" + System.currentTimeMillis() + ".jpg";
        StorageReference photoRef = storage.getReference().child(fileName);

        Log.d(TAG, "Uploading gallery image to: " + fileName);

        // Upload file
        photoRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "Upload successful, getting download URL...");
                    // Get download URL
                    photoRef.getDownloadUrl()
                            .addOnSuccessListener(downloadUri -> {
                                Log.d(TAG, "Got download URL: " + downloadUri);
                                savePhotoToFirestore(downloadUri.toString());
                                binding.buttonAddPhoto.setEnabled(true);
                            })
                            .addOnFailureListener(e -> {
                                binding.progressBar.setVisibility(View.GONE);
                                binding.buttonAddPhoto.setEnabled(true);
                                Log.e(TAG, "Failed to get download URL", e);
                                Toast.makeText(getContext(),
                                        "Failed to get photo URL",
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.buttonAddPhoto.setEnabled(true);
                    Log.e(TAG, "Failed to upload photo", e);
                    Toast.makeText(getContext(),
                            "Failed to upload photo",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnProgressListener(snapshot -> {
                    // Calculate upload progress
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    Log.d(TAG, "Upload progress: " + Math.round(progress) + "%");
                });
    }

    // Upload photo from camera
    private void uploadPhotoFromCamera(String photoPath) {
        // Show progress
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.buttonAddPhoto.setEnabled(false);

        File photoFile = new File(photoPath);
        Uri photoUri = Uri.fromFile(photoFile);

        // Create storage reference
        String fileName = "pets/" + petId + "/" + System.currentTimeMillis() + ".jpg";
        StorageReference photoRef = storage.getReference().child(fileName);

        Log.d(TAG, "Uploading camera photo to: " + fileName);

        // Upload file
        photoRef.putFile(photoUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "Upload successful, getting download URL...");
                    // Get download URL
                    photoRef.getDownloadUrl()
                            .addOnSuccessListener(downloadUri -> {
                                Log.d(TAG, "Got download URL: " + downloadUri);
                                savePhotoToFirestore(downloadUri.toString());
                                binding.buttonAddPhoto.setEnabled(true);
                                // Delete local file
                                photoFile.delete();
                            })
                            .addOnFailureListener(e -> {
                                binding.progressBar.setVisibility(View.GONE);
                                binding.buttonAddPhoto.setEnabled(true);
                                Log.e(TAG, "Failed to get download URL", e);
                                Toast.makeText(getContext(),
                                        "Failed to get photo URL",
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.buttonAddPhoto.setEnabled(true);
                    Log.e(TAG, "Failed to upload photo", e);
                    Toast.makeText(getContext(),
                            "Failed to upload photo",
                            Toast.LENGTH_SHORT).show();
                })
                .addOnProgressListener(snapshot -> {
                    // Calculate upload progress
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    Log.d(TAG, "Upload progress: " + Math.round(progress) + "%");
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

    private void savePhotoToFirestore(String photoUrl) {
        // Create new photo object
        Photo photo = new Photo(photoUrl, petId);

        // Save to Firestore
        db.collection("pets").document(petId)
                .collection("photos")
                .add(photo)
                .addOnSuccessListener(documentReference -> {
                    binding.progressBar.setVisibility(View.GONE);
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