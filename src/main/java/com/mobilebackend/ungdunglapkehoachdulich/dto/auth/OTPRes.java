package com.mobilebackend.ungdunglapkehoachdulich.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Response sau khi gửi OTP thành công
 */
@Data
@Builder
@AllArgsConstructor
public class OTPRes {
    private String message;
    private String email;
    private Integer expiryMinutes;
}
