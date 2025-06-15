package com.example.hongkongpetownersapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.hongkongpetownersapp.databinding.FragmentCameraBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class CameraFragment extends Fragment {

    private static final String TAG = "CameraFragment";
    private FragmentCameraBinding binding;
    private ImageCapture imageCapture;
    private String petId;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(getContext(),
                            "Camera permission is required to take photos",
                            Toast.LENGTH_LONG).show();
                    navigateBack();
                }
            });

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentCameraBinding.inflate(inflater, container, false);

        if (getArguments() != null) {
            petId = getArguments().getString("petId");
        }

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        binding.buttonCapture.setOnClickListener(v -> {
            Log.d(TAG, "Capture button clicked");
            Toast.makeText(getContext(), "Taking photo...", Toast.LENGTH_SHORT).show();
            takePhoto();
        });

        binding.buttonClose.setOnClickListener(v -> navigateBack());
    }

    private void startCamera() {
        Log.d(TAG, "Starting camera...");

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();

                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageCapture);

                Log.d(TAG, "Camera started successfully");
                Toast.makeText(getContext(), "Camera ready", Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                Log.e(TAG, "Failed to start camera", e);
                Toast.makeText(getContext(),
                        "Failed to start camera: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void takePhoto() {
        Log.d(TAG, "takePhoto() called");

        if (imageCapture == null) {
            Log.e(TAG, "imageCapture is null");
            Toast.makeText(getContext(), "Camera not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.progressBar.setVisibility(View.VISIBLE);
        binding.buttonCapture.setEnabled(false);

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(System.currentTimeMillis());
        String fileName = "pet_" + petId + "_" + timestamp + ".jpg";
        File photoFile = new File(requireContext().getExternalFilesDir(null), fileName);

        Log.d(TAG, "Photo will be saved to: " + photoFile.getAbsolutePath());

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        Log.d(TAG, "Photo saved successfully");

                        if (binding != null) {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.buttonCapture.setEnabled(true);
                        }

                        // 傳遞結果
                        Uri savedUri = Uri.fromFile(photoFile);
                        Bundle result = new Bundle();
                        result.putString("photoUri", savedUri.toString());
                        result.putString("photoPath", photoFile.getAbsolutePath());
                        getParentFragmentManager().setFragmentResult("photoResult", result);

                        Toast.makeText(getContext(),
                                "Photo saved! Path: " + photoFile.getName(),
                                Toast.LENGTH_LONG).show();

                        navigateBack();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);

                        if (binding != null) {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.buttonCapture.setEnabled(true);
                        }

                        Toast.makeText(getContext(),
                                "Failed: " + exception.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void navigateBack() {
        if (isAdded()) {
            NavHostFragment.findNavController(this).navigateUp();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}