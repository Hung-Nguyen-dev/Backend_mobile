package com.mobilebackend.ungdunglapkehoachdulich.dto.auth;

import lombok.Data;

/**
 * Request để hoàn tất đăng ký sau khi xác thực OTP
 */
@Data
public class CompleteRegistrationReq {
    private String username;
    private String password;
    private String email;
    private String fullName;
    private String avatarUrl;
}
