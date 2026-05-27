package com.mobilebackend.ungdunglapkehoachdulich.dto.auth;

import lombok.Builder;
import lombok.Data;

/**
 * Response payload for authentication-related endpoints.
 */
@Data
@Builder
public class AuthRes {
    private Integer id;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String role;
}