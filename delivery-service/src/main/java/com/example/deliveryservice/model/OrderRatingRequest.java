package com.example.deliveryservice.model;

/**
 * DTO for rating an order. Contains only the rating value. Ratings should
 * typically be validated (e.g. between 1 and 5) but validation is omitted
 * here for brevity.
 */
public class OrderRatingRequest {
    private Integer rating;

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }
}