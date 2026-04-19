package com.example.myway;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import com.example.myway.models.PassengerRequest;
import com.example.myway.models.Trip;
import com.example.myway.utils.PolylineUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapActivity extends MenuActivity implements OnMapReadyCallback {

    private static final int FINE_PERMISSION_CODE = 1;
    private static final double LANE_OFFSET_METERS = 6.0;

    private GoogleMap myMap;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private EditText etSearchAddress;
    private ImageView btnSearchIcon;
    private ImageButton btnMore;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabMyLocation;
    private MaterialButton btnToggleTrips;
    private MaterialButton btnToggleRequests;

    private CardView cardRouteInfo;
    private TextView tvRouteTypeBadge;
    private TextView tvInfoRoute;
    private TextView tvInfoDateTime;
    private TextView tvInfoPrice;
    private TextView tvInfoSeats;
    private TextView tvInfoDriver;
    private TextView tvInfoReqDateTime;
    private TextView tvInfoMaxPrice;
    private TextView tvInfoPassenger;
    private LinearLayout layoutDriverInfo;
    private LinearLayout layoutPassengerInfo;
    private ImageButton btnCloseInfo;

    private FirebaseFirestore db;
    private final Map<String, Trip> tripMap = new HashMap<>();
    private final Map<String, PassengerRequest> requestMap = new HashMap<>();
    private final List<Trip> allTrips = new ArrayList<>();
    private final List<PassengerRequest> allRequests = new ArrayList<>();

    private boolean tripsVisible = true;
    private boolean requestsVisible = true;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        loadLocale();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        db = FirebaseFirestore.getInstance();

        etSearchAddress     = findViewById(R.id.etSearchAddress);
        btnSearchIcon       = findViewById(R.id.btnSearchIcon);
        btnMore             = findViewById(R.id.btnMore);
        fabMyLocation       = findViewById(R.id.fabMyLocation);
        btnToggleTrips      = findViewById(R.id.btnToggleTrips);
        btnToggleRequests   = findViewById(R.id.btnToggleRequests);
        cardRouteInfo       = findViewById(R.id.cardRouteInfo);
        tvRouteTypeBadge    = findViewById(R.id.tvRouteTypeBadge);
        tvInfoRoute         = findViewById(R.id.tvInfoRoute);
        tvInfoDateTime      = findViewById(R.id.tvInfoDateTime);
        tvInfoPrice         = findViewById(R.id.tvInfoPrice);
        tvInfoSeats         = findViewById(R.id.tvInfoSeats);
        tvInfoDriver        = findViewById(R.id.tvInfoDriver);
        tvInfoReqDateTime   = findViewById(R.id.tvInfoReqDateTime);
        tvInfoMaxPrice      = findViewById(R.id.tvInfoMaxPrice);
        tvInfoPassenger     = findViewById(R.id.tvInfoPassenger);
        layoutDriverInfo    = findViewById(R.id.layoutDriverInfo);
        layoutPassengerInfo = findViewById(R.id.layoutPassengerInfo);
        btnCloseInfo        = findViewById(R.id.btnCloseInfo);

        btnCloseInfo.setOnClickListener(v -> cardRouteInfo.setVisibility(View.GONE));

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        initSearchWidgets();
        setupMoreButton(btnMore);
        setupToggleButtons();
    }

    private void setupToggleButtons() {
        applyToggleStyle(btnToggleTrips,    tripsVisible,    0xFFFFAA00);
        applyToggleStyle(btnToggleRequests, requestsVisible, 0xFF4CAF50);

        btnToggleTrips.setOnClickListener(v -> {
            tripsVisible = !tripsVisible;
            applyToggleStyle(btnToggleTrips, tripsVisible, 0xFFFFAA00);
            redrawRoutes();
        });

        btnToggleRequests.setOnClickListener(v -> {
            requestsVisible = !requestsVisible;
            applyToggleStyle(btnToggleRequests, requestsVisible, 0xFF4CAF50);
            redrawRoutes();
        });
    }

    private void applyToggleStyle(MaterialButton btn, boolean active, int activeColor) {
        btn.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(active ? activeColor : 0xFF2A2A2A));
        btn.setTextColor(active ? 0xFF111111 : 0xFF888888);
        btn.setStrokeColor(
                android.content.res.ColorStateList.valueOf(active ? activeColor : 0xFF444444));
        btn.setAlpha(active ? 1f : 0.65f);
    }

    private void initSearchWidgets() {
        etSearchAddress.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || (event != null
                    && event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                geoLocate();
                return true;
            }
            return false;
        });
        btnSearchIcon.setOnClickListener(v -> geoLocate());
        fabMyLocation.setOnClickListener(v -> getLastLocation());
    }

    private void geoLocate() {
        String searchString = etSearchAddress.getText().toString().trim();
        if (searchString.isEmpty()) return;

        Geocoder geocoder = new Geocoder(this);
        List<Address> list = null;
        try {
            list = geocoder.getFromLocationName(searchString, 1);
        } catch (IOException e) {
            Toast.makeText(this, "Error finding location", Toast.LENGTH_SHORT).show();
        }

        if (list != null && !list.isEmpty()) {
            Address address = list.get(0);
            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
            myMap.addMarker(new MarkerOptions()
                    .position(latLng)
                    .title(address.getAddressLine(0)));
            myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
            cardRouteInfo.setVisibility(View.GONE);
        } else {
            Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            myMap.setMyLocationEnabled(true);
            myMap.getUiSettings().setMyLocationButtonEnabled(false);
        }

        myMap.setOnMapClickListener(latLng -> cardRouteInfo.setVisibility(View.GONE));

        myMap.setOnPolylineClickListener(polyline -> {
            String tag = (String) polyline.getTag();
            if (tag == null) return;
            if (tag.startsWith("trip:")) {
                Trip trip = tripMap.get(tag.substring(5));
                if (trip != null) showDriverRouteInfo(trip);
            } else if (tag.startsWith("req:")) {
                PassengerRequest req = requestMap.get(tag.substring(4));
                if (req != null) showPassengerRouteInfo(req);
            }
        });

        getLastLocation();
        loadRoutesOnMap();
    }

    private void loadRoutesOnMap() {
        allTrips.clear();
        allRequests.clear();
        tripMap.clear();
        requestMap.clear();

        db.collection("trips")
                .whereGreaterThan("dateTime", System.currentTimeMillis())
                .get()
                .addOnSuccessListener(tripSnap -> {
                    for (DocumentSnapshot doc : tripSnap.getDocuments()) {
                        Trip trip = doc.toObject(Trip.class);
                        if (trip != null && trip.hasMapRoute()) {
                            allTrips.add(trip);
                            tripMap.put(trip.getTripId(), trip);
                        }
                    }

                    db.collection("requests")
                            .whereGreaterThan("dateTime", System.currentTimeMillis())
                            .get()
                            .addOnSuccessListener(reqSnap -> {
                                for (DocumentSnapshot doc : reqSnap.getDocuments()) {
                                    PassengerRequest req = doc.toObject(PassengerRequest.class);
                                    if (req != null && req.getStartLat() != 0) {
                                        allRequests.add(req);
                                        requestMap.put(req.getRequestId(), req);
                                    }
                                }
                                redrawRoutes();
                            });
                });
    }

    private void redrawRoutes() {
        if (myMap == null) return;
        myMap.clear();
        cardRouteInfo.setVisibility(View.GONE);

        Map<String, List<Object>> groups = new HashMap<>();

        if (tripsVisible) {
            for (Trip trip : allTrips) {
                String key = routeBucket(
                        trip.getStartLat(), trip.getStartLng(),
                        trip.getEndLat(),   trip.getEndLng());
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(trip);
            }
        }

        if (requestsVisible) {
            for (PassengerRequest req : allRequests) {
                String key = routeBucket(
                        req.getStartLat(), req.getStartLng(),
                        req.getEndLat(),   req.getEndLng());
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(req);
            }
        }

        for (List<Object> group : groups.values()) {
            int n = group.size();
            for (int i = 0; i < n; i++) {
                double offset = (i - (n - 1) / 2.0) * LANE_OFFSET_METERS;
                Object item = group.get(i);
                if (item instanceof Trip) {
                    drawTrip((Trip) item, offset);
                } else if (item instanceof PassengerRequest) {
                    drawRequest((PassengerRequest) item, offset);
                }
            }
        }
    }

    private void drawTrip(Trip trip, double offsetMeters) {
        List<LatLng> raw;
        String encoded = trip.getEncodedPolyline();
        if (encoded != null && !encoded.isEmpty()) {
            raw = PolylineUtils.decode(encoded);
        } else {
            raw = new ArrayList<>();
            raw.add(new LatLng(trip.getStartLat(), trip.getStartLng()));
            raw.add(new LatLng(trip.getEndLat(),   trip.getEndLng()));
        }

        List<LatLng> points = applyLaneOffset(raw, offsetMeters);

        Polyline polyline = myMap.addPolyline(new PolylineOptions()
                .width(14f)
                .color(0xFFFFAA00)
                .geodesic(false)
                .clickable(true)
                .addAll(points));
        polyline.setTag("trip:" + trip.getTripId());

        myMap.addMarker(new MarkerOptions()
                .position(points.get(0))
                .title(trip.getFromLocation())
                .snippet(trip.getDriverName())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));

        myMap.addMarker(new MarkerOptions()
                .position(points.get(points.size() - 1))
                .title(trip.getToLocation())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
    }

    private void drawRequest(PassengerRequest req, double offsetMeters) {
        List<LatLng> raw;
        String encoded = req.getEncodedPolyline();
        if (encoded != null && !encoded.isEmpty()) {
            raw = PolylineUtils.decode(encoded);
        } else {
            raw = new ArrayList<>();
            raw.add(new LatLng(req.getStartLat(), req.getStartLng()));
            raw.add(new LatLng(req.getEndLat(),   req.getEndLng()));
        }

        List<LatLng> points = applyLaneOffset(raw, offsetMeters);

        Polyline polyline = myMap.addPolyline(new PolylineOptions()
                .width(14f)
                .color(0xFF4CAF50)
                .geodesic(false)
                .clickable(true)
                .addAll(points));
        polyline.setTag("req:" + req.getRequestId());

        myMap.addMarker(new MarkerOptions()
                .position(points.get(0))
                .title(req.getFromLocation())
                .snippet(req.getPassengerName())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        myMap.addMarker(new MarkerOptions()
                .position(points.get(points.size() - 1))
                .title(req.getToLocation())
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));
    }

    private List<LatLng> applyLaneOffset(List<LatLng> points, double meters) {
        if (Math.abs(meters) < 0.01 || points.size() < 2) return points;
        List<LatLng> result = new ArrayList<>(points.size());
        for (int i = 0; i < points.size(); i++) {
            LatLng cur  = points.get(i);
            LatLng from = (i > 0)                   ? points.get(i - 1) : points.get(i + 1);
            LatLng to   = (i < points.size() - 1)   ? points.get(i + 1) : points.get(i - 1);
            double bear = bearing(from, to);
            result.add(offsetPoint(cur, (bear + 90.0) % 360.0, meters));
        }
        return result;
    }

    private double bearing(LatLng from, LatLng to) {
        double lat1 = Math.toRadians(from.latitude);
        double lat2 = Math.toRadians(to.latitude);
        double dLon = Math.toRadians(to.longitude - from.longitude);
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2)
                - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        return (Math.toDegrees(Math.atan2(y, x)) + 360.0) % 360.0;
    }

    private LatLng offsetPoint(LatLng point, double bearingDeg, double meters) {
        double R    = 6371000.0;
        double d    = meters / R;
        double lat1 = Math.toRadians(point.latitude);
        double lon1 = Math.toRadians(point.longitude);
        double b    = Math.toRadians(bearingDeg);
        double lat2 = Math.asin(
                Math.sin(lat1) * Math.cos(d)
                        + Math.cos(lat1) * Math.sin(d) * Math.cos(b));
        double lon2 = lon1 + Math.atan2(
                Math.sin(b) * Math.sin(d) * Math.cos(lat1),
                Math.cos(d) - Math.sin(lat1) * Math.sin(lat2));
        return new LatLng(Math.toDegrees(lat2), Math.toDegrees(lon2));
    }

    private String routeBucket(double sLat, double sLng, double eLat, double eLng) {
        double p = 0.008;
        String a = Math.round(sLat / p) + "," + Math.round(sLng / p);
        String b = Math.round(eLat / p) + "," + Math.round(eLng / p);
        return (a.compareTo(b) <= 0) ? a + "|" + b : b + "|" + a;
    }

    private void showDriverRouteInfo(Trip trip) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.US);
        tvRouteTypeBadge.setText("DRIVER ROUTE");
        tvRouteTypeBadge.setBackgroundColor(0xFFFFAA00);
        tvInfoRoute.setText(trip.getFromLocation() + "  →  " + trip.getToLocation());
        tvInfoDateTime.setText(sdf.format(trip.getDateTime()));
        tvInfoPrice.setText((int) trip.getPricePerSeat() + " AMD");
        tvInfoSeats.setText(trip.getSeatsAvailable() + " seat(s) left");
        tvInfoDriver.setText(trip.getDriverName() + "  ·  "
                + trip.getCarCategory() + "  ·  " + trip.getLicensePlate());
        layoutDriverInfo.setVisibility(View.VISIBLE);
        layoutPassengerInfo.setVisibility(View.GONE);
        cardRouteInfo.setVisibility(View.VISIBLE);
    }

    private void showPassengerRouteInfo(PassengerRequest req) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.US);
        tvRouteTypeBadge.setText("PASSENGER REQUEST");
        tvRouteTypeBadge.setBackgroundColor(0xFF388E3C);
        tvInfoRoute.setText(req.getFromLocation() + "  →  " + req.getToLocation());
        tvInfoReqDateTime.setText(sdf.format(req.getDateTime()));
        tvInfoMaxPrice.setText("Up to " + (int) req.getMaxPrice() + " AMD");
        tvInfoPassenger.setText(req.getPassengerName() + "  ·  " + req.getPassengerPhone());
        layoutDriverInfo.setVisibility(View.GONE);
        layoutPassengerInfo.setVisibility(View.VISIBLE);
        cardRouteInfo.setVisibility(View.VISIBLE);
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    FINE_PERMISSION_CODE);
            return;
        }
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(location -> {
            if (location != null) {
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 13));
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == FINE_PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    myMap.setMyLocationEnabled(true);
                    myMap.getUiSettings().setMyLocationButtonEnabled(false);
                }
                getLastLocation();
            } else {
                Toast.makeText(this, "Location permission is denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}