package com.mobilebackend.ungdunglapkehoachdulich.dto.auth;

import lombok.Builder;
import lombok.Data;

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