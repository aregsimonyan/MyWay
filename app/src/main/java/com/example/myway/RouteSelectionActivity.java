package com.example.myway;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
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
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RouteSelectionActivity extends AppCompatActivity implements OnMapReadyCallback {

    static final int REQUEST_CODE = 201;
    private static final String TAG = "RouteSelection";
    private static final String MAPS_API_KEY = BuildConfig.DIRECTIONS_API_KEY;

    private GoogleMap map;

    private Marker startMarker;
    private Marker endMarker;
    private Polyline autoRouteLine;
    private LatLng startPoint;
    private LatLng endPoint;

    private final List<LatLng> customPoints = new ArrayList<>();
    private final List<Marker> customMarkers = new ArrayList<>();
    private final List<LatLng> fullRoadPath = new ArrayList<>();
    private final List<Polyline> customSegmentLines = new ArrayList<>();

    private TextView tvInstruction;
    private TextView tvStartName;
    private TextView tvEndName;
    private ProgressBar progressBar;
    private MaterialButton btnConfirmRoute;
    private MaterialButton btnResetRoute;
    private MaterialButton btnSwitchMode;
    private MaterialButton btnFinishCustom;

    private boolean pickingStart = true;
    private boolean customMode = false;
    private boolean fetchingSegment = false;

    private String lastEncodedPolyline = "";
    private String startName = "";
    private String endName = "";

    private interface NameCallback {
        void onName(String name);
    }

    private interface SegmentCallback {
        void onDone(List<LatLng> segmentPoints);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_selection);

        tvInstruction   = findViewById(R.id.tvInstruction);
        tvStartName     = findViewById(R.id.tvStartName);
        tvEndName       = findViewById(R.id.tvEndName);
        progressBar     = findViewById(R.id.progressBar);
        btnConfirmRoute = findViewById(R.id.btnConfirmRoute);
        btnResetRoute   = findViewById(R.id.btnResetRoute);
        btnSwitchMode   = findViewById(R.id.btnSwitchMode);
        btnFinishCustom = findViewById(R.id.btnFinishCustom);

        btnConfirmRoute.setEnabled(false);
        btnFinishCustom.setVisibility(View.GONE);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapRouteSelection);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        btnConfirmRoute.setOnClickListener(v -> confirmAndReturn());
        btnResetRoute.setOnClickListener(v -> resetRoute());

        btnSwitchMode.setOnClickListener(v -> {
            resetRoute();
            customMode = !customMode;
            if (customMode) {
                btnSwitchMode.setText("Use Auto Route");
                tvInstruction.setText("Tap to add your first point.");
                btnFinishCustom.setVisibility(View.VISIBLE);
            } else {
                btnSwitchMode.setText("Draw Custom Route");
                tvInstruction.setText("Tap the map to set the START point");
                btnFinishCustom.setVisibility(View.GONE);
            }
        });

        btnFinishCustom.setOnClickListener(v -> finishCustomRoute());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(39.83, 44.55), 10));
        map.setOnMapClickListener(this::handleMapTap);
    }

    private void handleMapTap(LatLng point) {
        if (fetchingSegment) {
            Toast.makeText(this, "Please wait, fetching road...", Toast.LENGTH_SHORT).show();
            return;
        }
        if (customMode) {
            handleCustomTap(point);
        } else {
            handleAutoTap(point);
        }
    }

    private void handleCustomTap(LatLng point) {
        if (customPoints.isEmpty()) {
            customPoints.add(point);

            Marker m = map.addMarker(new MarkerOptions()
                    .position(point)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .title("Start"));
            customMarkers.add(m);

            tvInstruction.setText("Start set. Tap to add more points.");
        } else {
            LatLng prev = customPoints.get(customPoints.size() - 1);

            fetchingSegment = true;
            progressBar.setVisibility(View.VISIBLE);
            tvInstruction.setText("Fetching road to next point...");

            fetchSegment(prev, point, segmentPoints -> {
                fetchingSegment = false;
                progressBar.setVisibility(View.GONE);

                customPoints.add(point);

                Marker m = map.addMarker(new MarkerOptions()
                        .position(point)
                        .icon(BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_AZURE))
                        .title("Point " + customPoints.size()));
                customMarkers.add(m);

                fullRoadPath.addAll(segmentPoints);

                redrawFullCustomPath();

                tvInstruction.setText("Points: " + customPoints.size()
                        + ". Add more or tap FINISH.");

                fitCameraToCustomPoints();
            });
        }
    }

    private void redrawFullCustomPath() {
        for (Polyline pl : customSegmentLines) pl.remove();
        customSegmentLines.clear();

        if (fullRoadPath.size() >= 2) {
            Polyline pl = map.addPolyline(new PolylineOptions()
                    .addAll(fullRoadPath)
                    .width(12f)
                    .color(0xFFFFAA00)
                    .geodesic(false));
            customSegmentLines.add(pl);
        }
    }

    private void fetchSegment(LatLng from, LatLng to, SegmentCallback callback) {
        String urlStr = "https://maps.googleapis.com/maps/api/directions/json"
                + "?origin=" + from.latitude + "," + from.longitude
                + "&destination=" + to.latitude + "," + to.longitude
                + "&key=" + MAPS_API_KEY;

        new Thread(() -> {
            List<LatLng> result = new ArrayList<>();

            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                conn.disconnect();

                JSONObject response = new JSONObject(sb.toString());
                String status       = response.getString("status");

                if ("OK".equals(status)) {
                    JSONArray routes = response.getJSONArray("routes");
                    if (routes.length() > 0) {
                        String encoded = routes.getJSONObject(0)
                                .getJSONObject("overview_polyline")
                                .getString("points");
                        result.addAll(PolylineUtils.decode(encoded));
                    }
                } else {
                    Log.w(TAG, "Directions API status: " + status);
                }
            } catch (Exception e) {
                Log.e(TAG, "Segment fetch error: " + e.getMessage());
            }

            if (result.isEmpty()) {
                result.add(from);
                result.add(to);
            }

            final List<LatLng> finalResult = result;
            runOnUiThread(() -> callback.onDone(finalResult));
        }).start();
    }

    private void fitCameraToCustomPoints() {
        if (customPoints.size() < 2) return;
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng p : customPoints) builder.include(p);
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 120));
    }

    private void finishCustomRoute() {
        if (customPoints.size() < 2) {
            Toast.makeText(this, "Add at least 2 points", Toast.LENGTH_SHORT).show();
            return;
        }
        if (fetchingSegment) {
            Toast.makeText(this,
                    "Still fetching the last road segment, please wait.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        startPoint = customPoints.get(0);
        endPoint   = customPoints.get(customPoints.size() - 1);

        for (int i = 1; i < customMarkers.size() - 1; i++) {
            customMarkers.get(i).remove();
        }
        if (customMarkers.size() > 1) {
            customMarkers.get(customMarkers.size() - 1).remove();
            Marker endM = map.addMarker(new MarkerOptions()
                    .position(endPoint)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    .title("End"));
            customMarkers.set(customMarkers.size() - 1, endM);
        }

        lastEncodedPolyline = PolylineUtils.encode(fullRoadPath);

        reverseGeocode(startPoint, sName -> {
            startName = sName;
            tvStartName.setText("From: " + sName);
            tvStartName.setVisibility(View.VISIBLE);
            reverseGeocode(endPoint, eName -> {
                endName = eName;
                tvEndName.setText("To: " + eName);
                tvEndName.setVisibility(View.VISIBLE);
                tvInstruction.setText("Route ready. Confirm or Reset.");
                btnConfirmRoute.setEnabled(true);
                btnFinishCustom.setVisibility(View.GONE);
                fitCameraToCustomPoints();
            });
        });
    }

    private void handleAutoTap(LatLng point) {
        if (pickingStart) {
            startPoint = point;
            if (startMarker != null) startMarker.remove();
            startMarker = map.addMarker(new MarkerOptions()
                    .position(point)
                    .title("Start")
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_GREEN)));
            tvInstruction.setText("Detecting location...");
            reverseGeocode(point, name -> {
                startName = name;
                tvStartName.setText("From: " + name);
                tvStartName.setVisibility(View.VISIBLE);
                if (startMarker != null) startMarker.setTitle(name);
                pickingStart = false;
                tvInstruction.setText("Now tap to set the END point");
            });
        } else {
            endPoint = point;
            if (endMarker != null) endMarker.remove();
            endMarker = map.addMarker(new MarkerOptions()
                    .position(point)
                    .title("End")
                    .icon(BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_RED)));
            tvInstruction.setText("Detecting location...");
            reverseGeocode(point, name -> {
                endName = name;
                tvEndName.setText("To: " + name);
                tvEndName.setVisibility(View.VISIBLE);
                if (endMarker != null) endMarker.setTitle(name);
                tvInstruction.setText("Fetching road route...");
                fetchAutoRoute();
            });
        }
    }

    private void reverseGeocode(LatLng point, NameCallback callback) {
        new Thread(() -> {
            final String[] detectedName = {formatCoordinate(point)};
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(
                        point.latitude, point.longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address     = addresses.get(0);
                    String subLocality  = address.getSubLocality();
                    String locality     = address.getLocality();
                    String thoroughfare = address.getThoroughfare();
                    if (thoroughfare != null && locality != null) {
                        detectedName[0] = thoroughfare + ", " + locality;
                    } else if (subLocality != null && locality != null) {
                        detectedName[0] = subLocality + ", " + locality;
                    } else if (locality != null) {
                        detectedName[0] = locality;
                    } else if (address.getAddressLine(0) != null) {
                        detectedName[0] = address.getAddressLine(0);
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Geocoder error: " + e.getMessage());
            }
            runOnUiThread(() -> callback.onName(detectedName[0]));
        }).start();
    }

    private String formatCoordinate(LatLng point) {
        return String.format(Locale.US, "%.4f, %.4f", point.latitude, point.longitude);
    }

    private void fetchAutoRoute() {
        progressBar.setVisibility(View.VISIBLE);
        btnConfirmRoute.setEnabled(false);

        String urlStr = "https://maps.googleapis.com/maps/api/directions/json"
                + "?origin=" + startPoint.latitude + "," + startPoint.longitude
                + "&destination=" + endPoint.latitude + "," + endPoint.longitude
                + "&key=" + MAPS_API_KEY;

        new Thread(() -> {
            String encodedPolyline = null;
            String errorReason     = "Unknown error";

            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                conn.disconnect();

                JSONObject response = new JSONObject(sb.toString());
                String status       = response.getString("status");

                if ("OK".equals(status)) {
                    JSONArray routes = response.getJSONArray("routes");
                    if (routes.length() > 0) {
                        encodedPolyline = routes.getJSONObject(0)
                                .getJSONObject("overview_polyline")
                                .getString("points");
                    } else {
                        errorReason = "No routes returned by API";
                    }
                } else {
                    errorReason = response.has("error_message")
                            ? status + ": " + response.getString("error_message")
                            : "API status: " + status;
                }
            } catch (Exception e) {
                errorReason = e.getClass().getSimpleName() + ": " + e.getMessage();
            }

            final String finalEncoded = encodedPolyline;
            final String finalError   = errorReason;

            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                if (finalEncoded != null && !finalEncoded.isEmpty()) {
                    lastEncodedPolyline = finalEncoded;
                    drawAutoRoute(PolylineUtils.decode(finalEncoded));
                    tvInstruction.setText("Route found. Confirm or Reset.");
                } else {
                    lastEncodedPolyline = "";
                    drawStraightLine();
                    tvInstruction.setText("Straight line shown. Confirm or Reset.");
                    Toast.makeText(RouteSelectionActivity.this,
                            "Road route unavailable. Showing straight line.",
                            Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Falling back to straight line. Reason: " + finalError);
                }
                fitCameraAutoRoute();
                btnConfirmRoute.setEnabled(true);
            });
        }).start();
    }

    private void drawAutoRoute(List<LatLng> points) {
        if (autoRouteLine != null) autoRouteLine.remove();
        autoRouteLine = map.addPolyline(new PolylineOptions()
                .addAll(points)
                .width(12f)
                .color(0xFFFFAA00)
                .geodesic(false));
    }

    private void drawStraightLine() {
        if (autoRouteLine != null) autoRouteLine.remove();
        autoRouteLine = map.addPolyline(new PolylineOptions()
                .add(startPoint, endPoint)
                .width(12f)
                .color(0xFFFFAA00)
                .geodesic(true));
    }

    private void fitCameraAutoRoute() {
        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(startPoint)
                .include(endPoint)
                .build();
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120));
    }

    private void resetRoute() {
        if (startMarker   != null) { startMarker.remove();   startMarker   = null; }
        if (endMarker     != null) { endMarker.remove();     endMarker     = null; }
        if (autoRouteLine != null) { autoRouteLine.remove(); autoRouteLine = null; }

        for (Polyline pl : customSegmentLines) pl.remove();
        customSegmentLines.clear();
        for (Marker m : customMarkers) m.remove();
        customMarkers.clear();
        customPoints.clear();
        fullRoadPath.clear();

        startPoint = null;
        endPoint   = null;
        startName  = "";
        endName    = "";
        lastEncodedPolyline = "";
        pickingStart    = true;
        fetchingSegment = false;

        progressBar.setVisibility(View.GONE);

        if (customMode) {
            tvInstruction.setText("Tap to add your first point.");
            btnFinishCustom.setVisibility(View.VISIBLE);
        } else {
            tvInstruction.setText("Tap the map to set the START point");
        }

        tvStartName.setVisibility(View.GONE);
        tvEndName.setVisibility(View.GONE);
        btnConfirmRoute.setEnabled(false);
    }

    private void confirmAndReturn() {
        if (startPoint == null || endPoint == null) {
            Toast.makeText(this,
                    "Please select both start and end points",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Intent result = new Intent();
        result.putExtra("startLat",        startPoint.latitude);
        result.putExtra("startLng",        startPoint.longitude);
        result.putExtra("endLat",          endPoint.latitude);
        result.putExtra("endLng",          endPoint.longitude);
        result.putExtra("encodedPolyline", lastEncodedPolyline);
        result.putExtra("startName",       startName);
        result.putExtra("endName",         endName);
        setResult(RESULT_OK, result);
        finish();
    }
}