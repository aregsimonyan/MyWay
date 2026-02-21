package com.example.myway;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap myMap;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private static final int FINE_PERMISSION_CODE = 1;

    private EditText etSearchAddress;
    private ImageView btnSearchIcon;
    private ImageButton btnMore;
    private FloatingActionButton fabMyLocation;

    private SharedPreferences languagePrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        languagePrefs = getSharedPreferences("LanguagePrefs", MODE_PRIVATE);
        loadLocale();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        etSearchAddress = findViewById(R.id.etSearchAddress);
        btnSearchIcon = findViewById(R.id.btnSearchIcon);
        btnMore = findViewById(R.id.btnMore);
        fabMyLocation = findViewById(R.id.fabMyLocation);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        initSearchWidgets();
        setupMoreButton();
    }

    private void initSearchWidgets() {
        etSearchAddress.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || (event != null && event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    geoLocate();
                    return true;
                }
                return false;
            }
        });

        btnSearchIcon.setOnClickListener(v -> geoLocate());
        fabMyLocation.setOnClickListener(v -> getLastLocation());
    }

    private void setupMoreButton() {
        btnMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popup = new PopupMenu(MapActivity.this, v);
                popup.getMenuInflater().inflate(R.menu.common_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int id = item.getItemId();
                        if (id == R.id.action_account) {
                            startActivity(new Intent(MapActivity.this, ProfileActivity.class));
                            return true;
                        } else if (id == R.id.action_language) {
                            showLanguageDialog();
                            return true;
                        } else if (id == R.id.action_logout) {
                            showLogoutConfirmation();
                            return true;
                        }
                        return false;
                    }
                });
                popup.show();
            }
        });
    }

    private void geoLocate() {
        String searchString = etSearchAddress.getText().toString();
        if (searchString.isEmpty()) return;

        Geocoder geocoder = new Geocoder(MapActivity.this);
        List<Address> list = null;

        try {
            list = geocoder.getFromLocationName(searchString, 1);
        } catch (IOException e) {
            Toast.makeText(this, "Error finding location", Toast.LENGTH_SHORT).show();
        }

        if (list != null && !list.isEmpty()) {
            Address address = list.get(0);
            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());
            myMap.clear();
            myMap.addMarker(new MarkerOptions().position(latLng).title(address.getAddressLine(0)));
            myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f));
        } else {
            Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        myMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            myMap.setMyLocationEnabled(true);
            myMap.getUiSettings().setMyLocationButtonEnabled(false);
        }

        getLastLocation();
    }

    private void getLastLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_PERMISSION_CODE);
            return;
        }

        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    myMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15));
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == FINE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    myMap.setMyLocationEnabled(true);
                    myMap.getUiSettings().setMyLocationButtonEnabled(false);
                }
                getLastLocation();
            } else {
                Toast.makeText(this, "Location permission is denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showLanguageDialog() {
        final String[] languages = {"English", "Русский", "Հայերեն"};
        final String[] languageCodes = {"en", "ru", "hy"};

        int currentSelection = getCurrentLanguageIndex(languageCodes);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Language");
        builder.setSingleChoiceItems(languages, currentSelection, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setLocale(languageCodes[which]);
                dialog.dismiss();
                recreate();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.create().show();
    }

    private int getCurrentLanguageIndex(String[] codes) {
        String currentLang = languagePrefs.getString("language", "en");
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(currentLang)) return i;
        }
        return 0;
    }

    private void setLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(locale);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        SharedPreferences.Editor editor = languagePrefs.edit();
        editor.putString("language", languageCode);
        editor.apply();
    }

    private void loadLocale() {
        String languageCode = languagePrefs.getString("language", "en");
        setLocale(languageCode);
    }

    private void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        performLogout();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}