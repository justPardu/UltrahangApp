package com.example.ultrahangapp.service;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.ultrahangapp.model.Booking;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;

public class FirestoreService {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference bookingRef = db.collection("bookings");

    // Új foglalás létrehozása
    public void addBooking(Booking booking) {
        bookingRef.add(booking)
                .addOnSuccessListener(documentReference -> Log.d("Firestore", "Foglalás hozzáadva: " + documentReference.getId()))
                .addOnFailureListener(e -> Log.e("Firestore", "Hiba a foglalás hozzáadásakor", e));
    }

    // Foglalások lekérdezése
    public void getAllBookings(final Callback callback) {
        bookingRef.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    QuerySnapshot querySnapshot = task.getResult();
                    if (querySnapshot != null) {
                        // Lekérjük a foglalásokat és visszaadjuk őket a callback-en keresztül
                        List<Booking> bookings = querySnapshot.toObjects(Booking.class);
                        callback.onSuccess(bookings); // Sikeres lekérdezés esetén
                    } else {
                        callback.onFailure(new Exception("Nincsenek elérhető foglalások."));
                    }
                } else {
                    callback.onFailure(task.getException()); // Hibás lekérdezés esetén
                }
            }
        });
    }

    // Foglalás frissítése
    public void updateBooking(String documentId, Booking updatedBooking) {
        bookingRef.document(documentId).set(updatedBooking)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Foglalás frissítve"))
                .addOnFailureListener(e -> Log.e("Firestore", "Hiba a frissítéskor", e));
    }

    // Foglalás törlése
    public void deleteBooking(String documentId) {
        bookingRef.document(documentId).delete()
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Foglalás törölve"))
                .addOnFailureListener(e -> Log.e("Firestore", "Hiba a törléskor", e));
    }

    // Callback interface az adatlekérés eredményének kezelésére
    public interface Callback {
        void onSuccess(List<Booking> bookings);  // Sikeres adatlekérés
        void onFailure(Exception e);             // Hiba esetén
    }
}
