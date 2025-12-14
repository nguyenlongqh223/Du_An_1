package com.poly.ban_giay_app.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.poly.ban_giay_app.R;
import com.poly.ban_giay_app.network.model.NotificationResponse;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {
    private final List<NotificationResponse> items = new ArrayList<>();

    public void setItems(List<NotificationResponse> data) {
        android.util.Log.d("NotificationAdapter", "setItems called with: " + (data != null ? data.size() : "null") + " items");
        items.clear();
        if (data != null) {
            items.addAll(data);
            android.util.Log.d("NotificationAdapter", "Added " + items.size() + " items to adapter");
        }
        notifyDataSetChanged();
        android.util.Log.d("NotificationAdapter", "notifyDataSetChanged() called, getItemCount() = " + getItemCount());
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        if (position < 0 || position >= items.size()) {
            android.util.Log.e("NotificationAdapter", "Invalid position: " + position + ", items size: " + items.size());
            return;
        }
        NotificationResponse item = items.get(position);
        android.util.Log.d("NotificationAdapter", "onBindViewHolder position: " + position + ", title: " + (item != null ? item.getTitle() : "null"));
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        int count = items.size();
        android.util.Log.d("NotificationAdapter", "getItemCount: " + count);
        return count;
    }
    
    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= items.size()) {
            return RecyclerView.NO_ID;
        }
        NotificationResponse item = items.get(position);
        return item != null && item.getId() != null ? item.getId().hashCode() : position;
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvTime;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            
            // Log để debug
            android.util.Log.d("NotificationAdapter", "VH constructor:");
            android.util.Log.d("NotificationAdapter", "  - itemView: " + itemView);
            android.util.Log.d("NotificationAdapter", "  - tvTitle found: " + (tvTitle != null));
            android.util.Log.d("NotificationAdapter", "  - tvMessage found: " + (tvMessage != null));
            android.util.Log.d("NotificationAdapter", "  - tvTime found: " + (tvTime != null));
            
            if (tvTitle == null || tvMessage == null || tvTime == null) {
                android.util.Log.e("NotificationAdapter", "  - ❌ Some TextView is NULL! Check item_notification.xml layout");
            }
        }

        void bind(NotificationResponse item) {
            if (item == null) {
                android.util.Log.e("NotificationAdapter", "Item is null in bind()");
                return;
            }
            
            String title = item.getTitle() != null ? item.getTitle() : "Thông báo";
            String message = item.getMessage() != null ? item.getMessage() : "";
            String time = formatTime(item.getCreatedAt());
            
            android.util.Log.d("NotificationAdapter", "Binding item: " + title);
            android.util.Log.d("NotificationAdapter", "  - Title text: " + title);
            android.util.Log.d("NotificationAdapter", "  - Message text: " + message);
            android.util.Log.d("NotificationAdapter", "  - Time text: " + time);
            android.util.Log.d("NotificationAdapter", "  - tvTitle is null: " + (tvTitle == null));
            android.util.Log.d("NotificationAdapter", "  - tvMessage is null: " + (tvMessage == null));
            android.util.Log.d("NotificationAdapter", "  - tvTime is null: " + (tvTime == null));
            
            // Đảm bảo TextView không null trước khi set text
            if (tvTitle != null) {
                tvTitle.setText(title);
                android.util.Log.d("NotificationAdapter", "  - tvTitle.setText() called");
            } else {
                android.util.Log.e("NotificationAdapter", "  - tvTitle is NULL!");
            }
            
            if (tvMessage != null) {
                tvMessage.setText(message);
                android.util.Log.d("NotificationAdapter", "  - tvMessage.setText() called");
            } else {
                android.util.Log.e("NotificationAdapter", "  - tvMessage is NULL!");
            }
            
            if (tvTime != null) {
                tvTime.setText(time);
                android.util.Log.d("NotificationAdapter", "  - tvTime.setText() called");
            } else {
                android.util.Log.e("NotificationAdapter", "  - tvTime is NULL!");
            }

            // Dim unread vs read
            float alpha = item.isRead() ? 0.6f : 1f;
            if (tvTitle != null) tvTitle.setAlpha(alpha);
            if (tvMessage != null) tvMessage.setAlpha(alpha);
            if (tvTime != null) tvTime.setAlpha(alpha);
            
            android.util.Log.d("NotificationAdapter", "  - ✅ Bind completed");
        }

        private String formatTime(String isoString) {
            if (isoString == null) return "";
            try {
                // Try parse ISO format
                SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                Date date = iso.parse(isoString);
                if (date == null) return "";
                SimpleDateFormat out = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                return out.format(date);
            } catch (ParseException e) {
                return "";
            }
        }
    }
}

