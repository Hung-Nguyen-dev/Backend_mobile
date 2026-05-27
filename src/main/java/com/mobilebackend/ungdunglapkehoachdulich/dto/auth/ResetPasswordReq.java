package com.mobilebackend.ungdunglapkehoachdulich.dto.auth;

import lombok.Data;

/**
 * Request payload for resetting password after OTP verification.
 */
@Data
public class ResetPasswordReq {
    private String email;
    private String newPassword;
}
