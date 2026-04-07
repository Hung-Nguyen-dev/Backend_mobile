package com.mobilebackend.ungdunglapkehoachdulich.dto.booking;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class CoachBookingReq {
    private Integer userId;
    private Float totalAmount;
    private String paymentStatus;
    private String name;
    private String seat;
    private String pickUp;
    private String dropOff;
    private String plateNumber;
    private LocalDate departureDate;
    private LocalTime departureTime;
    private BookingPaymentReq payment;
}


