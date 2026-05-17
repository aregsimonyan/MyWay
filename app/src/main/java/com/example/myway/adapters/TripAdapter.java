package com.example.myway.adapters;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myway.R;
import com.example.myway.models.Trip;
import com.example.myway.utils.RatingUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {

    private final List<Trip> tripList;
    private final String     currentUserId;
    private final OnTripActionListener listener;

    public interface OnTripActionListener {
        void onCardClick(Trip trip);
        void onBookClick(Trip trip);
    }

    public TripAdapter(List<Trip> tripList, String currentUserId,
                       OnTripActionListener listener) {
        this.tripList      = tripList;
        this.currentUserId = currentUserId;
        this.listener      = listener;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trip, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = tripList.get(position);

        holder.tvRoute.setText(trip.getFromLocation() + "  →  " + trip.getToLocation());

        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, HH:mm", Locale.US);
        holder.tvDateTime.setText(sdf.format(trip.getDateTime()));
        holder.tvDriverInfo.setText(trip.getDriverName() + "  ·  " + trip.getLicensePlate());

        holder.tvPrice.setText((int) trip.getPricePerSeat() + " AMD");
        holder.tvSeats.setText(trip.getSeatsAvailable() + " seat(s) left");

        String category = trip.getCarCategory() != null ? trip.getCarCategory() : "Economy";
        holder.tvCategory.setText(category);
        if ("Business".equals(category))      holder.tvCategory.setBackgroundColor(0xFFFFAA00);
        else if ("Comfort".equals(category))  holder.tvCategory.setBackgroundColor(0xFF1E88E5);
        else                                   holder.tvCategory.setBackgroundColor(0xFF555555);

        holder.tvDriverRating.setVisibility(View.GONE);
        if (trip.getDriverId() != null && !trip.getDriverId().isEmpty()) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(trip.getDriverId())
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc == null) return;
                        Double avg   = doc.getDouble("averageRating");
                        Long   count = doc.getLong("ratingCount");
                        if (avg != null && count != null && count > 0) {
                            holder.tvDriverRating.setText(
                                    RatingUtils.buildStarsText(avg, count.intValue()));
                            holder.tvDriverRating.setVisibility(View.VISIBLE);
                        }
                    });
        }

        boolean isBooked = trip.getPassengerIds() != null
                && trip.getPassengerIds().contains(currentUserId);

        if (isBooked) {
            holder.btnBook.setText("✓ Booked");
            holder.btnBook.setBackgroundTintList(ColorStateList.valueOf(0xFF2E7D32));
            holder.btnBook.setTextColor(0xFFFFFFFF);
            holder.btnBook.setEnabled(false);
        } else if (trip.getSeatsAvailable() <= 0) {
            holder.btnBook.setText("Full");
            holder.btnBook.setBackgroundTintList(ColorStateList.valueOf(0xFF444444));
            holder.btnBook.setTextColor(0xFF888888);
            holder.btnBook.setEnabled(false);
        } else {
            holder.btnBook.setText("Book");
            holder.btnBook.setBackgroundTintList(ColorStateList.valueOf(0xFFEEEEEE));
            holder.btnBook.setTextColor(0xFF111111);
            holder.btnBook.setEnabled(true);
            holder.btnBook.setOnClickListener(v -> listener.onBookClick(trip));
        }

        holder.cardTrip.setOnClickListener(v -> listener.onCardClick(trip));
    }

    @Override
    public int getItemCount() { return tripList.size(); }

    public static class TripViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardTrip;
        TextView tvRoute, tvDateTime, tvDriverInfo, tvDriverRating,
                tvPrice, tvSeats, tvCategory;
        MaterialButton btnBook;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            cardTrip       = itemView.findViewById(R.id.cardTrip);
            tvRoute        = itemView.findViewById(R.id.tvRoute);
            tvDateTime     = itemView.findViewById(R.id.tvDateTime);
            tvDriverInfo   = itemView.findViewById(R.id.tvDriverInfo);
            tvDriverRating = itemView.findViewById(R.id.tvDriverRating);
            tvPrice        = itemView.findViewById(R.id.tvPrice);
            tvSeats        = itemView.findViewById(R.id.tvSeats);
            tvCategory     = itemView.findViewById(R.id.tvCategory);
            btnBook        = itemView.findViewById(R.id.btnBook);
        }
    }
}