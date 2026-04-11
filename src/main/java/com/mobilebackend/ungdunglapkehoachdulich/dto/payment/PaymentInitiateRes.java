package com.mobilebackend.ungdunglapkehoachdulich.dto.payment;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentInitiateRes {
    private Integer bookingId;
    private String provider;
    private String transactionNo;
    private String paymentUrl;
    private String status;
}

