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

public class VaccineRecordAdapter extends RecyclerView.Adapter<VaccineRecordAdapter.VaccineRecordViewHolder> {

    private List<VaccineRecord> records;
    private OnRecordClickListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public interface OnRecordClickListener {
        void onRecordClick(VaccineRecord record);
    }

    public VaccineRecordAdapter(List<VaccineRecord> records, OnRecordClickListener listener) {
        this.records = records;
        this.listener = listener;
    }

    @NonNull
    @Override
    public VaccineRecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vaccine_record, parent, false);
        return new VaccineRecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VaccineRecordViewHolder holder, int position) {
        VaccineRecord record = records.get(position);
        holder.bind(record);
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    class VaccineRecordViewHolder extends RecyclerView.ViewHolder {
        private TextView textVaccineName;
        private TextView textVaccinationDate;
        private TextView textNextDueDate;
        private TextView textVeterinarian;
        private TextView textClinic;
        private TextView textDueStatus;

        public VaccineRecordViewHolder(@NonNull View itemView) {
            super(itemView);
            textVaccineName = itemView.findViewById(R.id.text_vaccine_name);
            textVaccinationDate = itemView.findViewById(R.id.text_vaccination_date);
            textNextDueDate = itemView.findViewById(R.id.text_next_due_date);
            textVeterinarian = itemView.findViewById(R.id.text_veterinarian);
            textClinic = itemView.findViewById(R.id.text_clinic);
            textDueStatus = itemView.findViewById(R.id.text_due_status);
        }

        public void bind(VaccineRecord record) {
            textVaccineName.setText(record.getVaccineName());

            // Format dates
            if (record.getVaccinationDate() != null) {
                textVaccinationDate.setText("Given: " + dateFormat.format(record.getVaccinationDate().toDate()));
            }

            if (record.getNextDueDate() != null) {
                String nextDue = dateFormat.format(record.getNextDueDate().toDate());
                textNextDueDate.setText("Next due: " + nextDue);

                // Check if overdue
                long currentTime = System.currentTimeMillis();
                long dueTime = record.getNextDueDate().toDate().getTime();

                if (dueTime < currentTime) {
                    // Overdue
                    textDueStatus.setText("OVERDUE");
                    textDueStatus.setTextColor(itemView.getContext().getColor(android.R.color.holo_red_dark));
                    textDueStatus.setVisibility(View.VISIBLE);
                } else if (dueTime - currentTime < 30L * 24 * 60 * 60 * 1000) {
                    // Due within 30 days
                    textDueStatus.setText("DUE SOON");
                    textDueStatus.setTextColor(itemView.getContext().getColor(android.R.color.holo_orange_dark));
                    textDueStatus.setVisibility(View.VISIBLE);
                } else {
                    textDueStatus.setVisibility(View.GONE);
                }
            } else {
                textNextDueDate.setText("Next due: Not set");
                textDueStatus.setVisibility(View.GONE);
            }

            // Set veterinarian and clinic
            if (record.getVeterinarian() != null && !record.getVeterinarian().isEmpty()) {
                textVeterinarian.setText("Vet: " + record.getVeterinarian());
                textVeterinarian.setVisibility(View.VISIBLE);
            } else {
                textVeterinarian.setVisibility(View.GONE);
            }

            if (record.getClinic() != null && !record.getClinic().isEmpty()) {
                textClinic.setText("Clinic: " + record.getClinic());
                textClinic.setVisibility(View.VISIBLE);
            } else {
                textClinic.setVisibility(View.GONE);
            }

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRecordClick(record);
                }
            });
        }
    }
}