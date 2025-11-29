package com.example.tunipromos;

import android.graphics.Paint;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.example.tunipromos.model.Promotion;
import com.google.android.material.chip.Chip;
import com.google.firebase.firestore.FirebaseFirestore;

public class PromotionDetailsActivity extends AppCompatActivity {

    private ImageView detailImageView;
    private TextView detailTitleTextView, detailPriceAfterTextView, detailPriceBeforeTextView, detailValidityTextView,
            detailDescriptionTextView;
    private Chip detailCategoryChip;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_promotion_details);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        db = FirebaseFirestore.getInstance();

        detailImageView = findViewById(R.id.detailImageView);
        detailTitleTextView = findViewById(R.id.detailTitleTextView);
        detailPriceAfterTextView = findViewById(R.id.detailPriceAfterTextView);
        detailPriceBeforeTextView = findViewById(R.id.detailPriceBeforeTextView);
        detailValidityTextView = findViewById(R.id.detailValidityTextView);
        detailDescriptionTextView = findViewById(R.id.detailDescriptionTextView);
        detailCategoryChip = findViewById(R.id.detailCategoryChip);

        String promoId = getIntent().getStringExtra("PROMOTION_ID");
        if (promoId != null) {
            loadPromotion(promoId);
        }
    }

    private void loadPromotion(String promoId) {
        db.collection("promotions").document(promoId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Promotion promo = documentSnapshot.toObject(Promotion.class);
                        if (promo != null) {
                            displayPromotion(promo);
                        }
                    }
                });
    }

    private void displayPromotion(Promotion promo) {
        detailTitleTextView.setText(promo.getTitle());
        detailDescriptionTextView.setText(promo.getDescription());
        detailPriceAfterTextView.setText(String.format("%.2f DT", promo.getPriceAfter()));
        detailPriceBeforeTextView.setText(String.format("%.2f DT", promo.getPriceBefore()));
        detailPriceBeforeTextView
                .setPaintFlags(detailPriceBeforeTextView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        detailValidityTextView.setText("Valid until " + promo.getEndDate());
        detailCategoryChip.setText(promo.getCategory());

        if (promo.getImageUrl() != null && !promo.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(promo.getImageUrl())
                    .placeholder(R.drawable.ic_launcher_background)
                    .error(android.R.drawable.ic_menu_report_image)
                    .into(detailImageView);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
