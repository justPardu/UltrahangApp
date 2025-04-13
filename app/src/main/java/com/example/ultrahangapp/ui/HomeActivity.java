package com.example.ultrahangapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ultrahangapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private TextView welcomeTextView;
    private TextView loggedInAsTextView;
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        welcomeTextView = findViewById(R.id.welcomeTextView);
        loggedInAsTextView = findViewById(R.id.loggedInAsTextView);
        logoutButton = findViewById(R.id.logoutButton);

        welcomeTextView.setText("Üdvözöllek a főmenüben!");
        if (currentUser != null) {
            loggedInAsTextView.setText("Bejelentkezve: " + currentUser.getEmail());
        }

        logoutButton.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }
}
