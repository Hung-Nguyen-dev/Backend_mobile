package com.mobilebackend.ungdunglapkehoachdulich.controller;

import com.mobilebackend.ungdunglapkehoachdulich.dto.ai.AiCityDataSuggestionRes;
import com.mobilebackend.ungdunglapkehoachdulich.dto.ai.AiItineraryDtos.AiItineraryRequest;
import com.mobilebackend.ungdunglapkehoachdulich.dto.ai.AiItineraryDtos.AiItineraryResponse;
import com.mobilebackend.ungdunglapkehoachdulich.dto.places.PlaceNearRes;
import com.mobilebackend.ungdunglapkehoachdulich.service.AiItineraryService;
import com.mobilebackend.ungdunglapkehoachdulich.service.CityDataService;
import com.mobilebackend.ungdunglapkehoachdulich.service.CityDataService.CityPlace;
import com.mobilebackend.ungdunglapkehoachdulich.service.PlacesSearchService;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@CrossOrigin(origins = "*")
public class AiItineraryController {

    private final AiItineraryService aiItineraryService;
    private final CityDataService cityDataService;
    private final PlacesSearchService placesSearchService;

    public AiItineraryController(
            AiItineraryService aiItineraryService,
            CityDataService cityDataService,
            PlacesSearchService placesSearchService
    ) {
        this.aiItineraryService = aiItineraryService;
        this.cityDataService = cityDataService;
        this.placesSearchService = placesSearchService;
    }

    @PostMapping("/itinerary")
    public AiItineraryResponse suggest(@RequestBody AiItineraryRequest req) {
        return aiItineraryService.generate(req);
    }

    @GetMapping("/city-data")
    public List<AiCityDataSuggestionRes> cityData(
            @RequestParam String destination,
            @RequestParam String category,
            @RequestParam(required = false) String query
    ) {
        String normalizedCategory = normalizeCategory(category);
        if (normalizedCategory.isBlank()) {
            return List.of();
        }

        try {
            // Prefer live search (SerpAPI + OSM fallback) for all provinces/cities.
            for (String candidate : buildDestinationCandidates(destination)) {
                List<PlaceNearRes> live = placesSearchService.searchNearby(
                        candidate,
                        null,
                        null,
                        normalizedCategory,
                        5000,
                        12
                );
                if (!live.isEmpty()) {
                    return live.stream()
                            .map(this::fromPlaceNear)
                            .toList();
                }
            }
        } catch (Exception ignored) {
            // Fallback to bundled city-data if live provider is unavailable.
        }

        return cityDataService.searchPlaces(destination, normalizedCategory, query)
                .stream()
                .map(this::fromCityPlace)
                .toList();
    }

    private String normalizeCategory(String category) {
        if (category == null) {
            return "";
        }
        String lower = category.trim().toLowerCase(Locale.ROOT);
        if (lower.contains("tourism")) {
            return "tourism";
        }
        if (lower.contains("restaurant")) {
            return "restaurant";
        }
        if (lower.contains("cafe")) {
            return "cafe";
        }
        return "";
    }

    private List<String> buildDestinationCandidates(String destination) {
        Set<String> variants = new LinkedHashSet<>();
        String base = destination == null ? "" : destination.trim();
        if (!base.isBlank()) {
            variants.add(base);
            variants.add(base + ", Việt Nam");
        }

        String normalized = removeDiacritics(base).replaceAll("[^a-z0-9]+", "");
        if (normalized.contains("nghean")) {
            variants.add("Vinh, " + base);
            variants.add("Vinh, Nghệ An");
        }

        return variants.stream().filter(v -> !v.isBlank()).toList();
    }

    private String removeDiacritics(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
        return normalized.toLowerCase(Locale.ROOT);
    }

    private AiCityDataSuggestionRes fromPlaceNear(PlaceNearRes place) {
        String title = firstNonEmpty(place.getName(), place.getKind(), place.getCuisine(), "Địa điểm nổi bật");
        String type = firstNonEmpty(place.getCuisine(), place.getKind(), "địa điểm");
        String placeId = firstNonEmpty(place.getOsmType(), "") + "/" + firstNonEmpty(place.getOsmId(), "");
        if ("/".equals(placeId)) {
            placeId = null;
        }

        return AiCityDataSuggestionRes.builder()
                .title(title)
                .rating(place.getRating())
                .reviews(null)
                .description(firstNonEmpty(place.getDescription(), place.getAddressLine(), type))
                .type(type)
                .address(place.getAddressLine())
                .thumbnail(place.getPreviewImageUrl())
                .placeId(placeId)
                .gpsCoordinates(AiCityDataSuggestionRes.GpsCoordinates.builder()
                        .latitude(place.getLat())
                        .longitude(place.getLon())
                        .build())
                .hours(place.getOpeningHours())
                .priceRange(null)
                .mapLink(place.getOpenStreetMapUrl())
                .build();
    }

    private AiCityDataSuggestionRes fromCityPlace(CityPlace place) {
        return AiCityDataSuggestionRes.builder()
                .title(place.title())
                .rating(place.rating())
                .reviews(place.reviews())
                .description(firstNonEmpty(place.description(), place.address(), place.type()))
                .type(place.type())
                .address(place.address())
                .thumbnail(place.thumbnail())
                .placeId(place.placeId())
                .gpsCoordinates(AiCityDataSuggestionRes.GpsCoordinates.builder()
                        .latitude(place.latitude())
                        .longitude(place.longitude())
                        .build())
                .hours(place.hours())
                .priceRange(place.priceRange())
                .mapLink(buildMapLink(place.title(), place.placeId()))
                .build();
    }

    private String buildMapLink(String title, String placeId) {
        String q = firstNonEmpty(title, "địa điểm");
        String encodedQuery = java.net.URLEncoder.encode(q, java.nio.charset.StandardCharsets.UTF_8);
        if (placeId != null && !placeId.isBlank()) {
            String encodedPlaceId = java.net.URLEncoder.encode(placeId, java.nio.charset.StandardCharsets.UTF_8);
            return "https://www.google.com/maps/search/?api=1&query=" + encodedQuery + "&query_place_id=" + encodedPlaceId;
        }
        return "https://www.google.com/maps/search/?api=1&query=" + encodedQuery;
    }

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
