package com.mobilebackend.ungdunglapkehoachdulich.dto.booking;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BookingPaymentReq {
    private String transactionNo;
    private Float amount;
    private LocalDate paymentDate;
}

