package com.myway.myway;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Filter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.myway.myway.utils.PolylineUtils;
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

    /**
     * ArrayAdapter whose Filter does nothing — every item we add is always shown.
     * This is necessary because AutoCompleteTextView's default Filter hides items
     * that don't share a prefix with the typed text, which breaks geocoded results.
     */
    private static class NoFilterAdapter extends ArrayAdapter<String> {
        private final List<String> allItems = new ArrayList<>();

        private final Filter noOpFilter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults r = new FilterResults();
                r.values = allItems;
                r.count  = allItems.size();
                return r;
            }
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                notifyDataSetChanged();
            }
        };

        NoFilterAdapter(Context ctx) {
            super(ctx, android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        }

        void setItems(List<String> items) {
            allItems.clear();
            allItems.addAll(items);
            clear();
            addAll(items);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Filter getFilter() {
            return noOpFilter;
        }
    }

    private GoogleMap map;

    private Marker startMarker;
    private Marker endMarker;
    private Polyline autoRouteLine;
    private LatLng startPoint;
    private LatLng endPoint;

    private final List<LatLng>   customPoints       = new ArrayList<>();
    private final List<Marker>   customMarkers      = new ArrayList<>();
    private final List<LatLng>   fullRoadPath       = new ArrayList<>();
    private final List<Polyline> customSegmentLines = new ArrayList<>();

    private AutoCompleteTextView etFromSearch;
    private AutoCompleteTextView etToSearch;
    private TextView             tvInstruction;
    private TextView             tvStartName;
    private TextView             tvEndName;
    private ProgressBar          progressBar;
    private MaterialButton       btnConfirmRoute;
    private MaterialButton       btnResetRoute;
    private MaterialButton       btnSwitchMode;
    private MaterialButton       btnFinishCustom;

    private NoFilterAdapter fromAdapter;
    private NoFilterAdapter toAdapter;

    private final List<Address> fromAddressResults = new ArrayList<>();
    private final List<Address> toAddressResults   = new ArrayList<>();

    private final Handler searchHandler   = new Handler(Looper.getMainLooper());
    private Runnable      fromRunnable;
    private Runnable      toRunnable;

    private boolean settingFromCode = false;
    private boolean settingToCode   = false;

    private boolean pickingStart    = true;
    private boolean customMode      = false;
    private boolean fetchingSegment = false;

    private String lastEncodedPolyline = "";
    private String startName = "";
    private String endName   = "";

    private interface NameCallback    { void onName(String name); }
    private interface SegmentCallback { void onDone(List<LatLng> points); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_selection);

        etFromSearch    = findViewById(R.id.etFromSearch);
        etToSearch      = findViewById(R.id.etToSearch);
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

        fromAdapter = new NoFilterAdapter(this);
        toAdapter   = new NoFilterAdapter(this);
        etFromSearch.setAdapter(fromAdapter);
        etToSearch.setAdapter(toAdapter);

        etFromSearch.setThreshold(1);
        etToSearch.setThreshold(1);

        etFromSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int st, int before, int count) {
                if (settingFromCode) return;
                if (fromRunnable != null) searchHandler.removeCallbacks(fromRunnable);
                String q = s.toString().trim();
                if (q.length() < 2) return;
                fromRunnable = () -> fetchSuggestions(q, true);
                searchHandler.postDelayed(fromRunnable, 450);
            }
        });

        etToSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int st, int before, int count) {
                if (settingToCode) return;
                if (toRunnable != null) searchHandler.removeCallbacks(toRunnable);
                String q = s.toString().trim();
                if (q.length() < 2) return;
                toRunnable = () -> fetchSuggestions(q, false);
                searchHandler.postDelayed(toRunnable, 450);
            }
        });

        etFromSearch.setOnItemClickListener((parent, view, position, id) -> {
            if (position < fromAddressResults.size()) {
                Address a  = fromAddressResults.get(position);
                LatLng  pt = new LatLng(a.getLatitude(), a.getLongitude());
                String label = buildLabel(a);
                setFromSilently(label);
                etFromSearch.dismissDropDown();
                etFromSearch.clearFocus();
                placeStart(pt, label);
            }
        });

        etToSearch.setOnItemClickListener((parent, view, position, id) -> {
            if (position < toAddressResults.size()) {
                Address a  = toAddressResults.get(position);
                LatLng  pt = new LatLng(a.getLatitude(), a.getLongitude());
                String label = buildLabel(a);
                setToSilently(label);
                etToSearch.dismissDropDown();
                etToSearch.clearFocus();
                placeEnd(pt, label);
            }
        });

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

    private void setFromSilently(String text) {
        settingFromCode = true;
        etFromSearch.setText(text);
        if (text.length() > 0) etFromSearch.setSelection(text.length());
        settingFromCode = false;
    }

    private void setToSilently(String text) {
        settingToCode = true;
        etToSearch.setText(text);
        if (text.length() > 0) etToSearch.setSelection(text.length());
        settingToCode = false;
    }

    private void fetchSuggestions(String query, boolean isFrom) {
        new Thread(() -> {
            List<Address> results = new ArrayList<>();
            List<String>  labels  = new ArrayList<>();
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> found = geocoder.getFromLocationName(query, 5);
                if (found != null) {
                    for (Address a : found) {
                        results.add(a);
                        labels.add(buildLabel(a));
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Geocoder error: " + e.getMessage());
            }
            runOnUiThread(() -> {
                if (isFrom) {
                    fromAddressResults.clear();
                    fromAddressResults.addAll(results);
                    fromAdapter.setItems(labels);
                    if (!labels.isEmpty()) etFromSearch.showDropDown();
                } else {
                    toAddressResults.clear();
                    toAddressResults.addAll(results);
                    toAdapter.setItems(labels);
                    if (!labels.isEmpty()) etToSearch.showDropDown();
                }
            });
        }).start();
    }

    private String buildLabel(Address address) {
        String line = address.getAddressLine(0);
        if (line != null && !line.isEmpty()) return line;
        StringBuilder sb = new StringBuilder();
        if (address.getThoroughfare() != null) sb.append(address.getThoroughfare()).append(", ");
        if (address.getLocality()     != null) sb.append(address.getLocality());
        return sb.toString().trim();
    }

    private void placeStart(LatLng point, String label) {
        startPoint = point;
        startName  = label;
        if (startMarker != null) startMarker.remove();
        startMarker = map.addMarker(new MarkerOptions()
                .position(point)
                .title(label)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        tvStartName.setText("From: " + label);
        tvStartName.setVisibility(View.VISIBLE);
        pickingStart = false;
        if (endPoint != null) {
            tvInstruction.setText("Fetching road route...");
            fetchAutoRoute();
        } else {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 13));
            tvInstruction.setText("Now set the END point");
        }
    }

    private void placeEnd(LatLng point, String label) {
        endPoint = point;
        endName  = label;
        if (endMarker != null) endMarker.remove();
        endMarker = map.addMarker(new MarkerOptions()
                .position(point)
                .title(label)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        tvEndName.setText("To: " + label);
        tvEndName.setVisibility(View.VISIBLE);
        if (startPoint != null) {
            tvInstruction.setText("Fetching road route...");
            fetchAutoRoute();
        } else {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(point, 13));
            tvInstruction.setText("Now set the START point");
        }
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

    private void handleAutoTap(LatLng point) {
        if (pickingStart) {
            tvInstruction.setText("Detecting location...");
            reverseGeocode(point, name -> {
                setFromSilently(name);
                placeStart(point, name);
            });
        } else {
            tvInstruction.setText("Detecting location...");
            reverseGeocode(point, name -> {
                setToSilently(name);
                placeEnd(point, name);
            });
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
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        .title("Point " + customPoints.size()));
                customMarkers.add(m);
                fullRoadPath.addAll(segmentPoints);
                redrawFullCustomPath();
                tvInstruction.setText("Points: " + customPoints.size() + ". Add more or tap FINISH.");
                fitCameraToCustomPoints();
            });
        }
    }

    private void redrawFullCustomPath() {
        for (Polyline pl : customSegmentLines) pl.remove();
        customSegmentLines.clear();
        if (fullRoadPath.size() >= 2) {
            Polyline pl = map.addPolyline(new PolylineOptions()
                    .addAll(fullRoadPath).width(12f).color(0xFFFFAA00).geodesic(false));
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
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                conn.disconnect();
                JSONObject response = new JSONObject(sb.toString());
                if ("OK".equals(response.getString("status"))) {
                    JSONArray routes = response.getJSONArray("routes");
                    if (routes.length() > 0) {
                        String encoded = routes.getJSONObject(0)
                                .getJSONObject("overview_polyline").getString("points");
                        result.addAll(PolylineUtils.decode(encoded));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Segment fetch error: " + e.getMessage());
            }
            if (result.isEmpty()) { result.add(from); result.add(to); }
            runOnUiThread(() -> callback.onDone(result));
        }).start();
    }

    private void fitCameraToCustomPoints() {
        if (customPoints.size() < 2) return;
        LatLngBounds.Builder b = new LatLngBounds.Builder();
        for (LatLng p : customPoints) b.include(p);
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(b.build(), 120));
    }

    private void finishCustomRoute() {
        if (customPoints.size() < 2) {
            Toast.makeText(this, "Add at least 2 points", Toast.LENGTH_SHORT).show();
            return;
        }
        if (fetchingSegment) {
            Toast.makeText(this, "Still fetching the last road segment, please wait.", Toast.LENGTH_SHORT).show();
            return;
        }
        startPoint = customPoints.get(0);
        endPoint   = customPoints.get(customPoints.size() - 1);
        for (int i = 1; i < customMarkers.size() - 1; i++) customMarkers.get(i).remove();
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
            setFromSilently(sName);
            reverseGeocode(endPoint, eName -> {
                endName = eName;
                tvEndName.setText("To: " + eName);
                tvEndName.setVisibility(View.VISIBLE);
                setToSilently(eName);
                tvInstruction.setText("Route ready. Confirm or Reset.");
                btnConfirmRoute.setEnabled(true);
                btnFinishCustom.setVisibility(View.GONE);
                fitCameraToCustomPoints();
            });
        });
    }

    private void reverseGeocode(LatLng point, NameCallback callback) {
        new Thread(() -> {
            final String[] name = {String.format(Locale.US, "%.4f, %.4f", point.latitude, point.longitude)};
            try {
                Geocoder geocoder   = new Geocoder(this, Locale.getDefault());
                List<Address> list  = geocoder.getFromLocation(point.latitude, point.longitude, 1);
                if (list != null && !list.isEmpty()) {
                    Address a          = list.get(0);
                    String thoroughfare = a.getThoroughfare();
                    String locality     = a.getLocality();
                    String subLocality  = a.getSubLocality();
                    if (thoroughfare != null && locality != null)  name[0] = thoroughfare + ", " + locality;
                    else if (subLocality != null && locality != null) name[0] = subLocality + ", " + locality;
                    else if (locality != null)                    name[0] = locality;
                    else if (a.getAddressLine(0) != null)         name[0] = a.getAddressLine(0);
                }
            } catch (IOException e) {
                Log.e(TAG, "Reverse geocode error: " + e.getMessage());
            }
            runOnUiThread(() -> callback.onName(name[0]));
        }).start();
    }

    private void fetchAutoRoute() {
        if (startPoint == null || endPoint == null) return;
        progressBar.setVisibility(View.VISIBLE);
        btnConfirmRoute.setEnabled(false);
        String urlStr = "https://maps.googleapis.com/maps/api/directions/json"
                + "?origin=" + startPoint.latitude + "," + startPoint.longitude
                + "&destination=" + endPoint.latitude + "," + endPoint.longitude
                + "&key=" + MAPS_API_KEY;
        new Thread(() -> {
            String encoded    = null;
            String errorReason = "Unknown";
            try {
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
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
                        encoded = routes.getJSONObject(0)
                                .getJSONObject("overview_polyline").getString("points");
                    }
                } else { errorReason = status; }
            } catch (Exception e) { errorReason = e.getMessage(); }
            final String finalEncoded = encoded;
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
                    Log.w(TAG, "Fallback straight line: " + finalError);
                }
                fitCameraAutoRoute();
                btnConfirmRoute.setEnabled(true);
            });
        }).start();
    }

    private void drawAutoRoute(List<LatLng> points) {
        if (autoRouteLine != null) autoRouteLine.remove();
        autoRouteLine = map.addPolyline(new PolylineOptions()
                .addAll(points).width(12f).color(0xFFFFAA00).geodesic(false));
    }

    private void drawStraightLine() {
        if (autoRouteLine != null) autoRouteLine.remove();
        autoRouteLine = map.addPolyline(new PolylineOptions()
                .add(startPoint, endPoint).width(12f).color(0xFFFFAA00).geodesic(true));
    }

    private void fitCameraAutoRoute() {
        LatLngBounds bounds = new LatLngBounds.Builder()
                .include(startPoint).include(endPoint).build();
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
        setFromSilently("");
        setToSilently("");
        tvStartName.setVisibility(View.GONE);
        tvEndName.setVisibility(View.GONE);
        btnConfirmRoute.setEnabled(false);
        if (customMode) {
            tvInstruction.setText("Tap to add your first point.");
            btnFinishCustom.setVisibility(View.VISIBLE);
        } else {
            tvInstruction.setText("Tap the map to set the START point");
        }
    }

    private void confirmAndReturn() {
        if (startPoint == null || endPoint == null) {
            Toast.makeText(this, "Please select both start and end points", Toast.LENGTH_SHORT).show();
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