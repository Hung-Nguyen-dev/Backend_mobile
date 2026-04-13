package com.mobilebackend.ungdunglapkehoachdulich.dto.auth;

import lombok.Data;

/**
 * Request để gửi OTP đến email
 */
@Data
public class SendOTPReq {
    private String email;
}
