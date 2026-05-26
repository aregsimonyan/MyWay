package com.myway.myway.utils;

import android.text.InputFilter;
import android.text.Spanned;

import java.util.regex.Pattern;

public class LicensePlateUtils {

    private static final Pattern PLATE_PATTERN = Pattern.compile("^[0-9]{2}[A-Z]{2}[0-9]{3}$");

    public static InputFilter buildFilter() {
        return new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end,
                                       Spanned dest, int dstart, int dend) {
                StringBuilder filtered = new StringBuilder();
                for (int i = start; i < end; i++) {
                    char c = source.charAt(i);
                    if (Character.isDigit(c) || (Character.isLetter(c) && Character.isUpperCase(c))) {
                        filtered.append(c);
                    } else if (Character.isLetter(c) && Character.isLowerCase(c)) {
                        filtered.append(Character.toUpperCase(c));
                    }
                }
                return filtered.length() == (end - start) ? null : filtered;
            }
        };
    }

    public static InputFilter buildLengthFilter() {
        return new InputFilter.LengthFilter(7);
    }

    public static boolean isValid(String plate) {
        if (plate == null) return false;
        return PLATE_PATTERN.matcher(plate.trim()).matches();
    }

    public static String formatError() {
        return "Must be 2 digits + 2 letters + 3 digits (e.g. 35AB777)";
    }
}