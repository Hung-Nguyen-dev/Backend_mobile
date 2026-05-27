package com.mobilebackend.ungdunglapkehoachdulich.dto.auth;

import lombok.Data;

/**
 * Request payload to complete registration after OTP verification.
 */
@Data
public class CompleteRegistrationReq {
    private String username;
    private String password;
    private String email;
    private String fullName;
    private String avatarUrl;
}
