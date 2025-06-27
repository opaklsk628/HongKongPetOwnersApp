package com.example.hongkongpetownersapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.hongkongpetownersapp.databinding.FragmentPetAiChatBinding;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Pet AI Chat Fragment using Gemini Flash
 * Reference: https://ai.google.dev/tutorials/android_quickstart
 * Reference: https://github.com/google/generative-ai-android
 * Available Models: https://ai.google.dev/gemini-api/docs/models/gemini
 */
public class PetAIChatFragment extends Fragment {

    private static final String TAG = "PetAIChatFragment";
    private static final String API_KEY = "AIzaSyA7qyL3ZxacdcsK6W5SMMqMw5LrhsRi-S8";

    // Available model options:
    // "gemini-pro" - Standard Gemini Pro
    // "gemini-1.5-flash" - Gemini 1.5 Flash (faster, efficient)
    // "gemini-1.5-pro" - Gemini 1.5 Pro (more capable)
    private static final String MODEL_NAME = "gemini-1.5-flash"; // Using Gemini 1.5 Flash for speed

    private FragmentPetAiChatBinding binding;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messages = new ArrayList<>();
    private GenerativeModelFutures model;
    private Executor executor = Executors.newSingleThreadExecutor();

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentPetAiChatBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Gemini Flash model
        initializeGeminiModel();

        // Setup RecyclerView
        setupRecyclerView();

        // Setup send button
        binding.buttonSend.setOnClickListener(v -> sendMessage());

        // Setup enter key to send
        binding.editMessage.setOnEditorActionListener((v, actionId, event) -> {
            sendMessage();
            return true;
        });

        // Add welcome message in English
        addBotMessage("Hello! I'm your Pet AI Assistant 🐾\n\n" +
                "I can help you with various pet care questions, such as:\n" +
                "• Nutrition and diet advice\n" +
                "• Health and wellness tips\n" +
                "• Training techniques\n" +
                "• Behavioral issues\n" +
                "• Grooming and care\n\n" +
                "What would you like to know about your pet?");
    }

    private void initializeGeminiModel() {
        try {
            // Initialize Gemini model
            // Reference: https://ai.google.dev/gemini-api/docs/get-started/android
            GenerativeModel gm = new GenerativeModel(
                    /* modelName */ MODEL_NAME,
                    /* apiKey */ API_KEY
            );

            model = GenerativeModelFutures.from(gm);
            Log.d(TAG, "Initialized Gemini model: " + MODEL_NAME);
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Gemini model", e);
            Toast.makeText(getContext(), "Failed to initialize AI model", Toast.LENGTH_LONG).show();
        }
    }

    private void setupRecyclerView() {
        chatAdapter = new ChatAdapter(messages);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);
        binding.recyclerChat.setLayoutManager(layoutManager);
        binding.recyclerChat.setAdapter(chatAdapter);
    }

    private void sendMessage() {
        String message = binding.editMessage.getText().toString().trim();
        if (message.isEmpty()) {
            return;
        }

        // Add user message
        addUserMessage(message);

        // Clear input
        binding.editMessage.setText("");

        // Show typing indicator
        binding.progressBar.setVisibility(View.VISIBLE);

        // Send to Gemini with pet context
        sendToGemini(message);
    }

    private void sendToGemini(String userMessage) {
        // Add pet-focused context to the message
        String contextualMessage = "You are a helpful pet care AI assistant. " +
                "You MUST only answer questions related to pets (dogs, cats, birds, fish, rabbits, hamsters, reptiles, etc.). " +
                "Acceptable topics include: pet health, nutrition, training, behavior, grooming, exercise, " +
                "pet products, veterinary care, pet adoption, and general pet ownership advice. " +
                "If someone asks about non-pet topics, politely redirect them to ask about pets instead. " +
                "Keep responses helpful, friendly, concise, and informative. " +
                "User question: " + userMessage;

        try {
            // Create content
            Content content = new Content.Builder()
                    .addText(contextualMessage)
                    .build();

            // Send request to Gemini
            ListenableFuture<GenerateContentResponse> response =
                    model.generateContent(content);

            Futures.addCallback(
                    response,
                    new FutureCallback<GenerateContentResponse>() {
                        @Override
                        public void onSuccess(GenerateContentResponse result) {
                            try {
                                String reply = result.getText();
                                requireActivity().runOnUiThread(() -> {
                                    binding.progressBar.setVisibility(View.GONE);
                                    addBotMessage(reply);
                                });
                            } catch (Exception e) {
                                Log.e(TAG, "Error processing response", e);
                                handleError("Error processing response");
                            }
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            Log.e(TAG, "Error calling Gemini API", t);
                            handleError(t.getMessage());
                        }
                    },
                    executor
            );
        } catch (Exception e) {
            Log.e(TAG, "Error sending message", e);
            handleError("Error sending message");
        }
    }

    private void handleError(String errorMessage) {
        requireActivity().runOnUiThread(() -> {
            binding.progressBar.setVisibility(View.GONE);

            // Show user-friendly error message
            String userMessage = "Sorry, I'm having trouble connecting. Please try again.";

            // Add specific error info for debugging
            if (errorMessage != null && errorMessage.contains("not found")) {
                userMessage = "Sorry, there's a configuration issue. Please contact support.";
                Log.e(TAG, "Model not found error: " + errorMessage);
            } else if (errorMessage != null && errorMessage.contains("API key")) {
                userMessage = "Sorry, there's an authentication issue. Please try again later.";
                Log.e(TAG, "API key error: " + errorMessage);
            }

            addBotMessage(userMessage);

            // Show toast with more details
            Toast.makeText(getContext(),
                    "Error: " + (errorMessage != null ? errorMessage : "Unknown error"),
                    Toast.LENGTH_LONG).show();
        });
    }

    private void addUserMessage(String text) {
        messages.add(new ChatMessage(text, true));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        binding.recyclerChat.scrollToPosition(messages.size() - 1);
    }

    private void addBotMessage(String text) {
        messages.add(new ChatMessage(text, false));
        chatAdapter.notifyItemInserted(messages.size() - 1);
        binding.recyclerChat.scrollToPosition(messages.size() - 1);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        if (executor != null && executor instanceof java.util.concurrent.ExecutorService) {
            ((java.util.concurrent.ExecutorService) executor).shutdown();
        }
    }
}