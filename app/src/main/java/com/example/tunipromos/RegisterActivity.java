package com.example.tunipromos;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tunipromos.model.User;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    // UI Components
    private TextInputEditText nameEditText, emailEditText, passwordEditText;
    private RadioGroup roleRadioGroup;
    private Button registerButton;
    private TextView loginTextView;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Check Google Play Services
        checkPlayServices();

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupListeners();
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, 9000).show();
            } else {
                Log.e(TAG, "This device is not supported.");
                Toast.makeText(this, "This device is not supported for Firebase.", Toast.LENGTH_LONG).show();
                finish();
            }
            return false;
        }
        return true;
    }

    private void initViews() {
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        roleRadioGroup = findViewById(R.id.roleRadioGroup);
        registerButton = findViewById(R.id.registerButton);
        loginTextView = findViewById(R.id.loginTextView);
    }

    private void setupListeners() {
        registerButton.setOnClickListener(v -> attemptRegistration());
        loginTextView.setOnClickListener(v -> finish());
    }

    private void attemptRegistration() {
        nameEditText.setError(null);
        emailEditText.setError(null);
        passwordEditText.setError(null);

        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        
        String role = "user";
        if (roleRadioGroup.getCheckedRadioButtonId() == R.id.providerRadioButton) {
            role = "provider";
        }

        // Quick validation
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        performFirebaseRegistration(name, email, password, role);
    }

    private void performFirebaseRegistration(String name, String email, String password, String role) {
        showLoading(true);
        Log.d(TAG, "Starting registration...");

        // Add a safety timeout
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable timeoutRunnable = new Runnable() {
            @Override
            public void run() {
                if (registerButton.getText().equals("Chargement...")) {
                    showLoading(false);
                    Toast.makeText(RegisterActivity.this, "Timeout: Le serveur ne répond pas. Vérifiez votre connexion ou Google Play Services.", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Registration Timed Out");
                }
            }
        };
        handler.postDelayed(timeoutRunnable, 15000); // 15 seconds timeout

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        handler.removeCallbacks(timeoutRunnable); // Cancel timeout
                        
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Auth Success");
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            saveUserToFirestore(firebaseUser, name, email, role);
                        } else {
                            showLoading(false);
                            Log.e(TAG, "Auth Failed", task.getException());
                            String msg = task.getException() != null ? task.getException().getMessage() : "Unknown Error";
                            Toast.makeText(RegisterActivity.this, "Erreur Auth: " + msg, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void saveUserToFirestore(FirebaseUser firebaseUser, String name, String email, String role) {
        if (firebaseUser == null) return;

        User user = new User(firebaseUser.getUid(), name, email, role);

        db.collection("users").document(firebaseUser.getUid())
                .set(user)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Firestore Success");
                            Toast.makeText(RegisterActivity.this, "Compte créé !", Toast.LENGTH_SHORT).show();
                            navigateToMain();
                        } else {
                            Log.e(TAG, "Firestore Failed", task.getException());
                            // Proceed anyway since Auth worked
                            navigateToMain(); 
                        }
                    }
                });
    }

    private void navigateToMain() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLoading(boolean isLoading) {
        registerButton.setEnabled(!isLoading);
        if (isLoading) {
            registerButton.setText("Chargement...");
        } else {
            registerButton.setText("S'inscrire");
        }
    }
}
