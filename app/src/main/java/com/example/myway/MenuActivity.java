package com.example.myway;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.widget.ImageButton;
import android.widget.PopupMenu;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

public abstract class MenuActivity extends AppCompatActivity {
    protected void setupMoreButton(ImageButton btnMore) {
        btnMore.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenuInflater().inflate(R.menu.common_menu, popup.getMenu());

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();

                if (id == R.id.action_account) {
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
        });
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
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }


    protected void showLanguageDialog() {
        final String[] languages = {"English", "Русский", "Հայերեն"};
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