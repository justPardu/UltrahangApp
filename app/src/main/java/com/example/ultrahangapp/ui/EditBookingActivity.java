package com.example.ultrahangapp.ui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.ultrahangapp.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class EditBookingActivity extends AppCompatActivity {

    private String bookingId;
    private FirebaseFirestore db;
    private Calendar selectedDate;
    private int selectedHour = -1;
    private int selectedMinute = -1;

    private TextView selectedDateText, selectedTimeText;
    private Button pickDateButton, pickTimeButton, saveButton, backButton;

    private final int START_HOUR = 9;
    private final int END_HOUR = 15;
    private final int SLOT_MIN = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_booking);

        db = FirebaseFirestore.getInstance();

        selectedDateText = findViewById(R.id.editSelectedDateText);
        selectedTimeText = findViewById(R.id.editSelectedTimeText);
        pickDateButton = findViewById(R.id.editPickDateButton);
        pickTimeButton = findViewById(R.id.editPickTimeButton);
        saveButton = findViewById(R.id.saveButton);
        backButton = findViewById(R.id.backButton);

        Intent intent = getIntent();
        bookingId = intent.getStringExtra("bookingId");
        String bookingDate = intent.getStringExtra("bookingDate");
        String bookingTime = intent.getStringExtra("bookingTime");

        selectedDateText.setText("Kiválasztott dátum: " + bookingDate);
        selectedTimeText.setText("Kiválasztott idő: " + bookingTime);

        selectedDate = Calendar.getInstance();
        String[] dateParts = bookingDate.split("-");
        selectedDate.set(Calendar.YEAR, Integer.parseInt(dateParts[0]));
        selectedDate.set(Calendar.MONTH, Integer.parseInt(dateParts[1]) - 1);
        selectedDate.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateParts[2]));

        String[] timeParts = bookingTime.split(":");
        selectedHour = Integer.parseInt(timeParts[0]);
        selectedMinute = Integer.parseInt(timeParts[1]);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Időpont módosítása");
        toolbar.setTitleTextColor(Color.WHITE);

        backButton.setOnClickListener(v -> finish());

        pickDateButton.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                Calendar sel = Calendar.getInstance();
                sel.set(year, month, dayOfMonth);
                int dow = sel.get(Calendar.DAY_OF_WEEK);
                if (dow == Calendar.SUNDAY) {
                    Toast.makeText(this, "Vasárnap nem választható!", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Múltbéli dátum tiltása
                Calendar today = Calendar.getInstance();
                today.set(Calendar.HOUR_OF_DAY, 0);
                today.set(Calendar.MINUTE, 0);
                today.set(Calendar.SECOND, 0);
                today.set(Calendar.MILLISECOND, 0);

                if (sel.before(today)) {
                    Toast.makeText(this, "Nem választhatsz múltbéli dátumot!", Toast.LENGTH_SHORT).show();
                    return;
                }

                selectedDate.set(year, month, dayOfMonth);
                selectedDateText.setText(String.format("Kiválasztott dátum: %d-%02d-%02d", year, month + 1, dayOfMonth));
            }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show();
        });


        pickTimeButton.setOnClickListener(v -> {
            List<String> slots = generateTimeSlots();
            new AlertDialog.Builder(this)
                    .setTitle("Válassz időpontot")
                    .setItems(slots.toArray(new String[0]), (dialog, which) -> {
                        String chosen = slots.get(which);
                        selectedTimeText.setText("Kiválasztott idő: " + chosen);
                        String[] parts = chosen.split(":");
                        selectedHour = Integer.parseInt(parts[0]);
                        selectedMinute = Integer.parseInt(parts[1]);
                    })
                    .show();
        });

        saveButton.setOnClickListener(v -> {
            if (selectedDate != null && selectedHour != -1) {
                // Múltbéli időpont ellenőrzése
                Calendar now = Calendar.getInstance();
                Calendar selectedDateTime = (Calendar) selectedDate.clone();
                selectedDateTime.set(Calendar.HOUR_OF_DAY, selectedHour);
                selectedDateTime.set(Calendar.MINUTE, selectedMinute);
                selectedDateTime.set(Calendar.SECOND, 0);
                selectedDateTime.set(Calendar.MILLISECOND, 0);

                if (selectedDateTime.before(now)) {
                    Toast.makeText(this, "Nem választhatsz múltbéli időpontot!", Toast.LENGTH_SHORT).show();
                    return;
                }

                String dateString = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.getTime());
                String timeString = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);

                // Ellenőrzés az ütközésekre
                db.collection("bookings")
                        .whereEqualTo("date", dateString)
                        .whereEqualTo("time", timeString)
                        .get()
                        .addOnSuccessListener(query -> {
                            boolean isTaken = false;
                            for (var doc : query.getDocuments()) {
                                if (!doc.getId().equals(bookingId)) {
                                    isTaken = true;
                                    break;
                                }
                            }
                            if (isTaken) {
                                Toast.makeText(this, "Ez az időpont már foglalt!", Toast.LENGTH_SHORT).show();
                            } else {
                                db.collection("bookings")
                                        .document(bookingId)
                                        .update("date", dateString, "time", timeString)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, "Időpont sikeresen módosítva", Toast.LENGTH_SHORT).show();
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Hiba: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            }
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Hiba a lekérdezés során: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });

            } else {
                Toast.makeText(this, "Kérlek válassz dátumot és időt!", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private List<String> generateTimeSlots() {
        List<String> slots = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, START_HOUR);
        cal.set(Calendar.MINUTE, 0);
        while (cal.get(Calendar.HOUR_OF_DAY) < END_HOUR ||
                (cal.get(Calendar.HOUR_OF_DAY) == END_HOUR && cal.get(Calendar.MINUTE) == 0)) {
            slots.add(String.format(Locale.getDefault(), "%02d:%02d",
                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)));
            cal.add(Calendar.MINUTE, SLOT_MIN);
        }
        return slots;
    }
}
