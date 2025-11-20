package com.example.tunipromos;

import android.content.Intent;
import android.os.Bundle;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private TextInputEditText nameEditText, emailEditText, passwordEditText;
    private RadioGroup roleRadioGroup;
    private RadioButton userRadioButton, providerRadioButton;
    private Button registerButton;
    private TextView loginTextView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        roleRadioGroup = findViewById(R.id.roleRadioGroup);
        userRadioButton = findViewById(R.id.userRadioButton);
        providerRadioButton = findViewById(R.id.providerRadioButton);
        registerButton = findViewById(R.id.registerButton);
        loginTextView = findViewById(R.id.loginTextView);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registerUser();
            }
        });

        loginTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish(); // Retour à LoginActivity
            }
        });
    }

    private void registerUser() {
        final String name = nameEditText.getText().toString();
        final String email = emailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        
        int selectedRoleId = roleRadioGroup.getCheckedRadioButtonId();
        final String role;
        if (selectedRoleId == R.id.providerRadioButton) {
            role = "provider";
        } else {
            role = "user";
        }

        if (TextUtils.isEmpty(name)) {
            nameEditText.setError("Nom requis");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email requis");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Mot de passe requis");
            return;
        }
        if (password.length() < 6) {
            passwordEditText.setError("Le mot de passe doit contenir au moins 6 caractères");
            return;
        }

        registerButton.setEnabled(false);
        Toast.makeText(this, "Inscription en cours...", Toast.LENGTH_SHORT).show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "createUserWithEmail:success");
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            String userId = firebaseUser.getUid();

                            User user = new User(userId, name, email, role);

                            db.collection("users").document(userId).set(user)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            registerButton.setEnabled(true);
                                            if (task.isSuccessful()) {
                                                Toast.makeText(RegisterActivity.this, "Inscription réussie", Toast.LENGTH_SHORT).show();
                                                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                                                finish();
                                            } else {
                                                Toast.makeText(RegisterActivity.this, "Erreur Firestore: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                                            }
                                        }
                                    });

                        } else {
                            registerButton.setEnabled(true);
                            String errorMessage = "Erreur inconnue";
                            try {
                                throw task.getException();
                            } catch(FirebaseAuthWeakPasswordException e) {
                                errorMessage = "Mot de passe trop faible";
                                passwordEditText.setError(errorMessage);
                                passwordEditText.requestFocus();
                            } catch(FirebaseAuthInvalidCredentialsException e) {
                                errorMessage = "Email invalide";
                                emailEditText.setError(errorMessage);
                                emailEditText.requestFocus();
                            } catch(FirebaseAuthUserCollisionException e) {
                                errorMessage = "Cet email existe déjà";
                                emailEditText.setError(errorMessage);
                                emailEditText.requestFocus();
                            } catch(Exception e) {
                                errorMessage = e.getMessage();
                                Log.e(TAG, "Registration failed", e);
                            }
                            
                            Toast.makeText(RegisterActivity.this, "Erreur: " + errorMessage, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}
