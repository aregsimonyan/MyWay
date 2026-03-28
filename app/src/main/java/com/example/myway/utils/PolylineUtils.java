package com.example.myway.utils;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class PolylineUtils {

    public static List<LatLng> decode(String encoded) {
        List<LatLng> result = new ArrayList<>();
        int index = 0;
        int len = encoded.length();
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int b;
            int shift = 0;
            int rawResult = 0;

            do {
                b = encoded.charAt(index++) - 63;
                rawResult |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            int dlat = ((rawResult & 1) != 0 ? ~(rawResult >> 1) : (rawResult >> 1));
            lat += dlat;

            shift = 0;
            rawResult = 0;

            do {
                b = encoded.charAt(index++) - 63;
                rawResult |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);

            int dlng = ((rawResult & 1) != 0 ? ~(rawResult >> 1) : (rawResult >> 1));
            lng += dlng;

            result.add(new LatLng(lat / 1E5, lng / 1E5));
        }

        return result;
    }
}