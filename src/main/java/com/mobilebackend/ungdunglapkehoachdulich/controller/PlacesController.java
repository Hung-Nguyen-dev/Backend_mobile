package com.mobilebackend.ungdunglapkehoachdulich.controller;

import com.mobilebackend.ungdunglapkehoachdulich.dto.places.PlaceMapsEnrichRes;
import com.mobilebackend.ungdunglapkehoachdulich.service.PlacesSearchService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/places")
@RequiredArgsConstructor
public class PlacesController {
    private static final Logger log = LoggerFactory.getLogger(PlacesController.class);
    private final PlacesSearchService placesSearchService;

    /**
     * Tim khach san / nha hang qua OpenStreetMap (Photon/Nominatim geocode + Overpass).
     * Hoac truyen {@code lat},{@code lon} de tim quanh vi tri GPS (khong can geocode).
     */
    @GetMapping("/nearby")
    public ResponseEntity<?> nearby(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lon,
            /** Alias cua {@code lon} (tien cho client / SerpAPI-style). */
            @RequestParam(required = false) Double lng,
            @RequestParam(defaultValue = "hotel") String kind,
            /** Alias cua {@code kind}: {@code restaurant} | {@code hotel}. */
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer radiusMeters,
            @RequestParam(required = false) Integer limit
    ) {
        try {
            Double effectiveLon = lon != null ? lon : lng;
            String effectiveKind = (type != null && !type.isBlank()) ? type.trim() : kind;
            return ResponseEntity.ok(
                    placesSearchService.searchNearby(location, lat, effectiveLon, effectiveKind, radiusMeters, limit));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.warn("places/nearby failed: {}", e.toString(), e);
            String msg = e.getMessage();
            if (msg == null || msg.isBlank()) {
                msg = e.getClass().getSimpleName();
            }
            if (msg.length() > 220) {
                msg = msg.substring(0, 220) + "...";
            }
            return ResponseEntity.status(503).body("OSM loi: " + msg + " (xem log server neu can).");
        }
    }

    @GetMapping("/attractions")
    public ResponseEntity<?> getAttractions(
            @RequestParam String destination,
            @RequestParam(required = false) Integer limit
    ) {
        try {
            return ResponseEntity.ok(placesSearchService.searchAttractions(destination, limit));
        } catch (Exception e) {
            log.warn("places/attractions failed: {}", e.toString(), e);
            return ResponseEntity.status(503).body("Error fetching attractions: " + e.getMessage());
        }
    }

    /**
     * Bo sung SĐT, giờ mở cửa, đánh giá… từ Google Maps (SerpAPI). Trả JSON rỗng / null field nếu tắt Serp hoặc lỗi.
     */
    @GetMapping("/maps-enrich")
    public PlaceMapsEnrichRes mapsEnrich(
            @RequestParam String placeName,
            @RequestParam(required = false) String address,
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(required = false) String placeId
    ) {
        return placesSearchService.mapsEnrich(
                placeName,
                address == null ? "" : address,
                lat,
                lon,
                placeId
        );
    }
}
