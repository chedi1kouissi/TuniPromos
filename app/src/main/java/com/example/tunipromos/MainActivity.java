package com.example.tunipromos;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tunipromos.adapter.PromotionAdapter;
import com.example.tunipromos.model.Promotion;
import com.example.tunipromos.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView welcomeTextView;
    private FloatingActionButton addPromoFab;
    private RecyclerView promotionsRecyclerView;
    private PromotionAdapter promotionAdapter;
    private List<Promotion> promotionList;

    // Lanceur pour demander la permission de notification (Android 13+)
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Toast.makeText(this, "Notifications activées", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Notifications désactivées", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Configuration de la Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        // Vérification connexion
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        welcomeTextView = findViewById(R.id.welcomeTextView);
        addPromoFab = findViewById(R.id.addPromoFab);
        promotionsRecyclerView = findViewById(R.id.promotionsRecyclerView);

        welcomeTextView.setText("Bienvenue, " + currentUser.getEmail());

        // Configuration RecyclerView
        promotionList = new ArrayList<>();
        promotionAdapter = new PromotionAdapter(this, promotionList);
        promotionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        promotionsRecyclerView.setAdapter(promotionAdapter);

        // Vérifier le rôle de l'utilisateur pour afficher/masquer le FAB
        checkUserRole(currentUser.getUid());

        // Charger les promotions
        loadPromotions();

        addPromoFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AddPromotionActivity.class));
            }
        });

        // S'abonner aux notifications
        subscribeToNotifications();
        askNotificationPermission();
    }

    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
                // Permission déjà accordée
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }

    private void subscribeToNotifications() {
        FirebaseMessaging.getInstance().subscribeToTopic("all_promotions")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        String msg = "Abonné aux notifications";
                        if (!task.isSuccessful()) {
                            msg = "Echec abonnement notifications";
                        }
                        Log.d("FCM", msg);
                    }
                });
    }

    private void checkUserRole(String userId) {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        if (documentSnapshot.exists()) {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null && "provider".equals(user.getRole())) {
                                addPromoFab.setVisibility(View.VISIBLE);
                            } else {
                                addPromoFab.setVisibility(View.GONE);
                            }
                        }
                    }
                });
    }

    private void loadPromotions() {
        db.collection("promotions").get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        promotionList.clear();
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Promotion promotion = document.toObject(Promotion.class);
                            promotionList.add(promotion);
                        }
                        promotionAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "Erreur chargement: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_notifications) {
            // Rediriger vers NotificationsActivity
            startActivity(new Intent(MainActivity.this, NotificationsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
