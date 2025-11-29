package com.example.tunipromos;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.tunipromos.model.Promotion;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AddPromotionActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private TextView headerTitle, locationTextView;
    private TextInputEditText titleEditText, descriptionEditText, priceBeforeEditText, priceAfterEditText,
            endDateEditText, imageUrlEditText;
    private AutoCompleteTextView categoryAutoComplete;
    private ImageView promoImageView;
    private Button previewImageButton, publishButton, locationButton;
    private ProgressBar progressBar;

    private String existingImageUrl;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;

    private boolean isEditMode = false;
    private String promotionIdToEdit = null;
    private double latitude = 0.0;
    private double longitude = 0.0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_promotion);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        headerTitle = findViewById(R.id.headerTitle);
        titleEditText = findViewById(R.id.titleEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        priceBeforeEditText = findViewById(R.id.priceBeforeEditText);
        priceAfterEditText = findViewById(R.id.priceAfterEditText);
        categoryAutoComplete = findViewById(R.id.categoryAutoComplete);
        endDateEditText = findViewById(R.id.endDateEditText);
        imageUrlEditText = findViewById(R.id.imageUrlEditText);
        promoImageView = findViewById(R.id.promoImageView);
        previewImageButton = findViewById(R.id.previewImageButton);
        publishButton = findViewById(R.id.saveButton);
        progressBar = findViewById(R.id.progressBar);
        locationButton = findViewById(R.id.locationButton);
        locationTextView = findViewById(R.id.locationTextView);

        // Check if edit mode
        if (getIntent().hasExtra("IS_EDIT_MODE") && getIntent().getBooleanExtra("IS_EDIT_MODE", false)) {
            isEditMode = true;
            promotionIdToEdit = getIntent().getStringExtra("PROMOTION_ID");
            headerTitle.setText("Edit Promotion");
            publishButton.setText("Update Promotion");
            loadPromotionData(promotionIdToEdit);
        }

        // Image Preview
        previewImageButton.setOnClickListener(v -> {
            String url = imageUrlEditText.getText().toString().trim();
            if (!TextUtils.isEmpty(url)) {
                Glide.with(AddPromotionActivity.this)
                        .load(url)
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(android.R.drawable.ic_menu_report_image)
                        .into(promoImageView);
            } else {
                Toast.makeText(AddPromotionActivity.this, "Please enter a valid URL", Toast.LENGTH_SHORT).show();
            }
        });

        // Date Picker
        endDateEditText.setOnClickListener(v -> showDatePicker());

        // Location Button
        locationButton.setOnClickListener(v -> checkLocationPermission());

        publishButton.setOnClickListener(v -> {
            if (isEditMode) {
                updatePromotion();
            } else {
                publishPromotion();
            }
        });
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient
                    .getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                                locationTextView.setText(String.format("Lat: %.4f, Lng: %.4f", latitude, longitude));
                                Toast.makeText(AddPromotionActivity.this, "Location fetched", Toast.LENGTH_SHORT)
                                        .show();
                            } else {
                                Toast.makeText(AddPromotionActivity.this, "Location not found. Enable GPS.",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadPromotionData(String promoId) {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("promotions").document(promoId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        Promotion promo = documentSnapshot.toObject(Promotion.class);
                        if (promo != null) {
                            titleEditText.setText(promo.getTitle());
                            descriptionEditText.setText(promo.getDescription());
                            priceBeforeEditText.setText(String.valueOf(promo.getPriceBefore()));
                            priceAfterEditText.setText(String.valueOf(promo.getPriceAfter()));
                            categoryAutoComplete.setText(promo.getCategory(), false);
                            endDateEditText.setText(promo.getEndDate());
                            imageUrlEditText.setText(promo.getImageUrl());
                            existingImageUrl = promo.getImageUrl();
                            latitude = promo.getLatitude();
                            longitude = promo.getLongitude();

                            if (latitude != 0.0 || longitude != 0.0) {
                                locationTextView.setText(String.format("Lat: %.4f, Lng: %.4f", latitude, longitude));
                            }

                            if (existingImageUrl != null && !existingImageUrl.isEmpty()) {
                                Glide.with(AddPromotionActivity.this)
                                        .load(existingImageUrl)
                                        .into(promoImageView);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AddPromotionActivity.this, "Error loading: " + e.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                    finish();
                });
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year1, monthOfYear, dayOfMonth) -> endDateEditText
                        .setText(dayOfMonth + "/" + (monthOfYear + 1) + "/" + year1),
                year, month, day);
        datePickerDialog.show();
    }

    private void updatePromotion() {
        final String title = titleEditText.getText().toString();
        final String description = descriptionEditText.getText().toString();
        final String priceBeforeStr = priceBeforeEditText.getText().toString();
        final String priceAfterStr = priceAfterEditText.getText().toString();
        final String category = categoryAutoComplete.getText().toString();
        final String endDate = endDateEditText.getText().toString();
        final String imageUrl = imageUrlEditText.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(priceBeforeStr) || TextUtils.isEmpty(priceAfterStr)
                || TextUtils.isEmpty(imageUrl)) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        publishButton.setEnabled(false);

        double priceBefore = 0;
        double priceAfter = 0;
        try {
            priceBefore = Double.parseDouble(priceBeforeStr);
            priceAfter = Double.parseDouble(priceAfterStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid price format", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            publishButton.setEnabled(true);
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("description", description);
        updates.put("priceBefore", priceBefore);
        updates.put("priceAfter", priceAfter);
        updates.put("category", category);
        updates.put("endDate", endDate);
        updates.put("imageUrl", imageUrl);
        updates.put("latitude", latitude);
        updates.put("longitude", longitude);

        db.collection("promotions").document(promotionIdToEdit).update(updates)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AddPromotionActivity.this, "Promotion updated", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(AddPromotionActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    publishButton.setEnabled(true);
                    Toast.makeText(AddPromotionActivity.this, "Update error: " + e.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                });
    }

    private void publishPromotion() {
        final String title = titleEditText.getText().toString();
        final String description = descriptionEditText.getText().toString();
        final String priceBeforeStr = priceBeforeEditText.getText().toString();
        final String priceAfterStr = priceAfterEditText.getText().toString();
        final String category = categoryAutoComplete.getText().toString();
        final String endDate = endDateEditText.getText().toString();
        final String imageUrl = imageUrlEditText.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(priceBeforeStr) || TextUtils.isEmpty(priceAfterStr)
                || TextUtils.isEmpty(imageUrl)) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        publishButton.setEnabled(false);

        double priceBefore = 0;
        double priceAfter = 0;
        try {
            priceBefore = Double.parseDouble(priceBeforeStr);
            priceAfter = Double.parseDouble(priceAfterStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid price format", Toast.LENGTH_SHORT).show();
            progressBar.setVisibility(View.GONE);
            publishButton.setEnabled(true);
            return;
        }

        final String promoId = UUID.randomUUID().toString();
        final String providerId = mAuth.getCurrentUser().getUid();
        final String startDate = java.text.DateFormat.getDateInstance().format(new java.util.Date());

        Promotion promotion = new Promotion(promoId, title, description, priceBefore, priceAfter, category, providerId,
                startDate, endDate, imageUrl, latitude, longitude);

        db.collection("promotions").document(promoId).set(promotion)
                .addOnSuccessListener(aVoid -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(AddPromotionActivity.this, "Promotion published!", Toast.LENGTH_SHORT).show();

                    // Send notification to all users
                    NotificationHelper.sendNotificationToTopic("New Promotion!", "New promotion: " + title);

                    startActivity(new Intent(AddPromotionActivity.this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    publishButton.setEnabled(true);
                    Toast.makeText(AddPromotionActivity.this, "Firestore error: " + e.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                });
    }
}
