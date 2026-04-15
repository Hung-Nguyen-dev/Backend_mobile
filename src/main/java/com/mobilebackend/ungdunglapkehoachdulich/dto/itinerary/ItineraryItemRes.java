package com.mobilebackend.ungdunglapkehoachdulich.dto.itinerary;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ItineraryItemRes {
    private Integer id;
    private Integer tripId;
    private Integer userId;
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
    private LocalDateTime createdAt;
}
