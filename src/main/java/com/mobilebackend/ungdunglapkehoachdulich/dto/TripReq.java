package com.mobilebackend.ungdunglapkehoachdulich.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;

@Data
public class TripReq {
    private Integer id;
    private String tripName;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private Integer userId;
    
    private List<DailyPlan> activeDays;

    @Data
    public static class DailyPlan {
        private Integer dayIndex; 
        private List<PlaceDraft> places;
    }

    @Data
    public static class PlaceDraft {
        private String name;
        private String description;
        private String category;
        private Double latitude;
        private Double longitude;
        private String suggestTime;
        private String imageUrl;
    }
}
