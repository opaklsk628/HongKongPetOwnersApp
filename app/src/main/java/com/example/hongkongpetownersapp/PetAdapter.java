package com.example.hongkongpetownersapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class PetAdapter extends RecyclerView.Adapter<PetAdapter.PetViewHolder> {

    private List<Pet> pets;
    private String mode; // Navigation mode: "details", "album", "vaccine", "reminder", or "walking"

    // Constructor with mode parameter
    public PetAdapter(List<Pet> pets, String mode) {
        this.pets = pets;
        this.mode = mode;
    }

    @NonNull
    @Override
    public PetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pet, parent, false);
        return new PetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PetViewHolder holder, int position) {
        Pet pet = pets.get(position);
        holder.bind(pet, mode); // Pass mode to bind method
    }

    @Override
    public int getItemCount() {
        return pets.size();
    }

    static class PetViewHolder extends RecyclerView.ViewHolder {
        private TextView textPetName;
        private TextView textPetType;
        private TextView textPetIcon;
        private ImageView imagePetPhoto;

        public PetViewHolder(@NonNull View itemView) {
            super(itemView);
            textPetName = itemView.findViewById(R.id.text_pet_name);
            textPetType = itemView.findViewById(R.id.text_pet_type);
            textPetIcon = itemView.findViewById(R.id.text_pet_icon);
            imagePetPhoto = itemView.findViewById(R.id.image_pet_photo);
        }

        public void bind(Pet pet, String mode) {
            textPetName.setText(pet.getName());
            textPetType.setText(pet.getType());

            // Display photo if available
            if (pet.getPhotoUrl() != null && !pet.getPhotoUrl().isEmpty()) {
                imagePetPhoto.setVisibility(View.VISIBLE);
                textPetIcon.setVisibility(View.GONE);

                Glide.with(itemView.getContext())
                        .load(pet.getPhotoUrl())
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .centerCrop()
                        .into(imagePetPhoto);
            } else {
                imagePetPhoto.setVisibility(View.GONE);
                textPetIcon.setVisibility(View.VISIBLE);

                // Set icon based on pet type
                switch (pet.getType().toLowerCase()) {
                    case "dog":
                        textPetIcon.setText("🐕 Dog");
                        break;
                    case "cat":
                        textPetIcon.setText("🐈 Cat");
                        break;
                    case "bird":
                        textPetIcon.setText("🦜 Bird");
                        break;
                    case "fish":
                        textPetIcon.setText("🐠 Fish");
                        break;
                    case "rabbit":
                        textPetIcon.setText("🐰 Rabbit");
                        break;
                    default:
                        textPetIcon.setText("🐾 Pet");
                        break;
                }
            }

            // Set click listener based on mode
            itemView.setOnClickListener(v -> {
                Bundle bundle = new Bundle();
                bundle.putString("petId", pet.getId());

                if ("album".equals(mode)) {
                    // Album mode: navigate to pet album
                    bundle.putString("petName", pet.getName());
                    Navigation.findNavController(v)
                            .navigate(R.id.action_petListFragment_to_petAlbumFragment, bundle);
                } else if ("vaccine".equals(mode)) {
                    // Vaccine mode: navigate to vaccine records
                    bundle.putString("petName", pet.getName());
                    Navigation.findNavController(v)
                            .navigate(R.id.action_petListFragment_to_vaccineRecordsFragment, bundle);
                } else if ("reminder".equals(mode)) {
                    // Health reminder mode: navigate to health reminders for specific pet
                    bundle.putString("petName", pet.getName());
                    Navigation.findNavController(v)
                            .navigate(R.id.action_petListFragment_to_healthRemindersFragment, bundle);
                } else if ("walking".equals(mode)) {
                    // Walking mode: start recording walk for selected pet
                    bundle.putString("petName", pet.getName());
                    Navigation.findNavController(v)
                            .navigate(R.id.action_petListFragment_to_recordWalkingFragment, bundle);
                } else {
                    // Details mode (default): navigate to pet details
                    Navigation.findNavController(v)
                            .navigate(R.id.action_petListFragment_to_petDetailFragment, bundle);
                }
            });
        }
    }
}