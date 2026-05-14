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
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class PassengerAdapter
        extends RecyclerView.Adapter<PassengerAdapter.PassengerViewHolder> {

    private final List<BookedPassenger> passengers;

    public PassengerAdapter(List<BookedPassenger> passengers) {
        this.passengers = passengers;
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

        holder.btnCall.setOnClickListener(v -> {
            String phone = p.getPhone();
            if (phone == null || phone.isEmpty()) return;
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phone));
            v.getContext().startActivity(intent);
        });

        holder.btnCall.setVisibility(
                (p.getPhone() != null && !p.getPhone().isEmpty())
                        ? View.VISIBLE : View.GONE);
    }

    @Override
    public int getItemCount() { return passengers.size(); }

    static class PassengerViewHolder extends RecyclerView.ViewHolder {
        TextView       tvPassengerNumber;
        TextView       tvPassengerName;
        TextView       tvPassengerPhone;
        TextView       tvPassengerEmail;
        MaterialButton btnCall;

        PassengerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPassengerNumber = itemView.findViewById(R.id.tvPassengerNumber);
            tvPassengerName   = itemView.findViewById(R.id.tvPassengerName);
            tvPassengerPhone  = itemView.findViewById(R.id.tvPassengerPhone);
            tvPassengerEmail  = itemView.findViewById(R.id.tvPassengerEmail);
            btnCall           = itemView.findViewById(R.id.btnCallPassenger);
        }
    }
}