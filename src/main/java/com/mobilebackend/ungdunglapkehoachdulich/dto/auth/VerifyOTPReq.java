package com.mobilebackend.ungdunglapkehoachdulich.dto.auth;

import lombok.Data;

/**
 * Request payload for verifying OTP.
 */
@Data
public class VerifyOTPReq {
    private String email;
    private String otpCode;
}
