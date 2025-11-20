package com.example.tunipromos.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.tunipromos.AddPromotionActivity;
import com.example.tunipromos.R;
import com.example.tunipromos.model.Promotion;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class PromotionAdapter extends RecyclerView.Adapter<PromotionAdapter.ViewHolder> {

    private Context context;
    private List<Promotion> promotionList;
    private String currentUserId;

    public PromotionAdapter(Context context, List<Promotion> promotionList) {
        this.context = context;
        this.promotionList = promotionList;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
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
        holder.categoryTextView.setText(promotion.getCategory());
        holder.priceAfterTextView.setText(String.format("%.2f DT", promotion.getPriceAfter()));
        holder.priceBeforeTextView.setText(String.format("%.2f DT", promotion.getPriceBefore()));
        holder.priceBeforeTextView.setPaintFlags(holder.priceBeforeTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        holder.validityTextView.setText("Valide jusqu'au " + promotion.getEndDate());

        if (promotion.getImageUrl() != null && !promotion.getImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(promotion.getImageUrl())
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(holder.promoImageView);
        }

        // Gestion de la visibilité du bouton d'options
        if (currentUserId != null && currentUserId.equals(promotion.getProviderId())) {
            holder.optionsButton.setVisibility(View.VISIBLE);
            holder.optionsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPopupMenu(v, promotion, holder.getAdapterPosition());
                }
            });
        } else {
            holder.optionsButton.setVisibility(View.GONE);
        }
    }

    private void showPopupMenu(View view, final Promotion promotion, final int position) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.getMenuInflater().inflate(R.menu.promotion_options_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.action_edit) {
                    // Lancer l'activité d'édition avec les données de la promo
                    Intent intent = new Intent(context, AddPromotionActivity.class);
                    intent.putExtra("PROMOTION_ID", promotion.getPromoId());
                    // On pourrait passer les autres champs via putExtra, ou recharger depuis Firestore dans l'activité
                    intent.putExtra("IS_EDIT_MODE", true);
                    context.startActivity(intent);
                    return true;
                } else if (id == R.id.action_delete) {
                    deletePromotion(promotion.getPromoId(), position);
                    return true;
                }
                return false;
            }
        });
        popup.show();
    }

    private void deletePromotion(String promoId, final int position) {
        FirebaseFirestore.getInstance().collection("promotions").document(promoId)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        promotionList.remove(position);
                        notifyItemRemoved(position);
                        notifyItemRangeChanged(position, promotionList.size());
                        Toast.makeText(context, "Promotion supprimée", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(context, "Erreur lors de la suppression", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public int getItemCount() {
        return promotionList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView promoImageView;
        ImageButton optionsButton;
        TextView titleTextView, categoryTextView, priceAfterTextView, priceBeforeTextView, validityTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            promoImageView = itemView.findViewById(R.id.promoImageView);
            optionsButton = itemView.findViewById(R.id.optionsButton);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            categoryTextView = itemView.findViewById(R.id.categoryTextView);
            priceAfterTextView = itemView.findViewById(R.id.priceAfterTextView);
            priceBeforeTextView = itemView.findViewById(R.id.priceBeforeTextView);
            validityTextView = itemView.findViewById(R.id.validityTextView);
        }
    }
}
