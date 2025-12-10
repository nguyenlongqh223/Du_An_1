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
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        NotificationResponse item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvTime;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }

        void bind(NotificationResponse item) {
            tvTitle.setText(item.getTitle() != null ? item.getTitle() : "Thông báo");
            tvMessage.setText(item.getMessage() != null ? item.getMessage() : "");
            tvTime.setText(formatTime(item.getCreatedAt()));

            // Dim unread vs read
            float alpha = item.isRead() ? 0.6f : 1f;
            tvTitle.setAlpha(alpha);
            tvMessage.setAlpha(alpha);
            tvTime.setAlpha(alpha);
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

