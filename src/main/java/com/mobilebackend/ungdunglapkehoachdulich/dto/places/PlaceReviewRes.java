package com.mobilebackend.ungdunglapkehoachdulich.dto.places;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaceReviewRes {
    private String user;
    private String summary;
    private Double rating;
    private String relativeDate;
}
