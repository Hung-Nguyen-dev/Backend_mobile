package com.mobilebackend.ungdunglapkehoachdulich.dto.places;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Thong tin bo sung tu Google Maps (SerpAPI) cho man chi tiet dia diem.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceMapsEnrichRes {
    private String phone;
    private String website;
    private String bookingLink;
    private String openingHours;
    private Boolean openNow;
    private String openState;
    private Double rating;
    private Integer reviewsCount;
    private Double gpsLat;
    private Double gpsLon;
    private java.util.List<String> amenities;
    private java.util.List<PlaceReviewRes> reviews;
    /** Loai dia diem (vd. Nha hang Viet) */
    private String typeLabel;
    /** Dong ghi chu ngan (danh gia, loai hinh) */
    private String snippet;
    /** Mo ta ngan tu Google Maps (local_results.description) */
    private String description;
    /** Muc gia ($, $$, …) */
    private String price;

    public static PlaceMapsEnrichRes empty() {
        return PlaceMapsEnrichRes.builder().build();
    }
}
