package com.mobilebackend.ungdunglapkehoachdulich.dto.auth;

import lombok.Data;

/**
 * Request payload for sending OTP to an email address.
 */
@Data
public class SendOTPReq {
    private String email;
}
