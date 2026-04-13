package com.mobilebackend.ungdunglapkehoachdulich.dto.auth;

import lombok.Data;

/**
 * Request để xác thực OTP
 */
@Data
public class VerifyOTPReq {
    private String email;
    private String otpCode;
}
