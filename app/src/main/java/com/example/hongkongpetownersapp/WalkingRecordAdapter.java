package com.example.hongkongpetownersapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class WalkingRecordAdapter extends RecyclerView.Adapter<WalkingRecordAdapter.WalkingRecordViewHolder> {

    private List<WalkingRecord> records;
    private OnRecordClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());

    public interface OnRecordClickListener {
        void onRecordClick(WalkingRecord record);
    }

    public WalkingRecordAdapter(List<WalkingRecord> records, OnRecordClickListener listener) {
        this.records = records;
        this.listener = listener;
    }

    @NonNull
    @Override
    public WalkingRecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_walking_record, parent, false);
        return new WalkingRecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WalkingRecordViewHolder holder, int position) {
        WalkingRecord record = records.get(position);
        holder.bind(record);
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    class WalkingRecordViewHolder extends RecyclerView.ViewHolder {
        private TextView textPetName;
        private TextView textDate;
        private TextView textDistance;
        private TextView textSteps;
        private TextView textDuration;
        private TextView textSpeed;

        public WalkingRecordViewHolder(@NonNull View itemView) {
            super(itemView);
            textPetName = itemView.findViewById(R.id.text_pet_name);
            textDate = itemView.findViewById(R.id.text_date);
            textDistance = itemView.findViewById(R.id.text_distance);
            textSteps = itemView.findViewById(R.id.text_steps);
            textDuration = itemView.findViewById(R.id.text_duration);
            textSpeed = itemView.findViewById(R.id.text_speed);
        }

        public void bind(WalkingRecord record) {
            // Pet name
            textPetName.setText(record.getPetName());

            // Date
            if (record.getStartTime() != null) {
                textDate.setText(dateFormat.format(record.getStartTime().toDate()));
            }

            // Distance
            textDistance.setText(String.format("%.2f km", record.getDistance()));

            // Steps
            textSteps.setText(String.format("%,d steps", record.getSteps()));

            // Duration
            textDuration.setText(record.getFormattedDuration());

            // Speed
            textSpeed.setText(String.format("%.1f km/h", record.getAvgSpeed()));

            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRecordClick(record);
                }
            });
        }
    }
}