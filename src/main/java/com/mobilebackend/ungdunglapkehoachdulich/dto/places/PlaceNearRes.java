package com.mobilebackend.ungdunglapkehoachdulich.dto.places;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceNearRes {
    private String osmType;
    /** Chuỗi để tránh tràn số trên JS (OSM id lớn). */
    private String osmId;
    private String kind;
    private String name;
    private String addressLine;
    private double lat;
    private double lon;
    private String openStreetMapUrl;
    /** URL anh tu tag OSM (image / wikimedia_commons), co the null */
    private String previewImageUrl;
    /** Thong tin bo sung tu tag OSM (khong phai luc nao cung co) */
    private String phone;
    private String website;
    private String openingHours;
    private String cuisine;
    private String description;
    /** Diem danh gia (SerpAPI / Google Maps local), co the null neu OSM */
    private Double rating;
}
