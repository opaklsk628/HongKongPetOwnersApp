package com.example.hongkongpetownersapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int TYPE_DATE = 0;
    public static final int TYPE_PHOTO = 1;

    private List<Object> items;
    private OnPhotoClickListener listener;

    public interface OnPhotoClickListener {
        void onPhotoClick(Photo photo);
    }

    public PhotoAdapter(List<Object> items, OnPhotoClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        // Check if item is String (date header) or Photo object
        return items.get(position) instanceof String ? TYPE_DATE : TYPE_PHOTO;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_DATE) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_date_header, parent, false);
            return new DateViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_photo, parent, false);
            return new PhotoViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof DateViewHolder) {
            ((DateViewHolder) holder).bind((String) items.get(position));
        } else if (holder instanceof PhotoViewHolder) {
            ((PhotoViewHolder) holder).bind((Photo) items.get(position), listener);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ViewHolder for date headers
    static class DateViewHolder extends RecyclerView.ViewHolder {
        private TextView textDate;

        public DateViewHolder(@NonNull View itemView) {
            super(itemView);
            textDate = itemView.findViewById(R.id.text_date);
        }

        public void bind(String date) {
            textDate.setText(date);
        }
    }

    // ViewHolder for photos
    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        private ImageView imagePhoto;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePhoto = itemView.findViewById(R.id.image_photo);
        }

        public void bind(Photo photo, OnPhotoClickListener listener) {
            // Load photo using Glide
            Glide.with(itemView.getContext())
                    .load(photo.getUrl())
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(R.drawable.ic_launcher_background)
                    .centerCrop()
                    .into(imagePhoto);

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPhotoClick(photo);
                }
            });
        }
    }
}