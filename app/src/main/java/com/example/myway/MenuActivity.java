package com.example.myway;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Locale;

public abstract class MenuActivity extends AppCompatActivity {

    protected FirebaseAuth mAuth = FirebaseAuth.getInstance();
    protected FirebaseFirestore db = FirebaseFirestore.getInstance();

    protected void setupMoreButton(ImageButton btnMore) {
        btnMore.setOnClickListener(v -> {
            if (mAuth.getCurrentUser() == null) return;
            String uid = mAuth.getCurrentUser().getUid();
            db.collection("users").document(uid).get()
                    .addOnSuccessListener(doc -> buildAndShowMenu(v, doc))
                    .addOnFailureListener(e -> buildAndShowMenu(v, null));
        });
    }

    private void buildAndShowMenu(View anchor, DocumentSnapshot doc) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.common_menu, popup.getMenu());

        if (doc != null && doc.exists()) {
            List<String> roles = (List<String>) doc.get("roles");
            String activeRole  = doc.getString("activeRole");
            if (activeRole == null) activeRole = doc.getString("userType");

            boolean hasBothRoles = roles != null && roles.size() >= 2;

            if (hasBothRoles) {
                String targetRole = "Driver".equals(activeRole) ? "Passenger" : "Driver";
                MenuItem switchItem = popup.getMenu().findItem(R.id.action_switch_role);
                switchItem.setTitle("Switch to " + targetRole);
                switchItem.setVisible(true);
                popup.getMenu().findItem(R.id.action_add_role).setVisible(false);
            } else {
                String currentRole = (roles != null && !roles.isEmpty()) ? roles.get(0) : activeRole;
                String roleToAdd   = "Driver".equals(currentRole) ? "Passenger" : "Driver";
                MenuItem addItem = popup.getMenu().findItem(R.id.action_add_role);
                addItem.setTitle("Add " + roleToAdd + " Account");
                addItem.setVisible(true);
                popup.getMenu().findItem(R.id.action_switch_role).setVisible(false);
            }
        }

        final String finalActiveRole = (doc != null && doc.exists())
                ? resolveActiveRole(doc)
                : null;

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();

            if (id == R.id.action_edit_profile) {
                startActivity(new Intent(MenuActivity.this, EditProfileActivity.class));
                return true;
            } else if (id == R.id.action_switch_role) {
                String target = "Driver".equals(finalActiveRole) ? "Passenger" : "Driver";
                confirmSwitchRole(target);
                return true;
            } else if (id == R.id.action_add_role) {
                String current = (doc != null && doc.exists())
                        ? resolveActiveRole(doc) : "Passenger";
                String toAdd = "Driver".equals(current) ? "Passenger" : "Driver";
                Intent intent = new Intent(MenuActivity.this, AddRoleActivity.class);
                intent.putExtra("roleToAdd", toAdd);
                startActivity(intent);
                return true;
            } else if (id == R.id.action_account) {
                startActivity(new Intent(MenuActivity.this, ProfileActivity.class));
                return true;
            } else if (id == R.id.action_language) {
                showLanguageDialog();
                return true;
            } else if (id == R.id.action_logout) {
                showLogoutConfirmation();
                return true;
            }
            return false;
        });

        popup.show();
    }

    private String resolveActiveRole(DocumentSnapshot doc) {
        String activeRole = doc.getString("activeRole");
        if (activeRole == null) activeRole = doc.getString("userType");
        return activeRole;
    }

    private void confirmSwitchRole(String targetRole) {
        new AlertDialog.Builder(this)
                .setTitle("Switch Account")
                .setMessage("Switch to your " + targetRole + " account?")
                .setPositiveButton("Switch", (dialog, which) -> switchToRole(targetRole))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void switchToRole(String newRole) {
        if (mAuth.getCurrentUser() == null) return;
        String uid = mAuth.getCurrentUser().getUid();

        db.collection("users").document(uid)
                .update("activeRole", newRole)
                .addOnSuccessListener(aVoid -> {
                    Intent intent = "Driver".equals(newRole)
                            ? new Intent(MenuActivity.this, DriverHomeActivity.class)
                            : new Intent(MenuActivity.this, PassengerHomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Could not switch account. Please try again.",
                                Toast.LENGTH_SHORT).show());
    }

    protected void showLogoutConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Yes", (dialog, which) -> performLogout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void performLogout() {
        FirebaseAuth.getInstance().signOut();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
        googleSignInClient.signOut();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    protected void showLanguageDialog() {
        final String[] languages     = {"English", "Русский", "Հայերեն"};
        final String[] languageCodes = {"en", "ru", "hy"};

        int currentSelection = getCurrentLanguageIndex(languageCodes);

        new AlertDialog.Builder(this)
                .setTitle("Select Language")
                .setSingleChoiceItems(languages, currentSelection, (dialog, which) -> {
                    applyLocale(languageCodes[which]);
                    dialog.dismiss();
                    recreate();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int getCurrentLanguageIndex(String[] codes) {
        SharedPreferences prefs = getSharedPreferences("LanguagePrefs", MODE_PRIVATE);
        String currentLang = prefs.getString("language", "en");
        for (int i = 0; i < codes.length; i++) {
            if (codes[i].equals(currentLang)) return i;
        }
        return 0;
    }

    protected void loadLocale() {
        SharedPreferences prefs = getSharedPreferences("LanguagePrefs", MODE_PRIVATE);
        String languageCode = prefs.getString("language", "en");
        applyLocale(languageCode);
    }

    private void applyLocale(String languageCode) {
        SharedPreferences prefs = getSharedPreferences("LanguagePrefs", MODE_PRIVATE);
        prefs.edit().putString("language", languageCode).apply();

        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(locale);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }
}