package com.example.ultrahangapp.ui;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.AnimatorSet;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.ultrahangapp.R;
import com.example.ultrahangapp.model.Booking;
import com.example.ultrahangapp.receiver.AlarmReceiver;
import com.example.ultrahangapp.receiver.NotificationReceiver;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BookingActivity extends AppCompatActivity {

    private Button pickDateButton, confirmBookingButton, pickTimeButton, backButton;
    private TextView selectedDateText, selectedTimeText;
    private Calendar selectedDate;
    private FirebaseFirestore db;
    private FirebaseUser user;
    private int selectedHour = -1;
    private int selectedMinute = -1;

    private final int START_HOUR = 9;
    private final int END_HOUR = 15;
    private final int SLOT_MIN = 20;

    private static final int NOTIFICATION_ID = 1;
    private static final int ALARM_REQUEST_CODE = 100;
    private static final int REQUEST_POST_NOTIFICATIONS = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking);

        pickDateButton = findViewById(R.id.pickDateButton);
        confirmBookingButton = findViewById(R.id.confirmBookingButton);
        selectedDateText = findViewById(R.id.selectedDateText);
        pickTimeButton = findViewById(R.id.pickTimeButton);
        selectedTimeText = findViewById(R.id.selectedTimeText);
        backButton = findViewById(R.id.backButton);

        db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Időpontfoglalás");
        toolbar.setTitleTextColor(Color.WHITE);

        backButton.setOnClickListener(v -> finish());

        pickDateButton.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            new android.app.DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        Calendar sel = Calendar.getInstance();
                        sel.set(year, month, dayOfMonth);
                        int dow = sel.get(Calendar.DAY_OF_WEEK);
                        if (dow == Calendar.SUNDAY) {
                            Toast.makeText(this, "Vasárnap nem foglalhatsz időpontot.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        selectedDate = sel;
                        selectedDateText.setText(String.format("Kiválasztott dátum: %d-%02d-%02d", year, month + 1, dayOfMonth));
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
            ).show();
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

        confirmBookingButton.setOnClickListener(v -> {
            if (selectedDate == null || selectedHour < 0 || user == null) {
                Toast.makeText(this, "Kérlek válassz dátumot és időt!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Jelenlegi időpont Calendar példányban
            Calendar now = Calendar.getInstance();

            // Kiválasztott időpont összeállítása Calendar-ban
            Calendar selectedDateTime = Calendar.getInstance();
            selectedDateTime.set(
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH),
                    selectedHour,
                    selectedMinute,
                    0
            );
            selectedDateTime.set(Calendar.MILLISECOND, 0);

            // Ellenőrzés: múltbéli időpont?
            if (selectedDateTime.before(now)) {
                Toast.makeText(this, "Nem foglalhatsz múltbéli időpontra!", Toast.LENGTH_SHORT).show();
                return;
            }

            String dateString = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .format(selectedDate.getTime());
            String timeString = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);

            // További kód változatlan...
            db.collection("bookings")
                    .whereEqualTo("date", dateString)
                    .whereEqualTo("time", timeString)
                    .get()
                    .addOnSuccessListener(query -> {
                        if (!query.isEmpty()) {
                            Toast.makeText(this, "Ez az időpont már foglalt!", Toast.LENGTH_SHORT).show();
                        } else {
                            Booking booking = new Booking(user.getEmail(), dateString, timeString);
                            db.collection("bookings")
                                    .add(booking)
                                    .addOnSuccessListener(dref -> {
                                        Toast.makeText(this, "Sikeres foglalás!", Toast.LENGTH_SHORT).show();
                                        startSuccessAnimation();
                                        requestNotificationPermission();
                                        scheduleNotification(dateString, timeString);
                                        scheduleAlarm(dateString, timeString);
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Hiba történt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Hiba a lekérdezés során: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
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

    private void startSuccessAnimation() {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(confirmBookingButton, View.SCALE_X, 1f, 1.2f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(confirmBookingButton, View.SCALE_Y, 1f, 1.2f, 1f);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(scaleX, scaleY);
        animatorSet.setDuration(700);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());
        animatorSet.start();
    }

    private void scheduleNotification(String date, String time) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                Toast.makeText(this, "Engedély szükséges a pontos riasztásokhoz.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("date", date);
        intent.putExtra("time", time);

        // Egyedi requestCode generálása az időpont alapján
        int requestCode = (date + time).hashCode();

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, Integer.parseInt(date.substring(0, 4)));
        calendar.set(Calendar.MONTH, Integer.parseInt(date.substring(5, 7)) - 1);
        calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(date.substring(8, 10)));
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.substring(0, 2)));
        calendar.set(Calendar.MINUTE, Integer.parseInt(time.substring(3, 5)));
        calendar.set(Calendar.SECOND, 0);

        // Például 10 perccel előbb értesítsük meg
        calendar.add(Calendar.MINUTE, -10);

        if (alarmManager != null) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_POST_NOTIFICATIONS);
            }
        }
    }



    private void scheduleAlarm(String date, String time) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(intent);
                Toast.makeText(this, "Kérlek engedélyezd az „időzített riasztások” beállítást!", Toast.LENGTH_LONG).show();
                return;
            }
        }

        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, Integer.parseInt(date.substring(0, 4)));
        calendar.set(Calendar.MONTH, Integer.parseInt(date.substring(5, 7)) - 1);
        calendar.set(Calendar.DAY_OF_MONTH, Integer.parseInt(date.substring(8, 10)));
        calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(time.substring(0, 2)));
        calendar.set(Calendar.MINUTE, Integer.parseInt(time.substring(3, 5)));
        calendar.set(Calendar.SECOND, 0);

        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        }
    }


    private boolean canScheduleExactAlarms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return getSystemService(AlarmManager.class).canScheduleExactAlarms();
        }
        return true;
    }
}
