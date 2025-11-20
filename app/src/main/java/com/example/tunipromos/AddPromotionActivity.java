package com.example.tunipromos;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.tunipromos.model.Promotion;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AddPromotionActivity extends AppCompatActivity {

    private TextView headerTitle;
    private TextInputEditText titleEditText, descriptionEditText, priceBeforeEditText, priceAfterEditText, categoryEditText, endDateEditText;
    private ImageView promoImageView;
    private Button addImageButton, publishButton;
    private ProgressBar progressBar;

    private Uri imageUri;
    private String existingImageUrl;
    private FirebaseFirestore db;
    private StorageReference storageReference;
    private FirebaseAuth mAuth;

    private ActivityResultLauncher<String> mGetContent;

    private boolean isEditMode = false;
    private String promotionIdToEdit = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_promotion);

        db = FirebaseFirestore.getInstance();
        storageReference = FirebaseStorage.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        headerTitle = findViewById(R.id.headerTitle);
        titleEditText = findViewById(R.id.titleEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        priceBeforeEditText = findViewById(R.id.priceBeforeEditText);
        priceAfterEditText = findViewById(R.id.priceAfterEditText);
        categoryEditText = findViewById(R.id.categoryEditText);
        endDateEditText = findViewById(R.id.endDateEditText);
        promoImageView = findViewById(R.id.promoImageView);
        addImageButton = findViewById(R.id.addImageButton);
        publishButton = findViewById(R.id.publishButton);
        progressBar = findViewById(R.id.progressBar);

        // Vérifier si nous sommes en mode édition
        if (getIntent().hasExtra("IS_EDIT_MODE") && getIntent().getBooleanExtra("IS_EDIT_MODE", false)) {
            isEditMode = true;
            promotionIdToEdit = getIntent().getStringExtra("PROMOTION_ID");
            headerTitle.setText("Modifier la promotion");
            publishButton.setText("Mettre à jour");
            loadPromotionData(promotionIdToEdit);
        } else {
            headerTitle = findViewById(R.id.headerTitle); // Assure initialization if not edit mode (though already done)
            // headerTitle is technically initialized above, findViewById call is redundant but safe.
            // Keeping it clean:
            // headerTitle.setText("Nouvelle Promotion"); // Default in XML
        }

        // Sélecteur d'image
        mGetContent = registerForActivityResult(new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri != null) {
                            imageUri = uri;
                            promoImageView.setImageURI(uri);
                        }
                    }
                });

        addImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mGetContent.launch("image/*");
            }
        });

        // Sélecteur de date
        endDateEditText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        publishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isEditMode) {
                    updatePromotion();
                } else {
                    publishPromotion();
                }
            }
        });
    }

    private void loadPromotionData(String promoId) {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("promotions").document(promoId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        progressBar.setVisibility(View.GONE);
                        if (documentSnapshot.exists()) {
                            Promotion promo = documentSnapshot.toObject(Promotion.class);
                            if (promo != null) {
                                titleEditText.setText(promo.getTitle());
                                descriptionEditText.setText(promo.getDescription());
                                priceBeforeEditText.setText(String.valueOf(promo.getPriceBefore()));
                                priceAfterEditText.setText(String.valueOf(promo.getPriceAfter()));
                                categoryEditText.setText(promo.getCategory());
                                endDateEditText.setText(promo.getEndDate());
                                existingImageUrl = promo.getImageUrl();

                                if (existingImageUrl != null && !existingImageUrl.isEmpty()) {
                                    Glide.with(AddPromotionActivity.this)
                                            .load(existingImageUrl)
                                            .into(promoImageView);
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AddPromotionActivity.this, "Erreur chargement: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void showDatePicker() {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        endDateEditText.setText(dayOfMonth + "/" + (monthOfYear + 1) + "/" + year);
                    }
                }, year, month, day);
        datePickerDialog.show();
    }

    private void updatePromotion() {
        final String title = titleEditText.getText().toString();
        final String description = descriptionEditText.getText().toString();
        final String priceBeforeStr = priceBeforeEditText.getText().toString();
        final String priceAfterStr = priceAfterEditText.getText().toString();
        final String category = categoryEditText.getText().toString();
        final String endDate = endDateEditText.getText().toString();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(priceBeforeStr) || TextUtils.isEmpty(priceAfterStr)) {
            Toast.makeText(this, "Veuillez remplir les champs obligatoires", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        publishButton.setEnabled(false);

        final double priceBefore = Double.parseDouble(priceBeforeStr);
        final double priceAfter = Double.parseDouble(priceAfterStr);

        if (imageUri != null) {
            // Nouvelle image sélectionnée, on l'upload d'abord
            final StorageReference imageRef = storageReference.child("promo_images/" + promotionIdToEdit + ".jpg");
            imageRef.putFile(imageUri)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    updateFirestoreData(title, description, priceBefore, priceAfter, category, endDate, uri.toString());
                                }
                            });
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressBar.setVisibility(View.GONE);
                            publishButton.setEnabled(true);
                            Toast.makeText(AddPromotionActivity.this, "Erreur upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            // Pas de nouvelle image, on garde l'ancienne
            updateFirestoreData(title, description, priceBefore, priceAfter, category, endDate, existingImageUrl);
        }
    }

    private void updateFirestoreData(String title, String description, double priceBefore, double priceAfter, String category, String endDate, String imageUrl) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("title", title);
        updates.put("description", description);
        updates.put("priceBefore", priceBefore);
        updates.put("priceAfter", priceAfter);
        updates.put("category", category);
        updates.put("endDate", endDate);
        updates.put("imageUrl", imageUrl);

        db.collection("promotions").document(promotionIdToEdit).update(updates)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(AddPromotionActivity.this, "Promotion mise à jour", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(AddPromotionActivity.this, MainActivity.class)); // Refresh main list
                        finish();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressBar.setVisibility(View.GONE);
                        publishButton.setEnabled(true);
                        Toast.makeText(AddPromotionActivity.this, "Erreur update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void publishPromotion() {
        final String title = titleEditText.getText().toString();
        final String description = descriptionEditText.getText().toString();
        final String priceBeforeStr = priceBeforeEditText.getText().toString();
        final String priceAfterStr = priceAfterEditText.getText().toString();
        final String category = categoryEditText.getText().toString();
        final String endDate = endDateEditText.getText().toString();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(priceBeforeStr) || TextUtils.isEmpty(priceAfterStr) || imageUri == null) {
            Toast.makeText(this, "Veuillez remplir tous les champs obligatoires et ajouter une image", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        publishButton.setEnabled(false);

        final double priceBefore = Double.parseDouble(priceBeforeStr);
        final double priceAfter = Double.parseDouble(priceAfterStr);
        final String promoId = UUID.randomUUID().toString();
        final String providerId = mAuth.getCurrentUser().getUid();

        final StorageReference imageRef = storageReference.child("promo_images/" + promoId + ".jpg");

        imageRef.putFile(imageUri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        imageRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                String imageUrl = uri.toString();
                                String startDate = java.text.DateFormat.getDateInstance().format(new java.util.Date());

                                Promotion promotion = new Promotion(promoId, title, description, priceBefore, priceAfter, category, providerId, startDate, endDate, imageUrl);

                                db.collection("promotions").document(promoId).set(promotion)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {
                                                progressBar.setVisibility(View.GONE);
                                                Toast.makeText(AddPromotionActivity.this, "Promotion publiée avec succès", Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(AddPromotionActivity.this, MainActivity.class));
                                                finish();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                progressBar.setVisibility(View.GONE);
                                                publishButton.setEnabled(true);
                                                Toast.makeText(AddPromotionActivity.this, "Erreur Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressBar.setVisibility(View.GONE);
                        publishButton.setEnabled(true);
                        Toast.makeText(AddPromotionActivity.this, "Erreur upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
