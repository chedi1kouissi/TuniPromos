package com.example.tunipromos.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tunipromos.R;
import com.example.tunipromos.adapter.PromotionAdapter;
import com.example.tunipromos.model.Promotion;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class ProviderDashboardFragment extends Fragment {

    private TextView totalPromotionsTextView, totalLikesTextView;
    private RecyclerView dashboardRecyclerView;
    private ProgressBar loadingProgressBar;
    private PromotionAdapter promotionAdapter;
    private List<Promotion> promotionList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_provider_dashboard, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        totalPromotionsTextView = view.findViewById(R.id.totalPromotionsTextView);
        totalLikesTextView = view.findViewById(R.id.totalLikesTextView);
        dashboardRecyclerView = view.findViewById(R.id.dashboardRecyclerView);
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar);

        promotionList = new ArrayList<>();
        promotionAdapter = new PromotionAdapter(getContext(), promotionList);
        dashboardRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        dashboardRecyclerView.setAdapter(promotionAdapter);

        loadDashboardData();

        return view;
    }

    private void loadDashboardData() {
        if (mAuth.getCurrentUser() == null)
            return;

        loadingProgressBar.setVisibility(View.VISIBLE);
        String currentUserId = mAuth.getCurrentUser().getUid();

        db.collection("promotions")
                .whereEqualTo("providerId", currentUserId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    loadingProgressBar.setVisibility(View.GONE);
                    promotionList.clear();
                    int totalLikes = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Promotion promotion = document.toObject(Promotion.class);
                        promotionList.add(promotion);
                        if (promotion.getLikes() != null) {
                            totalLikes += promotion.getLikes().size();
                        }
                    }

                    totalPromotionsTextView.setText(String.valueOf(promotionList.size()));
                    totalLikesTextView.setText(String.valueOf(totalLikes));
                    promotionAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    loadingProgressBar.setVisibility(View.GONE);
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error loading dashboard: " + e.getMessage(), Toast.LENGTH_SHORT)
                                .show();
                    }
                });
    }
}
