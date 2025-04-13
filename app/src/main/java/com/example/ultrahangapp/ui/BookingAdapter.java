package com.example.ultrahangapp.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ultrahangapp.R;
import com.example.ultrahangapp.model.Booking;

import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.BookingViewHolder> {

    private List<Booking> bookingList;

    public BookingAdapter(List<Booking> bookingList) {
        this.bookingList = bookingList;
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_booking, parent, false);
        return new BookingViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        Booking booking = bookingList.get(position);
        holder.patientName.setText(booking.getPatientName());
        holder.date.setText(booking.getDate());
        holder.time.setText(booking.getTime());
        holder.doctor.setText(booking.getDoctor());
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    public static class BookingViewHolder extends RecyclerView.ViewHolder {

        public TextView patientName, date, time, doctor;

        public BookingViewHolder(View itemView) {
            super(itemView);
            patientName = itemView.findViewById(R.id.patientName);
            date = itemView.findViewById(R.id.date);
            time = itemView.findViewById(R.id.time);
            doctor = itemView.findViewById(R.id.doctor);
        }
    }
}
