package com.example.myway.adapters;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myway.R;
import com.example.myway.models.BookedPassenger;
import com.example.myway.utils.RatingUtils;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class PassengerAdapter
        extends RecyclerView.Adapter<PassengerAdapter.PassengerViewHolder> {

    public interface OnRateClickListener {
        void onRateClick(BookedPassenger passenger);
    }

    public interface TripEndedProvider {
        boolean isTripEnded();
    }

    private final List<BookedPassenger> passengers;
    private final OnRateClickListener   rateListener;
    private final TripEndedProvider     tripEndedProvider;

    public PassengerAdapter(List<BookedPassenger> passengers,
                            OnRateClickListener rateListener,
                            TripEndedProvider tripEndedProvider) {
        this.passengers        = passengers;
        this.rateListener      = rateListener;
        this.tripEndedProvider = tripEndedProvider;
    }

    @NonNull
    @Override
    public PassengerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_passenger, parent, false);
        return new PassengerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PassengerViewHolder holder, int position) {
        BookedPassenger p = passengers.get(position);

        holder.tvPassengerNumber.setText("#" + (position + 1));
        holder.tvPassengerName.setText(p.getName() != null ? p.getName() : "—");
        holder.tvPassengerPhone.setText(
                p.getPhone() != null && !p.getPhone().isEmpty() ? p.getPhone() : "No phone");
        holder.tvPassengerEmail.setText(
                p.getEmail() != null && !p.getEmail().isEmpty() ? p.getEmail() : "No email");

        if (p.getRatingCount() > 0) {
            holder.tvPassengerRating.setText(
                    RatingUtils.buildStarsText(p.getAverageRating(), p.getRatingCount()));
            holder.tvPassengerRating.setVisibility(View.VISIBLE);
        } else {
            holder.tvPassengerRating.setVisibility(View.GONE);
        }

        boolean ended = tripEndedProvider.isTripEnded();

        if (p.getPhone() != null && !p.getPhone().isEmpty()) {
            holder.btnCall.setVisibility(View.VISIBLE);
            holder.btnCall.setOnClickListener(v -> {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + p.getPhone()));
                v.getContext().startActivity(intent);
            });
        } else {
            holder.btnCall.setVisibility(View.GONE);
        }

        if (ended) {
            holder.btnRate.setVisibility(View.VISIBLE);
            holder.btnRate.setOnClickListener(v -> rateListener.onRateClick(p));
        } else {
            holder.btnRate.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() { return passengers.size(); }

    static class PassengerViewHolder extends RecyclerView.ViewHolder {
        TextView       tvPassengerNumber;
        TextView       tvPassengerName;
        TextView       tvPassengerPhone;
        TextView       tvPassengerEmail;
        TextView       tvPassengerRating;
        MaterialButton btnCall;
        MaterialButton btnRate;

        PassengerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPassengerNumber  = itemView.findViewById(R.id.tvPassengerNumber);
            tvPassengerName    = itemView.findViewById(R.id.tvPassengerName);
            tvPassengerPhone   = itemView.findViewById(R.id.tvPassengerPhone);
            tvPassengerEmail   = itemView.findViewById(R.id.tvPassengerEmail);
            tvPassengerRating  = itemView.findViewById(R.id.tvPassengerRating);
            btnCall            = itemView.findViewById(R.id.btnCallPassenger);
            btnRate            = itemView.findViewById(R.id.btnRatePassenger);
        }
    }
}