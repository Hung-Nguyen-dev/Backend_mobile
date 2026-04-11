package com.mobilebackend.ungdunglapkehoachdulich.dto.payment;

import lombok.Data;

@Data
public class PaymentInitiateReq {
    private String provider;
    private Float amount;
    private String orderInfo;
    private String returnUrl;
    private String ipnUrl;
    private String requestType;
}

