package com.example.myway.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myway.R;
import com.example.myway.models.PassengerRequest;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    private List<PassengerRequest> requestList;
    private OnDeleteClickListener listener;

    public interface OnDeleteClickListener {
        void onDeleteClick(PassengerRequest request);
    }

    public RequestAdapter(List<PassengerRequest> requestList, OnDeleteClickListener listener) {
        this.requestList = requestList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        PassengerRequest req = requestList.get(position);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.US);

        holder.tvReqRoute.setText(req.getFromLocation() + "  →  " + req.getToLocation());
        holder.tvReqDateTime.setText(sdf.format(req.getDateTime()));
        holder.tvReqMaxPrice.setText("Max: " + (int) req.getMaxPrice() + " AMD");

        holder.btnDelete.setOnClickListener(v -> listener.onDeleteClick(req));
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView tvReqRoute;
        TextView tvReqDateTime;
        TextView tvReqMaxPrice;
        Button btnDelete;

        public RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReqRoute = itemView.findViewById(R.id.tvReqRoute);
            tvReqDateTime = itemView.findViewById(R.id.tvReqDateTime);
            tvReqMaxPrice = itemView.findViewById(R.id.tvReqMaxPrice);
            btnDelete = itemView.findViewById(R.id.btnDeleteRequest);
        }
    }
}