package com.example.hongkongpetownersapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class PetAdapter extends RecyclerView.Adapter<PetAdapter.PetViewHolder> {

    private List<Pet> pets;

    public PetAdapter(List<Pet> pets) {
        this.pets = pets;
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
        holder.bind(pet);
    }

    @Override
    public int getItemCount() {
        return pets.size();
    }

    static class PetViewHolder extends RecyclerView.ViewHolder {
        private TextView textPetName;
        private TextView textPetType;
        private TextView textPetIcon;

        public PetViewHolder(@NonNull View itemView) {
            super(itemView);
            textPetName = itemView.findViewById(R.id.text_pet_name);
            textPetType = itemView.findViewById(R.id.text_pet_type);
            textPetIcon = itemView.findViewById(R.id.text_pet_icon);
        }

        public void bind(Pet pet) {
            textPetName.setText(pet.getName());
            textPetType.setText(pet.getType());

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
    }
}