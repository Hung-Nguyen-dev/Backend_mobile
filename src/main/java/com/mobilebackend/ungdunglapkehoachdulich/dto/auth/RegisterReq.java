package com.mobilebackend.ungdunglapkehoachdulich.dto.auth;

import lombok.Data;

@Data
public class RegisterReq {
    private String username;
    private String password;
    private String email;
    private String fullName;
    private String avatarUrl;
}