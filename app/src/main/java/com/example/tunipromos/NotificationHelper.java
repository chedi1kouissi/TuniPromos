package com.example.tunipromos;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.example.tunipromos.model.NotificationModel;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class NotificationHelper {

        private static final String TAG = "NotificationHelper";

        public static void sendTestNotification(Context context) {
                Log.d(TAG, "sendTestNotification called");

                // Save to Firestore
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                if (mAuth.getCurrentUser() == null) {
                        Log.e(TAG, "No user logged in!");
                        Toast.makeText(context, "Error: Not logged in", Toast.LENGTH_LONG).show();
                        return;
                }

                String userId = mAuth.getCurrentUser().getUid();
                Log.d(TAG, "User ID: " + userId);

                NotificationModel notification = new NotificationModel(
                                "Test Notification",
                                "This is a test notification created at " + new java.util.Date(),
                                Timestamp.now());

                FirebaseFirestore.getInstance()
                                .collection("users").document(userId).collection("notifications")
                                .add(notification)
                                .addOnSuccessListener(documentReference -> {
                                        Log.d(TAG, "Notification saved to Firestore with ID: "
                                                        + documentReference.getId());
                                        Toast.makeText(context, "Notification saved! ID: " + documentReference.getId(),
                                                        Toast.LENGTH_LONG).show();
                                })
                                .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error saving notification to Firestore", e);
                                        Toast.makeText(context, "Error saving notification: " + e.getMessage(),
                                                        Toast.LENGTH_LONG).show();
                                });

                // Show local notification
                showLocalNotification(context);
        }

        public static void saveNotification(Context context, String title, String message) {
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                if (mAuth.getCurrentUser() != null) {
                        String userId = mAuth.getCurrentUser().getUid();
                        NotificationModel notification = new NotificationModel(
                                        title, message, Timestamp.now());
                        FirebaseFirestore.getInstance()
                                        .collection("users").document(userId).collection("notifications")
                                        .add(notification)
                                        .addOnFailureListener(e -> Log.e(TAG, "Error saving notification", e));
                }
        }

        private static void showLocalNotification(Context context) {
                Log.d(TAG, "showLocalNotification called");

                NotificationManager notificationManager = (NotificationManager) context
                                .getSystemService(Context.NOTIFICATION_SERVICE);
                String channelId = "test_channel";

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        NotificationChannel channel = new NotificationChannel(
                                        channelId,
                                        "Test Notifications",
                                        NotificationManager.IMPORTANCE_HIGH);
                        channel.setDescription("Test notifications for TuniPromos");
                        notificationManager.createNotificationChannel(channel);
                        Log.d(TAG, "Notification channel created");
                }

                NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                                .setSmallIcon(R.mipmap.ic_launcher_round)
                                .setContentTitle("Test Notification")
                                .setContentText("Test notification created at " + new java.util.Date())
                                .setAutoCancel(true)
                                .setPriority(NotificationCompat.PRIORITY_HIGH);

                notificationManager.notify((int) System.currentTimeMillis(), builder.build());
                Log.d(TAG, "Local notification displayed");
        }

        public static void sendNotificationToTopic(String title, String message) {
                new Thread(() -> {
                        try {
                                // REPLACE "YOUR_SERVER_KEY_HERE" WITH YOUR ACTUAL FIREBASE SERVER KEY
                                // Go to Firebase Console -> Project Settings -> Cloud Messaging -> Cloud
                                // Messaging API (Legacy)
                                String serverKey = "YOUR_SERVER_KEY_HERE";
                                String topic = "/topics/all_promotions";

                                java.net.URL url = new java.net.URL("https://fcm.googleapis.com/fcm/send");
                                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                                conn.setUseCaches(false);
                                conn.setDoInput(true);
                                conn.setDoOutput(true);
                                conn.setRequestMethod("POST");
                                conn.setRequestProperty("Authorization", "key=" + serverKey);
                                conn.setRequestProperty("Content-Type", "application/json");

                                org.json.JSONObject json = new org.json.JSONObject();
                                json.put("to", topic);

                                org.json.JSONObject info = new org.json.JSONObject();
                                info.put("title", title);
                                info.put("body", message);

                                // Send as data-only message to ensure onMessageReceived is triggered
                                // even when the app is in the background.
                                json.put("data", info);

                                java.io.OutputStreamWriter wr = new java.io.OutputStreamWriter(conn.getOutputStream());
                                wr.write(json.toString());
                                wr.flush();

                                int status = conn.getResponseCode();
                                Log.d(TAG, "Notification sent status: " + status);

                        } catch (Exception e) {
                                Log.e(TAG, "Error sending notification", e);
                        }
                }).start();
        }
}
