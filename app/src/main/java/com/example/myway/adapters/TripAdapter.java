package com.example.myway.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myway.R;
import com.example.myway.models.Trip;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TripAdapter extends RecyclerView.Adapter<TripAdapter.TripViewHolder> {

    private List<Trip> tripList;
    private OnTripClickListener listener;

    public interface OnTripClickListener {
        void onBookClick(Trip trip);
    }

    public TripAdapter(List<Trip> tripList, OnTripClickListener listener) {
        this.tripList = tripList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TripViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trip, parent, false);
        return new TripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TripViewHolder holder, int position) {
        Trip trip = tripList.get(position);

        holder.tvRoute.setText(trip.getFromLocation() + " -> " + trip.getToLocation());
        holder.tvPrice.setText((int)trip.getPricePerSeat() + " AMD");

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US);
        holder.tvDateTime.setText(sdf.format(trip.getDateTime()));

        holder.tvDriverInfo.setText(trip.getDriverName() + " (" + trip.getCarCategory() + ")");
        holder.tvSeats.setText(trip.getSeatsAvailable() + " seats left");

        if (trip.getSeatsAvailable() <= 0) {
            holder.btnBook.setEnabled(false);
            holder.btnBook.setText("FULL");
        } else {
            holder.btnBook.setEnabled(true);
            holder.btnBook.setText("BOOK");
        }

        holder.btnBook.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onBookClick(trip);
            }
        });
    }

    @Override
    public int getItemCount() {
        return tripList.size();
    }

    public static class TripViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoute, tvPrice, tvDateTime, tvDriverInfo, tvSeats;
        Button btnBook;

        public TripViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoute = itemView.findViewById(R.id.tvRoute);
            tvPrice = itemView.findViewById(R.id.tvPrice);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvDriverInfo = itemView.findViewById(R.id.tvDriverInfo);
            tvSeats = itemView.findViewById(R.id.tvSeats);
            btnBook = itemView.findViewById(R.id.btnBook);
        }
    }
}