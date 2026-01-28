package com.example.myway.models;

import java.util.List;

public class Trip {
    private String tripId;
    private String driverId;
    private String driverName;
    private String licensePlate;
    private String fromLocation;
    private String toLocation;
    private long dateTime;
    private double pricePerSeat;
    private int totalSeats;
    private int seatsAvailable;
    private String carCategory;
    private List<String> passengerIds;

    public Trip() {
    }

    public Trip(String tripId, String driverId, String driverName, String licensePlate, String fromLocation, String toLocation, long dateTime, double pricePerSeat, int totalSeats, String carCategory) {
        this.tripId = tripId;
        this.driverId = driverId;
        this.driverName = driverName;
        this.licensePlate = licensePlate;
        this.fromLocation = fromLocation;
        this.toLocation = toLocation;
        this.dateTime = dateTime;
        this.pricePerSeat = pricePerSeat;
        this.totalSeats = totalSeats;
        this.seatsAvailable = totalSeats;
        this.carCategory = carCategory;
    }

    public String getTripId() {
        return tripId;
    }

    public void setTripId(String tripId) {
        this.tripId = tripId;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
    }

    public String getFromLocation() {
        return fromLocation;
    }

    public void setFromLocation(String fromLocation) {
        this.fromLocation = fromLocation;
    }

    public String getToLocation() {
        return toLocation;
    }

    public void setToLocation(String toLocation) {
        this.toLocation = toLocation;
    }

    public long getDateTime() {
        return dateTime;
    }

    public void setDateTime(long dateTime) {
        this.dateTime = dateTime;
    }

    public double getPricePerSeat() {
        return pricePerSeat;
    }

    public void setPricePerSeat(double pricePerSeat) {
        this.pricePerSeat = pricePerSeat;
    }

    public int getTotalSeats() {
        return totalSeats;
    }

    public void setTotalSeats(int totalSeats) {
        this.totalSeats = totalSeats;
    }

    public int getSeatsAvailable() {
        return seatsAvailable;
    }

    public void setSeatsAvailable(int seatsAvailable) {
        this.seatsAvailable = seatsAvailable;
    }

    public String getCarCategory() {
        return carCategory;
    }

    public void setCarCategory(String carCategory) {
        this.carCategory = carCategory;
    }

    public List<String> getPassengerIds() {
        return passengerIds;
    }

    public void setPassengerIds(List<String> passengerIds) {
        this.passengerIds = passengerIds;
    }
}