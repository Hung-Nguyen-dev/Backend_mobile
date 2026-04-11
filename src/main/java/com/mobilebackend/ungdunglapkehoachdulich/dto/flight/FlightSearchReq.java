package com.mobilebackend.ungdunglapkehoachdulich.dto.flight;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class FlightSearchReq {
    private String originLocationCode;
    private String destinationLocationCode;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate departureDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate returnDate;
    private Integer adults = 1;
    private Integer children;
    private Integer infants;
    private String travelClass;
    private Boolean nonStop;
    private String currencyCode = "VND";
    private Integer maxPrice;
    private Integer max = 10;
}


