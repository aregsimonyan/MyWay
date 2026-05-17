package com.example.myway;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
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
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapActivity extends MenuActivity implements OnMapReadyCallback {

    private static final int FINE_PERMISSION_CODE = 1;
    private static final double LANE_OFFSET_METERS = 22.0;

    private static final int[] TRIP_COLORS = {
            0xFFFFD600, 0xFFFF6D00, 0xFFE91E63, 0xFF00BCD4, 0xFF7C4DFF,
            0xFF76FF03, 0xFFFF1744, 0xFF00E5FF, 0xFFFFAB00, 0xFF69F0AE
    };

    private static final int[] REQUEST_COLORS = {
            0xFF00E676, 0xFF40C4FF, 0xFFEA80FC, 0xFFCCFF90, 0xFF80D8FF,
            0xFFB9F6CA, 0xFFCE93D8, 0xFF80CBC4, 0xFFA5D6A7, 0xFF90CAF9
    };

    private GoogleMap myMap;
    private FusedLocationProviderClient fusedLocationProviderClient;

    private EditText etSearchAddress;
    private ImageView btnSearchIcon;
    private ImageButton btnMore;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabMyLocation;
    private MaterialButton btnToggleTrips;
    private MaterialButton btnToggleRequests;
    private MaterialCardView layerToggleCard;

    private CardView cardRouteInfo;
    private TextView tvRouteTypeBadge;
    private TextView tvInfoRoute;
    private TextView tvInfoDateTime;
    private TextView tvInfoPrice;
    private TextView tvInfoSeats;
    private TextView tvInfoDriver;
    private TextView tvInfoCarModel;
    private TextView tvInfoCarCategory;
    private TextView tvInfoLicensePlate;
    private TextView tvInfoReqDateTime;
    private TextView tvInfoMaxPrice;
    private TextView tvInfoPassenger;
    private TextView tvInfoPassengerPhone;
    private LinearLayout layoutDriverInfo;
    private LinearLayout layoutPassengerInfo;
    private ImageButton btnCloseInfo;
    private MaterialButton btnBookFromMap;
    private MaterialButton btnShowAllRoutes;
    private MaterialButton btnShowAllRequestRoutes;
    private MaterialButton btnCallPassenger;
    private MaterialButton btnMessagePassenger;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private final Map<String, Trip> tripMap = new HashMap<>();
    private final Map<String, PassengerRequest> requestMap = new HashMap<>();
    private final List<Trip> allTrips = new ArrayList<>();
    private final List<PassengerRequest> allRequests = new ArrayList<>();

    private boolean showingTrips = true;

    private String focusedTripId = null;
    private String isolatedTripId = null;
    private String isolatedRequestId = null;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        loadLocale();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        db    = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        focusedTripId = getIntent().getStringExtra("tripId");

        etSearchAddress        = findViewById(R.id.etSearchAddress);
        btnSearchIcon          = findViewById(R.id.btnSearchIcon);
        btnMore                = findViewById(R.id.btnMore);
        fabMyLocation          = findViewById(R.id.fabMyLocation);
        btnToggleTrips         = findViewById(R.id.btnToggleTrips);
        btnToggleRequests      = findViewById(R.id.btnToggleRequests);
        layerToggleCard        = findViewById(R.id.layerToggleCard);
        cardRouteInfo          = findViewById(R.id.cardRouteInfo);
        tvRouteTypeBadge       = findViewById(R.id.tvRouteTypeBadge);
        tvInfoRoute            = findViewById(R.id.tvInfoRoute);
        tvInfoDateTime         = findViewById(R.id.tvInfoDateTime);
        tvInfoPrice            = findViewById(R.id.tvInfoPrice);
        tvInfoSeats            = findViewById(R.id.tvInfoSeats);
        tvInfoDriver           = findViewById(R.id.tvInfoDriver);
        tvInfoCarModel         = findViewById(R.id.tvInfoCarModel);
        tvInfoCarCategory      = findViewById(R.id.tvInfoCarCategory);
        tvInfoLicensePlate     = findViewById(R.id.tvInfoLicensePlate);
        tvInfoReqDateTime      = findViewById(R.id.tvInfoReqDateTime);
        tvInfoMaxPrice         = findViewById(R.id.tvInfoMaxPrice);
        tvInfoPassenger        = findViewById(R.id.tvInfoPassenger);
        tvInfoPassengerPhone   = findViewById(R.id.tvInfoPassengerPhone);
        layoutDriverInfo       = findViewById(R.id.layoutDriverInfo);
        layoutPassengerInfo    = findViewById(R.id.layoutPassengerInfo);
        btnCloseInfo           = findViewById(R.id.btnCloseInfo);
        btnBookFromMap         = findViewById(R.id.btnBookFromMap);
        btnShowAllRoutes       = findViewById(R.id.btnShowAllRoutes);
        btnShowAllRequestRoutes = findViewById(R.id.btnShowAllRequestRoutes);
        btnCallPassenger       = findViewById(R.id.btnCallPassenger);
        btnMessagePassenger    = findViewById(R.id.btnMessagePassenger);

        btnCloseInfo.setOnClickListener(v -> {
            if (isolatedTripId != null || isolatedRequestId != null) {
                exitIsolationMode();
            } else {
                cardRouteInfo.setVisibility(View.GONE);
            }
        });

        btnShowAllRoutes.setOnClickListener(v -> exitIsolationMode());
        btnShowAllRequestRoutes.setOnClickListener(v -> exitIsolationMode());

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) mapFragment.getMapAsync(this);

        if (focusedTripId != null) {
            layerToggleCard.setVisibility(View.GONE);
            etSearchAddress.setHint("Single trip view");
            etSearchAddress.setEnabled(false);
        } else {
            initSearchWidgets();
            setupToggleButtons();
        }

        setupMoreButton(btnMore);
    }

    private void exitIsolationMode() {
        isolatedTripId = null;
        isolatedRequestId = null;
        btnShowAllRoutes.setVisibility(View.GONE);
        btnShowAllRequestRoutes.setVisibility(View.GONE);
        btnBookFromMap.setVisibility(View.GONE);
        btnCallPassenger.setVisibility(View.GONE);
        btnMessagePassenger.setVisibility(View.GONE);
        cardRouteInfo.setVisibility(View.GONE);
        redrawRoutes();
    }

    private void setupToggleButtons() {
        applyActiveStyle(btnToggleTrips, 0xFFFFD600);
        applyInactiveStyle(btnToggleRequests);

        btnToggleTrips.setOnClickListener(v -> {
            if (showingTrips) return;
            showingTrips = true;
            applyActiveStyle(btnToggleTrips, 0xFFFFD600);
            applyInactiveStyle(btnToggleRequests);
            redrawRoutes();
        });

        btnToggleRequests.setOnClickListener(v -> {
            if (!showingTrips) return;
            showingTrips = false;
            applyActiveStyle(btnToggleRequests, 0xFF00E676);
            applyInactiveStyle(btnToggleTrips);
            redrawRoutes();
        });
    }

    private void applyActiveStyle(MaterialButton btn, int color) {
        btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        btn.setTextColor(0xFF111111);
        btn.setStrokeColor(android.content.res.ColorStateList.valueOf(color));
        btn.setAlpha(1f);
    }

    private void applyInactiveStyle(MaterialButton btn) {
        btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF2A2A2A));
        btn.setTextColor(0xFF888888);
        btn.setStrokeColor(android.content.res.ColorStateList.valueOf(0xFF444444));
        btn.setAlpha(0.65f);
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

        myMap.setOnMapClickListener(latLng -> {
            if (isolatedTripId != null || isolatedRequestId != null) {
                exitIsolationMode();
            } else {
                cardRouteInfo.setVisibility(View.GONE);
            }
        });

        myMap.setOnPolylineClickListener(polyline -> {
            String tag = (String) polyline.getTag();
            if (tag == null) return;

            if (tag.startsWith("trip:")) {
                String id = tag.substring(5);
                Trip trip = tripMap.get(id);
                if (trip == null) return;

                if (focusedTripId != null) {
                    showDriverRouteInfo(trip, false);
                } else {
                    isolateTripOnMap(trip);
                }

            } else if (tag.startsWith("req:")) {
                PassengerRequest req = requestMap.get(tag.substring(4));
                if (req != null) {
                    isolateRequestOnMap(req);
                }
            }
        });

        getLastLocation();

        if (focusedTripId != null) {
            loadSingleTrip(focusedTripId);
        } else {
            loadRoutesOnMap();
        }
    }

    private void isolateTripOnMap(Trip trip) {
        isolatedTripId = trip.getTripId();

        myMap.clear();
        int color = colorForId(trip.getTripId(), TRIP_COLORS);
        drawTripWithColor(trip, 0, color);

        LatLngBounds.Builder builder = new LatLngBounds.Builder()
                .include(new LatLng(trip.getStartLat(), trip.getStartLng()))
                .include(new LatLng(trip.getEndLat(), trip.getEndLng()));
        myMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 140));

        showDriverRouteInfo(trip, true);
    }

    private void isolateRequestOnMap(PassengerRequest req) {
        isolatedRequestId = req.getRequestId();

        myMap.clear();
        int color = colorForId(req.getRequestId(), REQUEST_COLORS);
        drawRequestWithColor(req, 0, color);

        LatLngBounds.Builder builder = new LatLngBounds.Builder()
                .include(new LatLng(req.getStartLat(), req.getStartLng()))
                .include(new LatLng(req.getEndLat(), req.getEndLng()));
        myMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 140));

        showPassengerRouteInfo(req, true);
    }

    private void loadSingleTrip(String tripId) {
        db.collection("trips").document(tripId).get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) return;
                    Trip trip = doc.toObject(Trip.class);
                    if (trip == null) return;
                    allTrips.clear();
                    tripMap.clear();
                    allTrips.add(trip);
                    tripMap.put(trip.getTripId(), trip);
                    showingTrips = true;
                    redrawRoutes();
                    if (trip.hasMapRoute()) {
                        LatLngBounds bounds = new LatLngBounds.Builder()
                                .include(new LatLng(trip.getStartLat(), trip.getStartLng()))
                                .include(new LatLng(trip.getEndLat(), trip.getEndLng()))
                                .build();
                        myMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 140));
                    }
                    showDriverRouteInfo(trip, false);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not load trip.", Toast.LENGTH_SHORT).show());
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

        if (showingTrips) {
            Map<String, List<Trip>> groups = new HashMap<>();
            for (Trip trip : allTrips) {
                String key = routeBucket(trip.getStartLat(), trip.getStartLng(),
                        trip.getEndLat(), trip.getEndLng());
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(trip);
            }
            for (List<Trip> group : groups.values()) {
                int n = group.size();
                for (int i = 0; i < n; i++) {
                    double offset = (i - (n - 1) / 2.0) * LANE_OFFSET_METERS;
                    Trip trip = group.get(i);
                    int color = colorForId(trip.getTripId(), TRIP_COLORS);
                    drawTripWithColor(trip, offset, color);
                }
            }
        } else {
            Map<String, List<PassengerRequest>> groups = new HashMap<>();
            for (PassengerRequest req : allRequests) {
                String key = routeBucket(req.getStartLat(), req.getStartLng(),
                        req.getEndLat(), req.getEndLng());
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(req);
            }
            for (List<PassengerRequest> group : groups.values()) {
                int n = group.size();
                for (int i = 0; i < n; i++) {
                    double offset = (i - (n - 1) / 2.0) * LANE_OFFSET_METERS;
                    PassengerRequest req = group.get(i);
                    int color = colorForId(req.getRequestId(), REQUEST_COLORS);
                    drawRequestWithColor(req, offset, color);
                }
            }
        }
    }

    private int colorForId(String id, int[] palette) {
        return palette[Math.abs(id.hashCode()) % palette.length];
    }

    private float markerHueForColor(int argbColor) {
        float[] hsv = new float[3];
        android.graphics.Color.RGBToHSV(
                (argbColor >> 16) & 0xFF,
                (argbColor >> 8) & 0xFF,
                argbColor & 0xFF, hsv);
        return hsv[0];
    }

    private void drawTripWithColor(Trip trip, double offsetMeters, int color) {
        String encoded = trip.getEncodedPolyline();
        List<LatLng> raw = (encoded != null && !encoded.isEmpty())
                ? PolylineUtils.decode(encoded)
                : defaultLine(trip.getStartLat(), trip.getStartLng(), trip.getEndLat(), trip.getEndLng());

        List<LatLng> points = applyLaneOffset(raw, offsetMeters);
        float hue = markerHueForColor(color);

        Polyline polyline = myMap.addPolyline(new PolylineOptions()
                .width(16f).color(color).geodesic(false).clickable(true).addAll(points));
        polyline.setTag("trip:" + trip.getTripId());

        myMap.addMarker(new MarkerOptions().position(points.get(0))
                .title(trip.getFromLocation()).snippet(trip.getDriverName())
                .icon(BitmapDescriptorFactory.defaultMarker(hue)));
        myMap.addMarker(new MarkerOptions().position(points.get(points.size() - 1))
                .title(trip.getToLocation())
                .icon(BitmapDescriptorFactory.defaultMarker(hue)));
    }

    private void drawRequestWithColor(PassengerRequest req, double offsetMeters, int color) {
        String encoded = req.getEncodedPolyline();
        List<LatLng> raw = (encoded != null && !encoded.isEmpty())
                ? PolylineUtils.decode(encoded)
                : defaultLine(req.getStartLat(), req.getStartLng(), req.getEndLat(), req.getEndLng());

        List<LatLng> points = applyLaneOffset(raw, offsetMeters);
        float hue = markerHueForColor(color);

        Polyline polyline = myMap.addPolyline(new PolylineOptions()
                .width(16f).color(color).geodesic(false).clickable(true).addAll(points));
        polyline.setTag("req:" + req.getRequestId());

        myMap.addMarker(new MarkerOptions().position(points.get(0))
                .title(req.getFromLocation()).snippet(req.getPassengerName())
                .icon(BitmapDescriptorFactory.defaultMarker(hue)));
        myMap.addMarker(new MarkerOptions().position(points.get(points.size() - 1))
                .title(req.getToLocation())
                .icon(BitmapDescriptorFactory.defaultMarker(hue)));
    }

    private List<LatLng> defaultLine(double sLat, double sLng, double eLat, double eLng) {
        List<LatLng> list = new ArrayList<>();
        list.add(new LatLng(sLat, sLng));
        list.add(new LatLng(eLat, eLng));
        return list;
    }

    private void showDriverRouteInfo(Trip trip, boolean showActions) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.US);

        tvRouteTypeBadge.setText("DRIVER ROUTE");
        tvRouteTypeBadge.setBackgroundColor(0xFFFFAA00);
        tvInfoRoute.setText(trip.getFromLocation() + "  →  " + trip.getToLocation());
        tvInfoDateTime.setText(sdf.format(trip.getDateTime()));
        tvInfoPrice.setText((int) trip.getPricePerSeat() + " AMD");
        tvInfoSeats.setText(trip.getSeatsAvailable() + " seat(s) left");
        tvInfoDriver.setText(trip.getDriverName());
        tvInfoCarModel.setText(trip.getCarModel() != null && !trip.getCarModel().isEmpty()
                ? trip.getCarModel() : "—");
        tvInfoCarCategory.setText(trip.getCarCategory() != null ? trip.getCarCategory() : "—");
        tvInfoLicensePlate.setText(trip.getLicensePlate() != null ? trip.getLicensePlate() : "—");

        if (showActions) {
            String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
            boolean alreadyBooked = trip.getPassengerIds() != null
                    && trip.getPassengerIds().contains(uid);

            btnShowAllRoutes.setVisibility(View.VISIBLE);

            if (alreadyBooked) {
                btnBookFromMap.setVisibility(View.VISIBLE);
                btnBookFromMap.setText("✓ Booked");
                btnBookFromMap.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFF2E7D32));
                btnBookFromMap.setTextColor(0xFFFFFFFF);
                btnBookFromMap.setEnabled(false);
            } else if (trip.getSeatsAvailable() <= 0) {
                btnBookFromMap.setVisibility(View.VISIBLE);
                btnBookFromMap.setText("Full");
                btnBookFromMap.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFF888888));
                btnBookFromMap.setTextColor(0xFFFFFFFF);
                btnBookFromMap.setEnabled(false);
            } else {
                btnBookFromMap.setVisibility(View.VISIBLE);
                btnBookFromMap.setText("Book This Trip");
                btnBookFromMap.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(0xFFEEEEEE));
                btnBookFromMap.setTextColor(0xFF111111);
                btnBookFromMap.setEnabled(true);
                btnBookFromMap.setOnClickListener(v -> confirmAndBook(trip));
            }
        } else {
            btnShowAllRoutes.setVisibility(View.GONE);
            btnBookFromMap.setVisibility(View.GONE);
        }

        btnShowAllRequestRoutes.setVisibility(View.GONE);
        btnCallPassenger.setVisibility(View.GONE);
        btnMessagePassenger.setVisibility(View.GONE);

        layoutDriverInfo.setVisibility(View.VISIBLE);
        layoutPassengerInfo.setVisibility(View.GONE);
        cardRouteInfo.setVisibility(View.VISIBLE);
    }

    private void showPassengerRouteInfo(PassengerRequest req, boolean showActions) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.US);

        tvRouteTypeBadge.setText("PASSENGER REQUEST");
        tvRouteTypeBadge.setBackgroundColor(0xFF388E3C);
        tvInfoRoute.setText(req.getFromLocation() + "  →  " + req.getToLocation());
        tvInfoReqDateTime.setText(sdf.format(req.getDateTime()));
        tvInfoMaxPrice.setText("Up to " + (int) req.getMaxPrice() + " AMD");
        tvInfoPassenger.setText(req.getPassengerName());

        String phone = req.getPassengerPhone();
        tvInfoPassengerPhone.setText(phone != null && !phone.isEmpty() ? phone : "—");

        if (showActions) {
            btnShowAllRequestRoutes.setVisibility(View.VISIBLE);

            if (phone != null && !phone.isEmpty()) {
                btnCallPassenger.setVisibility(View.VISIBLE);
                btnCallPassenger.setOnClickListener(v -> {
                    Intent callIntent = new Intent(Intent.ACTION_DIAL);
                    callIntent.setData(Uri.parse("tel:" + phone));
                    startActivity(callIntent);
                });

                btnMessagePassenger.setVisibility(View.VISIBLE);
                btnMessagePassenger.setOnClickListener(v -> {
                    Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                    smsIntent.setData(Uri.parse("sms:" + phone));
                    startActivity(smsIntent);
                });
            } else {
                btnCallPassenger.setVisibility(View.GONE);
                btnMessagePassenger.setVisibility(View.GONE);
            }
        } else {
            btnShowAllRequestRoutes.setVisibility(View.GONE);
            btnCallPassenger.setVisibility(View.GONE);
            btnMessagePassenger.setVisibility(View.GONE);
        }

        btnShowAllRoutes.setVisibility(View.GONE);
        btnBookFromMap.setVisibility(View.GONE);

        layoutDriverInfo.setVisibility(View.GONE);
        layoutPassengerInfo.setVisibility(View.VISIBLE);
        cardRouteInfo.setVisibility(View.VISIBLE);
    }

    private void confirmAndBook(Trip trip) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Confirm Booking")
                .setMessage("Book a seat from " + trip.getFromLocation()
                        + " to " + trip.getToLocation()
                        + " for " + (int) trip.getPricePerSeat() + " AMD?")
                .setPositiveButton("Book Now", (dialog, which) -> executeBooking(trip))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void executeBooking(Trip trip) {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Please log in to book.", Toast.LENGTH_SHORT).show();
            return;
        }

        btnBookFromMap.setEnabled(false);
        btnBookFromMap.setText("Booking...");

        final DocumentReference tripRef = db.collection("trips").document(trip.getTripId());
        final String passengerId = mAuth.getCurrentUser().getUid();

        db.runTransaction(new Transaction.Function<Void>() {
            @Override
            public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                DocumentSnapshot snapshot = transaction.get(tripRef);
                long newSeats = snapshot.getLong("seatsAvailable") - 1;
                if (newSeats < 0) {
                    throw new FirebaseFirestoreException("Trip is full",
                            FirebaseFirestoreException.Code.ABORTED);
                }
                transaction.update(tripRef, "seatsAvailable", newSeats);
                transaction.update(tripRef, "passengerIds", FieldValue.arrayUnion(passengerId));
                return null;
            }
        }).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Intent intent = new Intent(MapActivity.this, BookedTripActivity.class);
                intent.putExtra("tripId", trip.getTripId());
                startActivity(intent);
                exitIsolationMode();
                loadRoutesOnMap();
            } else {
                btnBookFromMap.setEnabled(true);
                btnBookFromMap.setText("Book This Trip");
                Toast.makeText(MapActivity.this,
                        "Booking failed: " + task.getException().getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<LatLng> applyLaneOffset(List<LatLng> points, double meters) {
        if (Math.abs(meters) < 0.01 || points.size() < 2) return points;
        List<LatLng> result = new ArrayList<>(points.size());
        for (int i = 0; i < points.size(); i++) {
            LatLng cur  = points.get(i);
            LatLng from = (i > 0) ? points.get(i - 1) : points.get(i + 1);
            LatLng to   = (i < points.size() - 1) ? points.get(i + 1) : points.get(i - 1);
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
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        return (Math.toDegrees(Math.atan2(y, x)) + 360.0) % 360.0;
    }

    private LatLng offsetPoint(LatLng point, double bearingDeg, double meters) {
        double R    = 6371000.0;
        double d    = meters / R;
        double lat1 = Math.toRadians(point.latitude);
        double lon1 = Math.toRadians(point.longitude);
        double b    = Math.toRadians(bearingDeg);
        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(d)
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
            if (location != null && focusedTripId == null && isolatedTripId == null
                    && isolatedRequestId == null) {
                myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(location.getLatitude(), location.getLongitude()), 13));
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == FINE_PERMISSION_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                myMap.setMyLocationEnabled(true);
                myMap.getUiSettings().setMyLocationButtonEnabled(false);
            }
            getLastLocation();
        } else {
            Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show();
        }
    }
}