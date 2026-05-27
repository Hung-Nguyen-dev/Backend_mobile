package com.mobilebackend.ungdunglapkehoachdulich.dto.auth;

import lombok.Data;

/**
 * Request payload for user registration.
 */
@Data
public class RegisterReq {
    private String username;
    private String password;
    private String email;
    private String fullName;
    private String avatarUrl;
}