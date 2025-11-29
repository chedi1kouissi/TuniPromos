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

import com.example.tunipromos.adapter.NotificationAdapter;
import com.example.tunipromos.model.NotificationModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView notificationsRecyclerView;
    private TextView emptyStateTextView;
    private ProgressBar loadingProgressBar;
    private NotificationAdapter notificationAdapter;
    private List<NotificationModel> notificationList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView);
        emptyStateTextView = findViewById(R.id.emptyStateTextView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);

        notificationList = new ArrayList<>();
        notificationAdapter = new NotificationAdapter(this, notificationList);
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationsRecyclerView.setAdapter(notificationAdapter);

        loadNotifications();
    }

    private void loadNotifications() {
        if (mAuth.getCurrentUser() == null) {
            android.util.Log.e("NotificationsActivity", "No user logged in");
            return;
        }

        loadingProgressBar.setVisibility(View.VISIBLE);
        String userId = mAuth.getCurrentUser().getUid();
        android.util.Log.d("NotificationsActivity", "Loading notifications for user: " + userId);

        db.collection("users").document(userId).collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    loadingProgressBar.setVisibility(View.GONE);
                    notificationList.clear();

                    android.util.Log.d("NotificationsActivity",
                            "Found " + queryDocumentSnapshots.size() + " notifications");

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        NotificationModel notification = document.toObject(NotificationModel.class);
                        android.util.Log.d("NotificationsActivity", "Notification: " + notification.getTitle());
                        notificationList.add(notification);
                    }

                    if (notificationList.isEmpty()) {
                        emptyStateTextView.setVisibility(View.VISIBLE);
                        notificationsRecyclerView.setVisibility(View.GONE);
                        android.util.Log.d("NotificationsActivity", "No notifications to display");
                    } else {
                        emptyStateTextView.setVisibility(View.GONE);
                        notificationsRecyclerView.setVisibility(View.VISIBLE);
                        notificationAdapter.notifyDataSetChanged();
                        android.util.Log.d("NotificationsActivity",
                                "Displaying " + notificationList.size() + " notifications");
                    }
                })
                .addOnFailureListener(e -> {
                    loadingProgressBar.setVisibility(View.GONE);
                    android.util.Log.e("NotificationsActivity", "Error loading notifications", e);
                    Toast.makeText(this, "Error loading notifications: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.notifications_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else if (item.getItemId() == R.id.action_refresh) {
            android.util.Log.d("NotificationsActivity", "Refresh clicked");
            loadNotifications();
            Toast.makeText(this, "Refreshing...", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
