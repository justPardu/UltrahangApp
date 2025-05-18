package com.example.ultrahangapp.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ultrahangapp.R;
import com.example.ultrahangapp.adapter.BookingAdapter;
import com.example.ultrahangapp.model.Booking;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private RecyclerView bookingRecyclerView;
    private Button filterButton;
    private TextView emptyView;

    // Szűrési állapot
    private boolean onlyFuture = true;
    private boolean orderAsc = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        bookingRecyclerView = findViewById(R.id.bookingListRecyclerView);
        filterButton = findViewById(R.id.filterButton);
        emptyView = findViewById(R.id.emptyView);

        bookingRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Főmenü");
        toolbar.setTitleTextColor(Color.WHITE);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        // Drawer header - email megjelenítése
        if (currentUser != null) {
            TextView navHeaderEmail = navigationView.getHeaderView(0).findViewById(R.id.navHeaderEmail);
            navHeaderEmail.setText("Bejelentkezve: " + currentUser.getEmail());
        }

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_booking) {
                startActivity(new Intent(this, BookingActivity.class));
            } else if (id == R.id.nav_logout) {
                auth.signOut();
                startActivity(new Intent(this, MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                finish();
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        filterButton.setOnClickListener(v -> showFilterDialog());
        loadBookings();
    }
    @Override
    protected void onResume() {
        super.onResume();
        loadBookings();
    }
    private void showFilterDialog() {
        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle("Szűrők");

        // Dinamikus layout
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        int pad = (int)(16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);

        CheckBox cbFuture = new CheckBox(this);
        cbFuture.setText("Csak jövőbeli időpontok");
        cbFuture.setChecked(onlyFuture);
        layout.addView(cbFuture);

        TextView tv = new TextView(this);
        tv.setText("Dátum szerinti rendezés:");
        layout.addView(tv);

        RadioGroup rg = new RadioGroup(this);
        RadioButton rAsc = new RadioButton(this);
        rAsc.setText("Növekvő");
        RadioButton rDesc = new RadioButton(this);
        rDesc.setText("Csökkenő");
        rg.addView(rAsc);
        rg.addView(rDesc);
        if (orderAsc) rAsc.setChecked(true);
        else rDesc.setChecked(true);
        layout.addView(rg);

        b.setView(layout)
                .setPositiveButton("OK", (dialog, which) -> {
                    onlyFuture = cbFuture.isChecked();
                    orderAsc = rAsc.isChecked();
                    loadBookings();
                })
                .setNegativeButton("Mégse", null)
                .show();
    }

    private void loadBookings() {
        if (currentUser == null) return;
        String userEmail = currentUser.getEmail();

        FirebaseFirestore.getInstance()
                .collection("bookings")
                .whereEqualTo("patientName", userEmail)
                .get()
                .addOnSuccessListener(snap -> {
                    List<Booking> list = new ArrayList<>();
                    String today = getTodayString();
                    for (DocumentSnapshot doc : snap) {
                        Booking b = doc.toObject(Booking.class);
                        b.setId(doc.getId());
                        // Csak jövőbeli időpontok
                        if (onlyFuture && b.getDate().compareTo(today) < 0) continue;
                        list.add(b);
                    }
                    // Rendezés dátum + idő szerint
                    Collections.sort(list, new Comparator<Booking>() {
                        @Override
                        public int compare(Booking o1, Booking o2) {
                            int cmp = o1.getDate().compareTo(o2.getDate());
                            if (cmp != 0) return orderAsc ? cmp : -cmp;
                            int tcmp = o1.getTime().compareTo(o2.getTime());
                            return orderAsc ? tcmp : -tcmp;
                        }
                    });

                    // Ha nincs elem, mutassuk az üres üzenetet
                    if (list.isEmpty()) {
                        bookingRecyclerView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                    } else {
                        emptyView.setVisibility(View.GONE);
                        bookingRecyclerView.setVisibility(View.VISIBLE);
                    }

                    BookingAdapter adapter = new BookingAdapter(list, new BookingAdapter.OnBookingActionListener() {
                        @Override
                        public void onEdit(Booking booking) {
                            startActivity(new Intent(HomeActivity.this, EditBookingActivity.class)
                                    .putExtra("bookingId", booking.getId())
                                    .putExtra("bookingDate", booking.getDate())
                                    .putExtra("bookingTime", booking.getTime()));
                        }
                        @Override
                        public void onDelete(Booking booking) {
                            new AlertDialog.Builder(HomeActivity.this)
                                    .setTitle("Időpont törlése")
                                    .setMessage("Biztosan törölni akarja az időpontot?")
                                    .setPositiveButton("Igen", (d,w) -> {
                                        FirebaseFirestore.getInstance()
                                                .collection("bookings")
                                                .document(booking.getId())
                                                .delete()
                                                .addOnSuccessListener(u -> loadBookings())
                                                .addOnFailureListener(e ->
                                                        Toast.makeText(HomeActivity.this,
                                                                "Hiba törléskor: " + e.getMessage(),
                                                                Toast.LENGTH_SHORT).show()
                                                );
                                    })
                                    .setNegativeButton("Mégse", null)
                                    .show();
                        }
                    });
                    bookingRecyclerView.setAdapter(adapter);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Hiba listázáskor: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private String getTodayString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .format(Calendar.getInstance().getTime());
    }
}
