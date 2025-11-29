package com.example.tunipromos.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tunipromos.AddPromotionActivity;
import com.example.tunipromos.PromotionDetailsActivity;
import com.example.tunipromos.R;
import com.example.tunipromos.model.Promotion;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class PromotionAdapter extends RecyclerView.Adapter<PromotionAdapter.ViewHolder> {

    private Context context;
    private List<Promotion> promotionList;
    private String currentUserId;

    public PromotionAdapter(Context context, List<Promotion> promotionList) {
        this.context = context;
        this.promotionList = promotionList;
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_promotion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Promotion promotion = promotionList.get(position);

        holder.titleTextView.setText(promotion.getTitle());
        holder.categoryChip.setText(promotion.getCategory());
        holder.priceAfterTextView.setText(String.format("%.2f DT", promotion.getPriceAfter()));
        holder.priceBeforeTextView.setText(String.format("%.2f DT", promotion.getPriceBefore()));
        holder.priceBeforeTextView
                .setPaintFlags(holder.priceBeforeTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        holder.validityTextView.setText("Valid until " + promotion.getEndDate());

        if (promotion.getImageUrl() != null && !promotion.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(promotion.getImageUrl())
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(holder.promoImageView);
        }

        // Likes Logic
        List<String> likes = promotion.getLikes();
        if (likes == null) {
            likes = new ArrayList<>();
            promotion.setLikes(likes);
        }
        holder.likeCountTextView.setText(String.valueOf(likes.size()));

        if (currentUserId != null) {
            holder.likeButton.setChecked(likes.contains(currentUserId));
        } else {
            holder.likeButton.setChecked(false);
        }

        holder.likeButton.setOnClickListener(v -> {
            if (currentUserId == null) {
                Toast.makeText(context, "Please login to like", Toast.LENGTH_SHORT).show();
                holder.likeButton.setChecked(false);
                return;
            }

            boolean isLiked = holder.likeButton.isChecked();
            FirebaseFirestore db = FirebaseFirestore.getInstance();

            if (isLiked) {
                db.collection("promotions").document(promotion.getPromoId())
                        .update("likes", FieldValue.arrayUnion(currentUserId));
                promotion.getLikes().add(currentUserId);
            } else {
                db.collection("promotions").document(promotion.getPromoId())
                        .update("likes", FieldValue.arrayRemove(currentUserId));
                promotion.getLikes().remove(currentUserId);
            }
            holder.likeCountTextView.setText(String.valueOf(promotion.getLikes().size()));
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, PromotionDetailsActivity.class);
            intent.putExtra("PROMOTION_ID", promotion.getPromoId());
            context.startActivity(intent);
        });

        // Options button logic
        if (currentUserId != null && currentUserId.equals(promotion.getProviderId())) {
            holder.optionsButton.setVisibility(View.VISIBLE);
            holder.optionsButton.setOnClickListener(v -> showPopupMenu(v, promotion, holder.getAdapterPosition()));
        } else {
            holder.optionsButton.setVisibility(View.GONE);
        }
    }

    private void showPopupMenu(View view, final Promotion promotion, final int position) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.getMenuInflater().inflate(R.menu.promotion_options_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_edit) {
                Intent intent = new Intent(context, AddPromotionActivity.class);
                intent.putExtra("PROMOTION_ID", promotion.getPromoId());
                intent.putExtra("IS_EDIT_MODE", true);
                context.startActivity(intent);
                return true;
            } else if (id == R.id.action_delete) {
                deletePromotion(promotion.getPromoId(), position);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void deletePromotion(String promoId, final int position) {
        FirebaseFirestore.getInstance().collection("promotions").document(promoId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    promotionList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, promotionList.size());
                    Toast.makeText(context, "Promotion deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(context, "Error deleting", Toast.LENGTH_SHORT).show());
    }

    @Override
    public int getItemCount() {
        return promotionList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView promoImageView;
        ImageButton optionsButton;
        TextView titleTextView, priceAfterTextView, priceBeforeTextView, validityTextView, likeCountTextView;
        com.google.android.material.chip.Chip categoryChip;
        ToggleButton likeButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            promoImageView = itemView.findViewById(R.id.promoImageView);
            optionsButton = itemView.findViewById(R.id.optionsButton);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            categoryChip = itemView.findViewById(R.id.categoryChip);
            priceAfterTextView = itemView.findViewById(R.id.priceAfterTextView);
            priceBeforeTextView = itemView.findViewById(R.id.priceBeforeTextView);
            validityTextView = itemView.findViewById(R.id.validityTextView);
            likeButton = itemView.findViewById(R.id.likeButton);
            likeCountTextView = itemView.findViewById(R.id.likeCountTextView);
        }
    }
}
