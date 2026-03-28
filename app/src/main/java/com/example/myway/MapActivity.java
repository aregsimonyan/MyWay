package com.example.myway;

import android.Manifest;
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
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MapActivity extends MenuActivity implements OnMapReadyCallback {

    private GoogleMap myMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final int FINE_PERMISSION_CODE = 1;

    private EditText etSearchAddress;
    private ImageView btnSearchIcon;
    private ImageButton btnMore;
    private FloatingActionButton fabMyLocation;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        loadLocale();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        db = FirebaseFirestore.getInstance();

        etSearchAddress = findViewById(R.id.etSearchAddress);
        btnSearchIcon = findViewById(R.id.btnSearchIcon);
        btnMore = findViewById(R.id.btnMore);
        fabMyLocation = findViewById(R.id.fabMyLocation);

        cardRouteInfo = findViewById(R.id.cardRouteInfo);
        tvRouteTypeBadge = findViewById(R.id.tvRouteTypeBadge);
        tvInfoRoute = findViewById(R.id.tvInfoRoute);
        tvInfoDateTime = findViewById(R.id.tvInfoDateTime);
        tvInfoPrice = findViewById(R.id.tvInfoPrice);
        tvInfoSeats = findViewById(R.id.tvInfoSeats);
        tvInfoDriver = findViewById(R.id.tvInfoDriver);
        tvInfoReqDateTime = findViewById(R.id.tvInfoReqDateTime);
        tvInfoMaxPrice = findViewById(R.id.tvInfoMaxPrice);
        tvInfoPassenger = findViewById(R.id.tvInfoPassenger);
        layoutDriverInfo = findViewById(R.id.layoutDriverInfo);
        layoutPassengerInfo = findViewById(R.id.layoutPassengerInfo);
        btnCloseInfo = findViewById(R.id.btnCloseInfo);

        btnCloseInfo.setOnClickListener(v -> cardRouteInfo.setVisibility(View.GONE));

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        initSearchWidgets();
        setupMoreButton(btnMore);
    }

    private void initSearchWidgets() {
        etSearchAddress.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && event.getAction() == KeyEvent.ACTION_DOWN
                        && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    geoLocate();
                    return true;
                }
                return false;
            }
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
        tripMap.clear();
        requestMap.clear();

        db.collection("trips")
                .whereGreaterThan("dateTime", System.currentTimeMillis())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot querySnapshot) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            Trip trip = doc.toObject(Trip.class);
                            if (trip == null || !trip.hasMapRoute()) continue;

                            LatLng start = new LatLng(trip.getStartLat(), trip.getStartLng());
                            LatLng end = new LatLng(trip.getEndLat(), trip.getEndLng());

                            PolylineOptions opts = new PolylineOptions()
                                    .width(14f)
                                    .color(0xFFFFAA00)
                                    .geodesic(false)
                                    .clickable(true);

                            String encoded = trip.getEncodedPolyline();
                            if (encoded != null && !encoded.isEmpty()) {
                                opts.addAll(PolylineUtils.decode(encoded));
                            } else {
                                opts.add(start, end);
                            }

                            Polyline polyline = myMap.addPolyline(opts);
                            polyline.setTag("trip:" + trip.getTripId());
                            tripMap.put(trip.getTripId(), trip);

                            myMap.addMarker(new MarkerOptions()
                                    .position(start)
                                    .title(trip.getFromLocation())
                                    .snippet(trip.getDriverName())
                                    .icon(BitmapDescriptorFactory.defaultMarker(
                                            BitmapDescriptorFactory.HUE_YELLOW)));

                            myMap.addMarker(new MarkerOptions()
                                    .position(end)
                                    .title(trip.getToLocation())
                                    .icon(BitmapDescriptorFactory.defaultMarker(
                                            BitmapDescriptorFactory.HUE_ORANGE)));
                        }
                    }
                });

        db.collection("requests")
                .whereGreaterThan("dateTime", System.currentTimeMillis())
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot querySnapshot) {
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            PassengerRequest req = doc.toObject(PassengerRequest.class);
                            if (req == null || req.getStartLat() == 0) continue;

                            LatLng start = new LatLng(req.getStartLat(), req.getStartLng());
                            LatLng end = new LatLng(req.getEndLat(), req.getEndLng());

                            PolylineOptions opts = new PolylineOptions()
                                    .width(14f)
                                    .color(0xFF4CAF50)
                                    .geodesic(false)
                                    .clickable(true);

                            String encoded = req.getEncodedPolyline();
                            if (encoded != null && !encoded.isEmpty()) {
                                opts.addAll(PolylineUtils.decode(encoded));
                            } else {
                                opts.add(start, end);
                            }

                            Polyline polyline = myMap.addPolyline(opts);
                            polyline.setTag("req:" + req.getRequestId());
                            requestMap.put(req.getRequestId(), req);

                            myMap.addMarker(new MarkerOptions()
                                    .position(start)
                                    .title(req.getFromLocation())
                                    .snippet(req.getPassengerName())
                                    .icon(BitmapDescriptorFactory.defaultMarker(
                                            BitmapDescriptorFactory.HUE_GREEN)));

                            myMap.addMarker(new MarkerOptions()
                                    .position(end)
                                    .title(req.getToLocation())
                                    .icon(BitmapDescriptorFactory.defaultMarker(
                                            BitmapDescriptorFactory.HUE_CYAN)));
                        }
                    }
                });
    }

    private void showDriverRouteInfo(Trip trip) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.US);
        tvRouteTypeBadge.setText("DRIVER ROUTE");
        tvRouteTypeBadge.setBackgroundColor(0xFFFFAA00);
        tvInfoRoute.setText(trip.getFromLocation() + "  →  " + trip.getToLocation());
        tvInfoDateTime.setText(sdf.format(trip.getDateTime()));
        tvInfoPrice.setText((int) trip.getPricePerSeat() + " AMD");
        tvInfoSeats.setText(trip.getSeatsAvailable() + " seat(s) left");
        tvInfoDriver.setText(trip.getDriverName() + "  ·  " + trip.getCarCategory()
                + "  ·  " + trip.getLicensePlate());
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
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_PERMISSION_CODE);
            return;
        }

        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 13));
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == FINE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
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