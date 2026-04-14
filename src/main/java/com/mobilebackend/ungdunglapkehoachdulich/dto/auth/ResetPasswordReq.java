package com.mobilebackend.ungdunglapkehoachdulich.dto.auth;

import lombok.Data;

@Data
public class ResetPasswordReq {
    private String email;
    private String newPassword;
}
