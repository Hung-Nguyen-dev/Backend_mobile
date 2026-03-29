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
    private List<ItineraryReq> itineraryReqs;
    private HashMap<Integer,Integer> postId;
}
