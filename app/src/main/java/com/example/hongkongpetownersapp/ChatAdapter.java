package com.example.hongkongpetownersapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ChatViewHolder> {

    private static final int VIEW_TYPE_USER = 1;
    private static final int VIEW_TYPE_BOT = 2;

    private List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).isUser() ? VIEW_TYPE_USER : VIEW_TYPE_BOT;
    }

    @NonNull
    @Override
    public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chat_message, parent, false);
        return new ChatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChatViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class ChatViewHolder extends RecyclerView.ViewHolder {
        private TextView textMessage;
        private TextView textSender;
        private View messageContainer;

        public ChatViewHolder(@NonNull View itemView) {
            super(itemView);
            textMessage = itemView.findViewById(R.id.text_message);
            textSender = itemView.findViewById(R.id.text_sender);
            messageContainer = itemView.findViewById(R.id.message_container);
        }

        public void bind(ChatMessage message) {
            textMessage.setText(message.getText());

            if (message.isUser()) {
                // User message - align to right
                textSender.setText("You");
                messageContainer.setBackgroundResource(R.drawable.bg_user_message);
                ((ViewGroup.MarginLayoutParams) messageContainer.getLayoutParams()).leftMargin = 100;
                ((ViewGroup.MarginLayoutParams) messageContainer.getLayoutParams()).rightMargin = 16;
            } else {
                // Bot message - align to left
                textSender.setText("Pet AI");
                messageContainer.setBackgroundResource(R.drawable.bg_bot_message);
                ((ViewGroup.MarginLayoutParams) messageContainer.getLayoutParams()).leftMargin = 16;
                ((ViewGroup.MarginLayoutParams) messageContainer.getLayoutParams()).rightMargin = 100;
            }
        }
    }
}