package com.poly.ban_giay_app.adapter;

import android.content.Intent;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StrikethroughSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.poly.ban_giay_app.ProductDetailActivity;
import com.poly.ban_giay_app.R;
import com.poly.ban_giay_app.models.Product;

import java.util.List;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.VH> {
    private final List<Product> items;

    public ProductAdapter(List<Product> items) {
        this.items = items;
    }

    public static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView name, priceOld, priceNew;

        public VH(@NonNull View itemView) {
            super(itemView);
            img = itemView.findViewById(R.id.imgProduct);
            name = itemView.findViewById(R.id.txtName);
            priceOld = itemView.findViewById(R.id.txtPriceOld);
            priceNew = itemView.findViewById(R.id.txtPriceNew);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        
        // Tính toán width để hiển thị 2 sản phẩm trên màn hình
        ViewGroup.LayoutParams params = v.getLayoutParams();
        if (params != null) {
            int screenWidth = parent.getContext().getResources().getDisplayMetrics().widthPixels;
            int margin = (int) (8 * parent.getContext().getResources().getDisplayMetrics().density * 2); // margin left + right
            int padding = (int) (16 * parent.getContext().getResources().getDisplayMetrics().density); // padding của RecyclerView
            int itemWidth = (screenWidth - padding - margin * 2) / 2; // 2 sản phẩm
            params.width = itemWidth;
            v.setLayoutParams(params);
        }
        
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Product p = items.get(position);
        
        // Load image from URL if available, otherwise use resource
        if (p.imageUrl != null && !p.imageUrl.isEmpty()) {
            // Nếu là URL từ server
            if (p.imageUrl.startsWith("http://") || p.imageUrl.startsWith("https://")) {
                Glide.with(holder.itemView.getContext())
                        .load(p.imageUrl)
                        .placeholder(R.drawable.giaymau) // Placeholder while loading
                        .error(R.drawable.giaymau) // Error image if load fails
                        .into(holder.img);
            } else {
                // Nếu là tên file ảnh (giay15, giay14, etc.), load từ drawable
                int imageResId = getImageResourceId(holder.itemView.getContext(), p.imageUrl);
                if (imageResId != 0) {
                    holder.img.setImageResource(imageResId);
                } else {
                    holder.img.setImageResource(R.drawable.giaymau);
                }
            }
        } else if (p.imageRes != 0) {
            holder.img.setImageResource(p.imageRes);
        } else {
            holder.img.setImageResource(R.drawable.giaymau);
        }
        
        holder.name.setText(p.name);

        if (p.priceOld != null && !p.priceOld.isEmpty()) {
            SpannableString ss = new SpannableString(p.priceOld);
            ss.setSpan(new StrikethroughSpan(), 0, p.priceOld.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            holder.priceOld.setVisibility(View.VISIBLE);
            holder.priceOld.setText(ss);
        } else {
            holder.priceOld.setVisibility(View.GONE);
        }

        if (p.priceNew != null && !p.priceNew.isEmpty()) {
            holder.priceNew.setVisibility(View.VISIBLE);
            holder.priceNew.setText(holder.itemView.getContext().getString(R.string.price_label, p.priceNew));
        } else {
            holder.priceNew.setVisibility(View.GONE);
        }
        
        // Click listener for product image
        holder.img.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ProductDetailActivity.class);
            intent.putExtra("product", p);
            v.getContext().startActivity(intent);
        });
        
        // Click listener for entire item
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), ProductDetailActivity.class);
            intent.putExtra("product", p);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
    
    /**
     * Lấy resource ID từ tên file ảnh (giay15, giay14, etc.)
     */
    private int getImageResourceId(Context context, String imageName) {
        // Loại bỏ extension và path nếu có
        String name = imageName;
        if (name.contains("/")) {
            name = name.substring(name.lastIndexOf("/") + 1);
        }
        if (name.contains(".")) {
            name = name.substring(0, name.lastIndexOf("."));
        }
        
        // Map tên file với resource ID
        return context.getResources().getIdentifier(name, "drawable", context.getPackageName());
    }
}
