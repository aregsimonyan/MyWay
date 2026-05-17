package com.example.myway.models;

public class BookedPassenger {

    private String uid;
    private String name;
    private String email;
    private String phone;
    private double averageRating;
    private int    ratingCount;

    public BookedPassenger() {}

    public BookedPassenger(String uid, String name, String email, String phone) {
        this.uid   = uid;
        this.name  = name;
        this.email = email;
        this.phone = phone;
    }

    public String getUid()                          { return uid; }
    public void   setUid(String uid)                { this.uid = uid; }
    public String getName()                         { return name; }
    public void   setName(String name)              { this.name = name; }
    public String getEmail()                        { return email; }
    public void   setEmail(String email)            { this.email = email; }
    public String getPhone()                        { return phone; }
    public void   setPhone(String phone)            { this.phone = phone; }
    public double getAverageRating()                { return averageRating; }
    public void   setAverageRating(double r)        { this.averageRating = r; }
    public int    getRatingCount()                  { return ratingCount; }
    public void   setRatingCount(int c)             { this.ratingCount = c; }
}