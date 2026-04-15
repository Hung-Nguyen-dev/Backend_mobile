package com.mobilebackend.ungdunglapkehoachdulich.dto.itinerary;

import lombok.Data;

import java.util.List;

@Data
public class ItineraryItemCreateReq {
    private Integer tripId;
    private String kind;
    private String placeName;
    private String address;
    private Double lat;
    private Double lon;
    private String phone;
    private String website;
    private String bookingLink;
    private Double rating;
    private Boolean openNow;
    private List<String> amenities;
    private List<String> reviews;
}
