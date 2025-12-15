package com.poly.ban_giay_app.adapter;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.poly.ban_giay_app.OrderDetailActivity;
import com.poly.ban_giay_app.R;
import com.poly.ban_giay_app.models.TransactionHistory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TransactionHistoryAdapter extends RecyclerView.Adapter<TransactionHistoryAdapter.ViewHolder> {
    private List<TransactionHistory> transactions = new ArrayList<>();
    private OnTransactionClickListener listener;

    public interface OnTransactionClickListener {
        void onTransactionClick(TransactionHistory transaction);
    }

    public void setOnTransactionClickListener(OnTransactionClickListener listener) {
        this.listener = listener;
    }

    public void setTransactions(List<TransactionHistory> transactions) {
        this.transactions = transactions != null ? transactions : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TransactionHistory transaction = transactions.get(position);
        holder.bind(transaction);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null && transaction.orderId != null && !transaction.orderId.isEmpty()) {
                listener.onTransactionClick(transaction);
            }
        });
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView txtUserName, txtProductName, txtAmount, txtPaymentMethod, txtPaymentDate, txtDeliveryAddress, txtPhoneNumber, txtQuantity;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtUserName = itemView.findViewById(R.id.txtUserName);
            txtProductName = itemView.findViewById(R.id.txtProductName);
            txtAmount = itemView.findViewById(R.id.txtAmount);
            txtPaymentMethod = itemView.findViewById(R.id.txtPaymentMethod);
            txtPaymentDate = itemView.findViewById(R.id.txtPaymentDate);
            txtDeliveryAddress = itemView.findViewById(R.id.txtDeliveryAddress);
            txtPhoneNumber = itemView.findViewById(R.id.txtPhoneNumber);
            txtQuantity = itemView.findViewById(R.id.txtQuantity);
        }

        public void bind(TransactionHistory transaction) {
            txtUserName.setText(transaction.userName != null ? transaction.userName : "N/A");
            txtProductName.setText(transaction.productName != null ? transaction.productName : "N/A");
            txtAmount.setText(formatPrice(transaction.amount));
            txtPaymentMethod.setText(transaction.getPaymentMethodDisplay());
            txtPaymentDate.setText(formatDate(transaction.paymentDate));
            String deliveryAddress = transaction.deliveryAddress != null && !transaction.deliveryAddress.isEmpty() 
                ? transaction.deliveryAddress : "Chưa cập nhật";
            txtDeliveryAddress.setText("Địa chỉ nhận hàng: " + deliveryAddress);
            String phoneNumber = transaction.phoneNumber != null && !transaction.phoneNumber.isEmpty() 
                ? transaction.phoneNumber : "Chưa cập nhật";
            txtPhoneNumber.setText("Số điện thoại: " + phoneNumber);
            int quantity = transaction.quantity > 0 ? transaction.quantity : 1;
            txtQuantity.setText("Số lượng: " + quantity);
        }

        private String formatPrice(long price) {
            return String.format(Locale.getDefault(), "%,d₫", price).replace(",", ".");
        }

        private String formatDate(String dateStr) {
            if (dateStr == null || dateStr.isEmpty()) {
                return "N/A";
            }
            try {
                // Try to parse ISO format
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date date = inputFormat.parse(dateStr);
                if (date == null) {
                    // Try without seconds
                    inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault());
                    date = inputFormat.parse(dateStr);
                }
                if (date != null) {
                    SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    return outputFormat.format(date);
                }
            } catch (ParseException e) {
                // If parsing fails, return as is
            }
            return dateStr;
        }
    }
}

