package com.example.myway.models;

public class PassengerRequest {
    private String requestId;
    private String passengerId;
    private String passengerName;
    private String passengerPhone;
    private String fromLocation;
    private String toLocation;
    private double startLat;
    private double startLng;
    private double endLat;
    private double endLng;
    private double maxPrice;
    private long dateTime;
    private String encodedPolyline;

    public PassengerRequest() {
    }

    public PassengerRequest(String requestId, String passengerId, String passengerName,
                            String passengerPhone, String fromLocation, String toLocation,
                            double startLat, double startLng, double endLat, double endLng,
                            double maxPrice, long dateTime, String encodedPolyline) {
        this.requestId = requestId;
        this.passengerId = passengerId;
        this.passengerName = passengerName;
        this.passengerPhone = passengerPhone;
        this.fromLocation = fromLocation;
        this.toLocation = toLocation;
        this.startLat = startLat;
        this.startLng = startLng;
        this.endLat = endLat;
        this.endLng = endLng;
        this.maxPrice = maxPrice;
        this.dateTime = dateTime;
        this.encodedPolyline = encodedPolyline;
    }

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }

    public String getPassengerId() { return passengerId; }
    public void setPassengerId(String passengerId) { this.passengerId = passengerId; }

    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String passengerName) { this.passengerName = passengerName; }

    public String getPassengerPhone() { return passengerPhone; }
    public void setPassengerPhone(String passengerPhone) { this.passengerPhone = passengerPhone; }

    public String getFromLocation() { return fromLocation; }
    public void setFromLocation(String fromLocation) { this.fromLocation = fromLocation; }

    public String getToLocation() { return toLocation; }
    public void setToLocation(String toLocation) { this.toLocation = toLocation; }

    public double getStartLat() { return startLat; }
    public void setStartLat(double startLat) { this.startLat = startLat; }

    public double getStartLng() { return startLng; }
    public void setStartLng(double startLng) { this.startLng = startLng; }

    public double getEndLat() { return endLat; }
    public void setEndLat(double endLat) { this.endLat = endLat; }

    public double getEndLng() { return endLng; }
    public void setEndLng(double endLng) { this.endLng = endLng; }

    public double getMaxPrice() { return maxPrice; }
    public void setMaxPrice(double maxPrice) { this.maxPrice = maxPrice; }

    public long getDateTime() { return dateTime; }
    public void setDateTime(long dateTime) { this.dateTime = dateTime; }

    public String getEncodedPolyline() { return encodedPolyline; }
    public void setEncodedPolyline(String encodedPolyline) { this.encodedPolyline = encodedPolyline; }
}