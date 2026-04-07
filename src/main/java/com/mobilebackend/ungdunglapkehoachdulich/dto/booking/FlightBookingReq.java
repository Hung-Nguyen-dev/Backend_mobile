package com.mobilebackend.ungdunglapkehoachdulich.dto.booking;

import lombok.Data;

import java.time.LocalTime;

@Data
public class FlightBookingReq {
    private Integer userId;
    private Float totalAmount;
    private String paymentStatus;
    private String pnrCode;
    private String flightNumber;
    private String departureAirport;
    private String arrivalAirport;
    private LocalTime departureTime;
    private LocalTime arrivalTime;
    private BookingPaymentReq payment;
}

