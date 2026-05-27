package com.mobilebackend.ungdunglapkehoachdulich.dto.auth;

import lombok.Data;

/**
 * Request payload for forgot-password OTP sending.
 */
@Data
public class ForgotPasswordReq {
    private String email;
}
