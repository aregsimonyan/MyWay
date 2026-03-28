package com.example.myway;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myway.utils.PolylineUtils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class RouteSelectionActivity extends AppCompatActivity implements OnMapReadyCallback {

    static final int REQUEST_CODE = 201;
    private static final String TAG = "RouteSelection";
    private static final String MAPS_API_KEY = BuildConfig.DIRECTIONS_API_KEY;

    private GoogleMap map;
    private Marker startMarker;
    private Marker endMarker;
    private Polyline routeLine;
    private LatLng startPoint;
    private LatLng endPoint;
    private TextView tvInstruction;
    private ProgressBar progressBar;
    private boolean pickingStart = true;
    private String lastEncodedPolyline = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_selection);

        tvInstruction = findViewById(R.id.tvInstruction);
        progressBar = findViewById(R.id.progressBar);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapRouteSelection);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        findViewById(R.id.btnConfirmRoute).setEnabled(false);
        findViewById(R.id.btnConfirmRoute).setOnClickListener(v -> confirmAndReturn());
        findViewById(R.id.btnResetRoute).setOnClickListener(v -> resetRoute());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(39.83, 44.55), 10));
        map.setOnMapClickListener(this::handleMapTap);
    }

    private void handleMapTap(LatLng point) {
        if (pickingStart) {
            startPoint = point;
            if (startMarker != null) startMarker.remove();
            startMarker = map.addMarker(new MarkerOptions()
                    .position(point)
                    .title("Start")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            pickingStart = false;
            tvInstruction.setText("Now tap to set the END point");
        } else {
            endPoint = point;
            if (endMarker != null) endMarker.remove();
            endMarker = map.addMarker(new MarkerOptions()
                    .position(point)
                    .title("End")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            tvInstruction.setText("Fetching road route...");
            fetchDirectionsRoute();
        }
    }

    private void fetchDirectionsRoute() {
        progressBar.setVisibility(View.VISIBLE);
        findViewById(R.id.btnConfirmRoute).setEnabled(false);

        String urlStr = "https://maps.googleapis.com/maps/api/directions/json"
                + "?origin=" + startPoint.latitude + "," + startPoint.longitude
                + "&destination=" + endPoint.latitude + "," + endPoint.longitude
                + "&key=" + MAPS_API_KEY;

        Log.d(TAG, "Directions request URL: " + urlStr);

        new Thread(() -> {
            String encodedPolyline = null;
            String errorReason = "Unknown error";

            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                int responseCode = conn.getResponseCode();
                Log.d(TAG, "HTTP response code: " + responseCode);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                conn.disconnect();

                String rawResponse = sb.toString();
                Log.d(TAG, "Directions API raw response: " + rawResponse);

                JSONObject response = new JSONObject(rawResponse);
                String status = response.getString("status");
                Log.d(TAG, "Directions API status: " + status);

                if ("OK".equals(status)) {
                    JSONArray routes = response.getJSONArray("routes");
                    if (routes.length() > 0) {
                        JSONObject polylineObj = routes.getJSONObject(0)
                                .getJSONObject("overview_polyline");
                        encodedPolyline = polylineObj.getString("points");
                        Log.d(TAG, "Polyline decoded successfully, length: " + encodedPolyline.length());
                    } else {
                        errorReason = "No routes returned by API";
                        Log.w(TAG, errorReason);
                    }
                } else {
                    if (response.has("error_message")) {
                        errorReason = status + ": " + response.getString("error_message");
                    } else {
                        errorReason = "API status: " + status;
                    }
                    Log.e(TAG, "Directions API error: " + errorReason);
                }

            } catch (Exception e) {
                errorReason = e.getClass().getSimpleName() + ": " + e.getMessage();
                Log.e(TAG, "Exception calling Directions API: " + errorReason, e);
            }

            final String finalEncoded = encodedPolyline;
            final String finalError = errorReason;

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);

                if (finalEncoded != null && !finalEncoded.isEmpty()) {
                    lastEncodedPolyline = finalEncoded;
                    drawDecodedRoute(PolylineUtils.decode(finalEncoded));
                    tvInstruction.setText("Road route found. Confirm or Reset to change.");
                } else {
                    lastEncodedPolyline = "";
                    drawStraightLine();
                    tvInstruction.setText("Straight line shown. Confirm or Reset.");
                    Log.w(TAG, "Falling back to straight line. Reason: " + finalError);
                    Toast.makeText(RouteSelectionActivity.this,
                            "Road route unavailable (" + finalError + "). Showing straight line.",
                            Toast.LENGTH_LONG).show();
                }

                fitCameraToBothPoints();
                findViewById(R.id.btnConfirmRoute).setEnabled(true);
            });
        }).start();
    }

    private void drawDecodedRoute(List<LatLng> points) {
        if (routeLine != null) routeLine.remove();
        routeLine = map.addPolyline(new PolylineOptions()
                .addAll(points)
                .width(12f)
                .color(0xFFFFAA00)
                .geodesic(false));
    }

    private void drawStraightLine() {
        if (routeLine != null) routeLine.remove();
        routeLine = map.addPolyline(new PolylineOptions()
                .add(startPoint, endPoint)
                .width(12f)
                .color(0xFFFFAA00)
                .geodesic(true));
    }

    private void fitCameraToBothPoints() {
        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(startPoint)
                .include(endPoint)
                .build();
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120));
    }

    private void resetRoute() {
        if (startMarker != null) { startMarker.remove(); startMarker = null; }
        if (endMarker != null) { endMarker.remove(); endMarker = null; }
        if (routeLine != null) { routeLine.remove(); routeLine = null; }
        startPoint = null;
        endPoint = null;
        lastEncodedPolyline = "";
        pickingStart = true;
        tvInstruction.setText("Tap the map to set the START point");
        progressBar.setVisibility(View.GONE);
        findViewById(R.id.btnConfirmRoute).setEnabled(false);
    }

    private void confirmAndReturn() {
        if (startPoint == null || endPoint == null) {
            Toast.makeText(this, "Please select both start and end points", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent result = new Intent();
        result.putExtra("startLat", startPoint.latitude);
        result.putExtra("startLng", startPoint.longitude);
        result.putExtra("endLat", endPoint.latitude);
        result.putExtra("endLng", endPoint.longitude);
        result.putExtra("encodedPolyline", lastEncodedPolyline);
        setResult(RESULT_OK, result);
        finish();
    }
}