package com.mobilebackend.ungdunglapkehoachdulich.dto.booking;

import lombok.Data;

import java.time.LocalDate;

@Data
public class HotelBookingReq {
    private Integer userId;
    private Float totalAmount;
    private String paymentStatus;
    private String roomType;
    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private BookingPaymentReq payment;
}

