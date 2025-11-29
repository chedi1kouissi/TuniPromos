package com.example.tunipromos;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tunipromos.adapter.PromotionAdapter;
import com.example.tunipromos.model.Promotion;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FavoritesActivity extends AppCompatActivity {

    private RecyclerView promotionsRecyclerView;
    private TextView emptyStateTextView;
    private ProgressBar loadingProgressBar;
    private PromotionAdapter promotionAdapter;
    private List<Promotion> promotionList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        promotionsRecyclerView = findViewById(R.id.promotionsRecyclerView);
        emptyStateTextView = findViewById(R.id.emptyStateTextView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);

        promotionList = new ArrayList<>();
        promotionAdapter = new PromotionAdapter(this, promotionList);
        promotionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        promotionsRecyclerView.setAdapter(promotionAdapter);

        loadFavorites();
    }

    private void loadFavorites() {
        if (mAuth.getCurrentUser() == null)
            return;

        loadingProgressBar.setVisibility(View.VISIBLE);
        String currentUserId = mAuth.getCurrentUser().getUid();

        // Load all promotions and filter by liked ones
        db.collection("promotions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    loadingProgressBar.setVisibility(View.GONE);
                    promotionList.clear();

                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        Promotion promotion = document.toObject(Promotion.class);
                        if (promotion != null && promotion.getLikes() != null &&
                                promotion.getLikes().contains(currentUserId)) {
                            promotionList.add(promotion);
                        }
                    }

                    if (promotionList.isEmpty()) {
                        emptyStateTextView.setVisibility(View.VISIBLE);
                        promotionsRecyclerView.setVisibility(View.GONE);
                    } else {
                        emptyStateTextView.setVisibility(View.GONE);
                        promotionsRecyclerView.setVisibility(View.VISIBLE);
                        promotionAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(e -> {
                    loadingProgressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading favorites: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
