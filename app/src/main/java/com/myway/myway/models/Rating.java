package com.myway.myway.models;

public class Rating {
    private String ratingId;
    private String tripId;
    private String raterId;
    private String ratedUserId;
    private String raterType;
    private float  stars;
    private long   createdAt;

    public Rating() {}

    public Rating(String ratingId, String tripId, String raterId,
                  String ratedUserId, String raterType, float stars) {
        this.ratingId     = ratingId;
        this.tripId       = tripId;
        this.raterId      = raterId;
        this.ratedUserId  = ratedUserId;
        this.raterType    = raterType;
        this.stars        = stars;
        this.createdAt    = System.currentTimeMillis();
    }

    public String getRatingId()                   { return ratingId; }
    public void   setRatingId(String ratingId)    { this.ratingId = ratingId; }
    public String getTripId()                     { return tripId; }
    public void   setTripId(String tripId)        { this.tripId = tripId; }
    public String getRaterId()                    { return raterId; }
    public void   setRaterId(String raterId)      { this.raterId = raterId; }
    public String getRatedUserId()                { return ratedUserId; }
    public void   setRatedUserId(String id)       { this.ratedUserId = id; }
    public String getRaterType()                  { return raterType; }
    public void   setRaterType(String raterType)  { this.raterType = raterType; }
    public float  getStars()                      { return stars; }
    public void   setStars(float stars)           { this.stars = stars; }
    public long   getCreatedAt()                  { return createdAt; }
    public void   setCreatedAt(long createdAt)    { this.createdAt = createdAt; }
}