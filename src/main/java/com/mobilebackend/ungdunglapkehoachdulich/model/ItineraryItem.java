package com.mobilebackend.ungdunglapkehoachdulich.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "itinerary_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "trip_id", nullable = false)
    private Integer tripId;

    @Column(name = "user_id", nullable = false)
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

    @Column(name = "amenities_json", columnDefinition = "TEXT")
    private String amenitiesJson;

    @Column(name = "reviews_json", columnDefinition = "TEXT")
    private String reviewsJson;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
