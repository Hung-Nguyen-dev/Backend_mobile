package com.mobilebackend.ungdunglapkehoachdulich.dto;

import lombok.Data;

@Data
public class UpdateTripStopReq {
    private String visitTime; // HH:mm
    private String note;
}
