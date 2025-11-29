package com.example.tunipromos.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tunipromos.AddPromotionActivity;
import com.example.tunipromos.R;
import com.example.tunipromos.adapter.PromotionAdapter;
import com.example.tunipromos.model.Promotion;
import com.example.tunipromos.model.User;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1002;
    private static final float MAX_DISTANCE_METERS = 10000; // 10 km

    private RecyclerView promotionsRecyclerView;
    private PromotionAdapter promotionAdapter;
    private List<Promotion> promotionList;
    private List<Promotion> allPromotions; // Store all loaded promotions
    private ExtendedFloatingActionButton addPromoFab;
    private ChipGroup filterChipGroup;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        promotionsRecyclerView = view.findViewById(R.id.promotionsRecyclerView);
        addPromoFab = view.findViewById(R.id.addPromoFab);
        filterChipGroup = view.findViewById(R.id.filterChipGroup);

        promotionList = new ArrayList<>();
        allPromotions = new ArrayList<>();
        promotionAdapter = new PromotionAdapter(getContext(), promotionList);
        promotionsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        promotionsRecyclerView.setAdapter(promotionAdapter);

        addPromoFab.setOnClickListener(v -> {
            startActivity(new Intent(getContext(), AddPromotionActivity.class));
        });

        filterChipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.nearbyChip)) {
                checkLocationPermission();
            } else {
                // Reset to all promotions
                promotionList.clear();
                promotionList.addAll(allPromotions);
                promotionAdapter.notifyDataSetChanged();
            }
        });

        checkUserRole();
        loadPromotions();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Reload if not filtering
        if (filterChipGroup.getCheckedChipId() == View.NO_ID) {
            loadPromotions();
        }
    }

    private void checkUserRole() {
        if (mAuth.getCurrentUser() == null)
            return;

        db.collection("users").document(mAuth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null && "provider".equals(user.getRole())) {
                            addPromoFab.setVisibility(View.VISIBLE);
                        } else {
                            addPromoFab.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void loadPromotions() {
        db.collection("promotions").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    allPromotions.clear();
                    promotionList.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Promotion promotion = document.toObject(Promotion.class);
                        allPromotions.add(promotion);
                        promotionList.add(promotion);
                    }
                    promotionAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Error loading promotions", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            filterPromotionsByLocation();
        }
    }

    private void filterPromotionsByLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient
                    .getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(requireActivity(), location -> {
                        if (location != null) {
                            filterListByDistance(location);
                        } else {
                            Toast.makeText(getContext(), "Location not found", Toast.LENGTH_SHORT).show();
                            filterChipGroup.clearCheck(); // Reset filter
                        }
                    });
        }
    }

    private void filterListByDistance(Location userLocation) {
        List<Promotion> filteredList = new ArrayList<>();
        for (Promotion promo : allPromotions) {
            if (promo.getLatitude() != 0.0 || promo.getLongitude() != 0.0) {
                float[] results = new float[1];
                Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                        promo.getLatitude(), promo.getLongitude(), results);
                if (results[0] <= MAX_DISTANCE_METERS) {
                    filteredList.add(promo);
                }
            }
        }

        promotionList.clear();
        if (filteredList.isEmpty()) {
            Toast.makeText(getContext(), "No promotions nearby", Toast.LENGTH_SHORT).show();
        } else {
            promotionList.addAll(filteredList);
        }
        promotionAdapter.notifyDataSetChanged();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                filterPromotionsByLocation();
            } else {
                Toast.makeText(getContext(), "Permission denied", Toast.LENGTH_SHORT).show();
                filterChipGroup.clearCheck();
            }
        }
    }
}
