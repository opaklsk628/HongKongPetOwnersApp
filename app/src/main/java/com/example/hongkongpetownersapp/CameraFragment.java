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
import java.util.concurrent.ExecutionException;

public class CameraFragment extends Fragment {

    private static final String TAG = "CameraFragment";
    private FragmentCameraBinding binding;
    private ImageCapture imageCapture;
    private String petId;

    // request the permission
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

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        // set the action if click camera button
        binding.buttonCapture.setOnClickListener(v -> takePhoto());
        binding.buttonClose.setOnClickListener(v -> navigateBack());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(requireContext());

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                Toast.makeText(getContext(),
                        "Error starting camera",
                        Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        // 預覽
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

        // 拍照
        imageCapture = new ImageCapture.Builder()
                .setTargetRotation(requireActivity().getWindowManager().getDefaultDisplay().getRotation())
                .build();

        // 選擇後置相機
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        try {
            // 解除所有綁定
            cameraProvider.unbindAll();

            // 綁定到生命週期
            cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture);

        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    private void takePhoto() {
        if (imageCapture == null) {
            return;
        }

        // 顯示進度條
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.buttonCapture.setEnabled(false);

        // 創建檔案名稱
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                .format(System.currentTimeMillis());
        String fileName = "pet_" + petId + "_" + timestamp + ".jpg";

        // 創建檔案
        File photoFile = new File(requireContext().getFilesDir(), fileName);

        // 設定輸出選項
        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        // 拍照
        imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(requireContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.buttonCapture.setEnabled(true);

                        Uri savedUri = Uri.fromFile(photoFile);
                        Log.d(TAG, "Photo saved: " + savedUri);

                        // 傳遞照片路徑回上一頁
                        Bundle result = new Bundle();
                        result.putString("photoUri", savedUri.toString());
                        result.putString("photoPath", photoFile.getAbsolutePath());
                        getParentFragmentManager().setFragmentResult("photoResult", result);

                        Toast.makeText(getContext(),
                                "Photo saved successfully",
                                Toast.LENGTH_SHORT).show();

                        navigateBack();
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.buttonCapture.setEnabled(true);

                        Log.e(TAG, "Photo capture failed", exception);
                        Toast.makeText(getContext(),
                                "Failed to save photo",
                                Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void navigateBack() {
        NavHostFragment.findNavController(this).navigateUp();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}