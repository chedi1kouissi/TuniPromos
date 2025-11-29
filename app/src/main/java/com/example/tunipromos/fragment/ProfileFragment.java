package com.example.tunipromos.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.tunipromos.FavoritesActivity;
import com.example.tunipromos.LoginActivity;
import com.example.tunipromos.MyPromotionsActivity;
import com.example.tunipromos.NotificationHelper;
import com.example.tunipromos.R;
import com.example.tunipromos.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private TextView userNameTextView, userEmailTextView;
    private TextView myPromotionsButton, providerDashboardButton, notificationPrefsButton, favoritesButton,
            settingsButton;
    private View myPromotionsDivider, providerDashboardDivider;
    private Button logoutButton;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        userNameTextView = view.findViewById(R.id.userNameTextView);
        userEmailTextView = view.findViewById(R.id.userEmailTextView);
        myPromotionsButton = view.findViewById(R.id.myPromotionsButton);
        myPromotionsDivider = view.findViewById(R.id.myPromotionsDivider);
        providerDashboardButton = view.findViewById(R.id.providerDashboardButton);
        providerDashboardDivider = view.findViewById(R.id.providerDashboardDivider);
        notificationPrefsButton = view.findViewById(R.id.notificationPrefsButton);
        favoritesButton = view.findViewById(R.id.favoritesButton);
        settingsButton = view.findViewById(R.id.settingsButton);
        logoutButton = view.findViewById(R.id.logoutButton);

        loadUserProfile();

        logoutButton.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        favoritesButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), FavoritesActivity.class);
            startActivity(intent);
        });

        myPromotionsButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MyPromotionsActivity.class);
            startActivity(intent);
        });

        settingsButton.setOnClickListener(v -> {
            // Test notification
            NotificationHelper.sendTestNotification(getContext());
            Toast.makeText(getContext(), "Test notification sent!", Toast.LENGTH_SHORT).show();
        });

        providerDashboardButton.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.nav_host_fragment, new ProviderDashboardFragment());
            transaction.addToBackStack(null);
            transaction.commit();
        });

        notificationPrefsButton.setOnClickListener(v -> showNotificationPreferencesDialog());

        TextView myNotificationsButton = view.findViewById(R.id.myNotificationsButton);
        myNotificationsButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), com.example.tunipromos.NotificationsActivity.class);
            startActivity(intent);
        });

        return view;
    }

    private void showNotificationPreferencesDialog() {
        String[] categories = getResources().getStringArray(R.array.categories_array);
        boolean[] checkedItems = new boolean[categories.length];
        List<String> selectedCategories = new ArrayList<>();

        // Load saved preferences first
        if (mAuth.getCurrentUser() != null) {
            String userId = mAuth.getCurrentUser().getUid();
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null && user.getNotificationCategories() != null) {
                            selectedCategories.addAll(user.getNotificationCategories());
                            // Pre-check items
                            for (int i = 0; i < categories.length; i++) {
                                if (selectedCategories.contains(categories[i])) {
                                    checkedItems[i] = true;
                                }
                            }
                        }

                        // Show dialog after loading
                        showDialogWithPreferences(categories, checkedItems, selectedCategories);
                    })
                    .addOnFailureListener(e -> {
                        // Show dialog anyway with no pre-selections
                        showDialogWithPreferences(categories, checkedItems, selectedCategories);
                    });
        }
    }

    private void showDialogWithPreferences(String[] categories, boolean[] checkedItems,
            List<String> selectedCategories) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Subscribe to Notifications");
        builder.setMultiChoiceItems(categories, checkedItems, (dialog, which, isChecked) -> {
            if (isChecked) {
                if (!selectedCategories.contains(categories[which])) {
                    selectedCategories.add(categories[which]);
                }
            } else {
                selectedCategories.remove(categories[which]);
            }
        });

        builder.setPositiveButton("Save", (dialog, which) -> {
            // Subscribe/unsubscribe to topics
            for (String category : categories) {
                String topic = category.replaceAll("\\s+", "_");
                if (selectedCategories.contains(category)) {
                    FirebaseMessaging.getInstance().subscribeToTopic(topic);
                } else {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic(topic);
                }
            }

            // Save to Firestore
            if (mAuth.getCurrentUser() != null) {
                String userId = mAuth.getCurrentUser().getUid();
                db.collection("users").document(userId)
                        .update("notificationCategories", selectedCategories)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(getContext(), "Preferences saved successfully", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Error saving preferences", Toast.LENGTH_SHORT).show();
                        });
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void loadUserProfile() {
        if (mAuth.getCurrentUser() == null)
            return;

        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null) {
                            userNameTextView.setText(user.getName());
                            userEmailTextView.setText(user.getEmail());

                            if ("provider".equals(user.getRole())) {
                                myPromotionsButton.setVisibility(View.VISIBLE);
                                myPromotionsDivider.setVisibility(View.VISIBLE);
                                providerDashboardButton.setVisibility(View.VISIBLE);
                                providerDashboardDivider.setVisibility(View.VISIBLE);
                            } else {
                                myPromotionsButton.setVisibility(View.GONE);
                                myPromotionsDivider.setVisibility(View.GONE);
                                providerDashboardButton.setVisibility(View.GONE);
                                providerDashboardDivider.setVisibility(View.GONE);
                            }
                        }
                    }
                });
    }
}
