package com.example.tunipromos;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailEditText, passwordEditText;
    private Button loginButton;
    private TextView registerTextView;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Switch from splash theme to normal theme
        setTheme(R.style.Theme_TuniPromos);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerTextView = findViewById(R.id.registerTextView);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        registerTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });

        // Run Diagnostics immediately on startup
        runSystemCheck();
    }

    private void runSystemCheck() {
        StringBuilder statusReport = new StringBuilder();
        boolean criticalError = false;

        // 1. Internet
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        statusReport.append("Internet: ").append(isConnected ? "OK" : "OFFLINE ❌").append("\n");
        if (!isConnected)
            criticalError = true;

        // 2. Firebase Config
        try {
            FirebaseApp.getInstance();
            statusReport.append("Firebase Config: OK\n");
        } catch (Exception e) {
            statusReport.append("Firebase Config: ERROR ❌ (").append(e.getMessage()).append(")\n");
            criticalError = true;
        }

        // 3. Database Ping
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("test").document("ping").get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Connection Success - Do nothing or show small toast
                        // Toast.makeText(LoginActivity.this, "Connected to Server",
                        // Toast.LENGTH_SHORT).show();
                    } else {
                        // Connection Failed
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown";
                        showErrorDialog("Connection Failed", "Firestore could not be reached.\n\nError: " + error
                                + "\n\n" + statusReport.toString());
                    }
                });

        // If internet is down, show dialog immediately
        if (criticalError) {
            showErrorDialog("System Check Failed", statusReport.toString());
        }
    }

    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Retry", (dialog, which) -> runSystemCheck())
                .setNegativeButton("Close", null)
                .show();
    }

    private void loginUser() {
        String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email requis");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Mot de passe requis");
            return;
        }

        showLoading(true);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Connexion réussie", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        } else {
                            String error = "Erreur de connexion";
                            if (task.getException() != null) {
                                error = task.getException().getMessage();
                            }
                            Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                            showErrorDialog("Login Failed", error);
                        }
                    }
                });
    }

    private void showLoading(boolean isLoading) {
        loginButton.setEnabled(!isLoading);
        if (isLoading) {
            loginButton.setText("Connexion...");
        } else {
            loginButton.setText("Se connecter");
        }
    }
}
