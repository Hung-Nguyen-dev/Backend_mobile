package com.mobilebackend.ungdunglapkehoachdulich.dto.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiCityDataSuggestionRes {
    private String title;
    private Double rating;
    private Integer reviews;
    private String description;
    private String type;
    private String address;
    private String thumbnail;

    @JsonProperty("place_id")
    private String placeId;

    @JsonProperty("gps_coordinates")
    private GpsCoordinates gpsCoordinates;

    private String hours;
    private String priceRange;
    private String mapLink;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GpsCoordinates {
        private Double latitude;
        private Double longitude;
    }
}