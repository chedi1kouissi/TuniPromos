package com.example.tunipromos;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.tunipromos.adapter.NotificationAdapter;
import com.example.tunipromos.model.NotificationModel;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private RecyclerView notificationsRecyclerView;
    private NotificationAdapter adapter;
    private List<NotificationModel> allNotifications;
    private List<NotificationModel> displayedNotifications;
    private ChipGroup filterChipGroup;
    private FirebaseFirestore db;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView);
        filterChipGroup = findViewById(R.id.filterChipGroup);

        allNotifications = new ArrayList<>();
        displayedNotifications = new ArrayList<>();
        adapter = new NotificationAdapter(this, displayedNotifications);

        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationsRecyclerView.setAdapter(adapter);

        loadNotifications();

        filterChipGroup.setOnCheckedChangeListener(new ChipGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(ChipGroup group, int checkedId) {
                filterNotifications(checkedId);
            }
        });
    }

    private void loadNotifications() {
        // On suppose que les notifs sont stockées dans une collection 'user_notifications'
        // ou une sous-collection de l'user. Pour simplifier, disons une collection racine avec l'ID user.
        db.collection("users").document(userId).collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                        if (error != null) return;

                        allNotifications.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            NotificationModel notification = doc.toObject(NotificationModel.class);
                            allNotifications.add(notification);
                        }
                        // Appliquer le filtre actuel
                        filterNotifications(filterChipGroup.getCheckedChipId());
                    }
                });
    }

    private void filterNotifications(int checkedId) {
        displayedNotifications.clear();
        if (checkedId == R.id.filterTodayChip) {
            for (NotificationModel notif : allNotifications) {
                if (notif.getTimestamp() != null && DateUtils.isToday(notif.getTimestamp().toDate().getTime())) {
                    displayedNotifications.add(notif);
                }
            }
        } else {
            // Default : TOUT
            displayedNotifications.addAll(allNotifications);
        }
        adapter.notifyDataSetChanged();
    }
}
