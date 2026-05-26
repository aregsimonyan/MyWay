package com.myway.myway.adapters;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.myway.myway.R;
import com.myway.myway.TripPassengersActivity;
import com.myway.myway.models.Trip;
import com.myway.myway.utils.RatingUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class DriverTripAdapter
        extends RecyclerView.Adapter<DriverTripAdapter.DriverTripViewHolder> {

    private final List<Trip>            tripList;
    private final OnDeleteClickListener listener;

    public interface OnDeleteClickListener {
        void onDeleteClick(Trip trip);
    }

    public DriverTripAdapter(List<Trip> tripList, OnDeleteClickListener listener) {
        this.tripList = tripList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DriverTripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_driver_trip, parent, false);
        return new DriverTripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DriverTripViewHolder holder, int position) {
        Trip trip = tripList.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.US);

        holder.tvRoute.setText(trip.getFromLocation() + "  →  " + trip.getToLocation());
        holder.tvDateTime.setText(sdf.format(trip.getDateTime()));

        int booked = trip.getTotalSeats() - trip.getSeatsAvailable();
        holder.tvPrice.setText((int) trip.getPricePerSeat() + " AMD  ·  "
                + trip.getSeatsAvailable() + "/" + trip.getTotalSeats() + " seats left"
                + (booked > 0 ? "  (" + booked + " booked)" : ""));

        holder.tvCar.setText(trip.getCarCategory() + "  ·  " + trip.getLicensePlate());

        holder.tvOwnRating.setVisibility(View.GONE);
        String driverUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (driverUid != null) {
            FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(driverUid)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc == null) return;
                        Double avg   = doc.getDouble("averageRating");
                        Long   count = doc.getLong("ratingCount");
                        if (avg != null && count != null && count > 0) {
                            holder.tvOwnRating.setText(
                                    RatingUtils.buildStarsText(avg, count.intValue()));
                            holder.tvOwnRating.setVisibility(View.VISIBLE);
                        }
                    });
        }

        long now = System.currentTimeMillis();
        if (trip.getDateTime() < now) {
            holder.tvStatus.setText("PAST");
            holder.tvStatus.setBackgroundColor(0xFF555555);
            holder.btnDelete.setVisibility(View.GONE);
        } else {
            holder.tvStatus.setText("UPCOMING");
            holder.tvStatus.setBackgroundColor(0xFF2E7D32);
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(trip));
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), TripPassengersActivity.class);
            intent.putExtra(TripPassengersActivity.EXTRA_TRIP_ID, trip.getTripId());
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() { return tripList.size(); }

    public static class DriverTripViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoute;
        TextView tvDateTime;
        TextView tvPrice;
        TextView tvCar;
        TextView tvStatus;
        TextView tvOwnRating;
        Button   btnDelete;

        public DriverTripViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoute      = itemView.findViewById(R.id.tvDriverTripRoute);
            tvDateTime   = itemView.findViewById(R.id.tvDriverTripDateTime);
            tvPrice      = itemView.findViewById(R.id.tvDriverTripPrice);
            tvCar        = itemView.findViewById(R.id.tvDriverTripCar);
            tvStatus     = itemView.findViewById(R.id.tvDriverTripStatus);
            tvOwnRating  = itemView.findViewById(R.id.tvDriverOwnRating);
            btnDelete    = itemView.findViewById(R.id.btnDeleteTrip);
        }
    }
}