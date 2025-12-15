package com.poly.ban_giay_app.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.poly.ban_giay_app.R;
import com.poly.ban_giay_app.network.ApiClient;
import com.poly.ban_giay_app.network.ApiService;
import com.poly.ban_giay_app.network.model.BaseResponse;
import com.poly.ban_giay_app.network.model.OrderResponse;
import com.poly.ban_giay_app.network.model.ProductResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.OrderItemViewHolder> {

    private List<OrderResponse.OrderItemResponse> items = new ArrayList<>();

    public void setItems(List<OrderResponse.OrderItemResponse> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_product, parent, false);
        return new OrderItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderItemViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class OrderItemViewHolder extends RecyclerView.ViewHolder {
        private final TextView txtProductName, txtProductSize, txtProductQuantity, txtProductPrice;
        private final ImageView imgProduct;
        private final Context context;
        private final ApiService apiService;

        public OrderItemViewHolder(@NonNull View itemView) {
            super(itemView);
            context = itemView.getContext();
            txtProductName = itemView.findViewById(R.id.txtProductName);
            txtProductSize = itemView.findViewById(R.id.txtProductSize);
            txtProductQuantity = itemView.findViewById(R.id.txtProductQuantity);
            txtProductPrice = itemView.findViewById(R.id.txtProductPrice);
            imgProduct = itemView.findViewById(R.id.imgProduct);
            ApiClient.init(context);
            apiService = ApiClient.getApiService();
        }

        public void bind(OrderResponse.OrderItemResponse item) {
            txtProductName.setText(item.getTenSanPham());
            txtProductSize.setText(context.getString(R.string.size_label, item.getKichThuoc()));
            txtProductQuantity.setText(context.getString(R.string.quantity_label, String.valueOf(item.getSoLuong())));
            if (item.getGia() != null) {
                txtProductPrice.setText(formatPrice(item.getGia()));
            } else {
                txtProductPrice.setText("0₫");
            }
            
            // Load product image
            if (item.getSanPhamId() != null && !item.getSanPhamId().isEmpty()) {
                loadProductImage(item.getSanPhamId());
            } else {
                imgProduct.setImageResource(R.drawable.img);
            }
        }

        private void loadProductImage(String productId) {
            apiService.getProductById(productId).enqueue(new Callback<BaseResponse<ProductResponse>>() {
                @Override
                public void onResponse(Call<BaseResponse<ProductResponse>> call, Response<BaseResponse<ProductResponse>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        BaseResponse<ProductResponse> body = response.body();
                        if (body.getSuccess() && body.getData() != null) {
                            ProductResponse product = body.getData();
                            String imageUrl = product.getImageUrl();
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                Glide.with(context)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.img)
                                    .error(R.drawable.img)
                                    .into(imgProduct);
                            } else {
                                imgProduct.setImageResource(R.drawable.img);
                            }
                        } else {
                            imgProduct.setImageResource(R.drawable.img);
                        }
                    } else {
                        imgProduct.setImageResource(R.drawable.img);
                    }
                }

                @Override
                public void onFailure(Call<BaseResponse<ProductResponse>> call, Throwable t) {
                    imgProduct.setImageResource(R.drawable.img);
                }
            });
        }

        private String formatPrice(long price) {
            return String.format(Locale.getDefault(), "%,d₫", price).replace(",", ".");
        }
    }
}
