package com.mobilebackend.ungdunglapkehoachdulich.dto.booking;

import lombok.Data;

import java.time.LocalTime;

@Data
public class RestaurantBookingReq {
    private Integer userId;
    private Float totalAmount;
    private String paymentStatus;
    private String address;
    private LocalTime reservationTime;
    private Integer numberOfGuests;
    private BookingPaymentReq payment;
}

