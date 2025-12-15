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
        TextView tvTitle, tvMessage, tvTime, tvProductName, tvCancellationReason;

        VH(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvProductName = itemView.findViewById(R.id.tvProductName);
            tvCancellationReason = itemView.findViewById(R.id.tvCancellationReason);
            
            // Log để debug
            android.util.Log.d("NotificationAdapter", "VH constructor:");
            android.util.Log.d("NotificationAdapter", "  - itemView: " + itemView);
            android.util.Log.d("NotificationAdapter", "  - tvTitle found: " + (tvTitle != null));
            android.util.Log.d("NotificationAdapter", "  - tvMessage found: " + (tvMessage != null));
            android.util.Log.d("NotificationAdapter", "  - tvTime found: " + (tvTime != null));
            android.util.Log.d("NotificationAdapter", "  - tvProductName found: " + (tvProductName != null));
            android.util.Log.d("NotificationAdapter", "  - tvCancellationReason found: " + (tvCancellationReason != null));
            
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

            // Hiển thị tên sản phẩm nếu có
            String productName = item.getTenSanPham();
            android.util.Log.d("NotificationAdapter", "  - Checking product name: " + productName);
            
            // Nếu không có trong field trực tiếp, thử lấy từ metadata
            if ((productName == null || productName.trim().isEmpty()) && item.getMetadata() != null) {
                try {
                    // Metadata có thể là Map hoặc object khác
                    if (item.getMetadata() instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> metadata = (java.util.Map<String, Object>) item.getMetadata();
                        if (metadata.containsKey("ten_san_pham")) {
                            productName = String.valueOf(metadata.get("ten_san_pham"));
                        } else if (metadata.containsKey("product_name")) {
                            productName = String.valueOf(metadata.get("product_name"));
                        } else if (metadata.containsKey("san_pham")) {
                            // Có thể là object
                            Object sanPhamObj = metadata.get("san_pham");
                            if (sanPhamObj instanceof java.util.Map) {
                                @SuppressWarnings("unchecked")
                                java.util.Map<String, Object> sanPham = (java.util.Map<String, Object>) sanPhamObj;
                                if (sanPham.containsKey("ten")) {
                                    productName = String.valueOf(sanPham.get("ten"));
                                } else if (sanPham.containsKey("name")) {
                                    productName = String.valueOf(sanPham.get("name"));
                                }
                            }
                        }
                        android.util.Log.d("NotificationAdapter", "  - Product name from metadata: " + productName);
                    }
                } catch (Exception e) {
                    android.util.Log.e("NotificationAdapter", "  - Error parsing metadata: " + e.getMessage());
                }
            }
            
            if (tvProductName != null) {
                if (productName != null && !productName.trim().isEmpty()) {
                    tvProductName.setText("Sản phẩm: " + productName);
                    tvProductName.setVisibility(View.VISIBLE);
                    android.util.Log.d("NotificationAdapter", "  - ✅ Product name displayed: " + productName);
                } else {
                    tvProductName.setVisibility(View.GONE);
                    android.util.Log.d("NotificationAdapter", "  - ❌ No product name found");
                }
            }

            // Hiển thị lý do hủy đơn nếu có
            String cancellationReason = item.getLyDoHuy();
            android.util.Log.d("NotificationAdapter", "  - Checking cancellation reason: " + cancellationReason);
            
            // Nếu không có trong field trực tiếp, thử lấy từ metadata
            if ((cancellationReason == null || cancellationReason.trim().isEmpty()) && item.getMetadata() != null) {
                try {
                    if (item.getMetadata() instanceof java.util.Map) {
                        @SuppressWarnings("unchecked")
                        java.util.Map<String, Object> metadata = (java.util.Map<String, Object>) item.getMetadata();
                        if (metadata.containsKey("ly_do_huy")) {
                            cancellationReason = String.valueOf(metadata.get("ly_do_huy"));
                        } else if (metadata.containsKey("cancellation_reason")) {
                            cancellationReason = String.valueOf(metadata.get("cancellation_reason"));
                        }
                        android.util.Log.d("NotificationAdapter", "  - Cancellation reason from metadata: " + cancellationReason);
                    }
                } catch (Exception e) {
                    android.util.Log.e("NotificationAdapter", "  - Error parsing metadata for cancellation: " + e.getMessage());
                }
            }
            
            if (tvCancellationReason != null) {
                if (cancellationReason != null && !cancellationReason.trim().isEmpty()) {
                    tvCancellationReason.setText("Lý do hủy: " + cancellationReason);
                    tvCancellationReason.setVisibility(View.VISIBLE);
                    android.util.Log.d("NotificationAdapter", "  - ✅ Cancellation reason displayed: " + cancellationReason);
                } else {
                    tvCancellationReason.setVisibility(View.GONE);
                    android.util.Log.d("NotificationAdapter", "  - ❌ No cancellation reason found");
                }
            }

            // Dim unread vs read
            float alpha = item.isRead() ? 0.6f : 1f;
            if (tvTitle != null) tvTitle.setAlpha(alpha);
            if (tvMessage != null) tvMessage.setAlpha(alpha);
            if (tvTime != null) tvTime.setAlpha(alpha);
            if (tvProductName != null) tvProductName.setAlpha(alpha);
            if (tvCancellationReason != null) tvCancellationReason.setAlpha(alpha);
            
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

