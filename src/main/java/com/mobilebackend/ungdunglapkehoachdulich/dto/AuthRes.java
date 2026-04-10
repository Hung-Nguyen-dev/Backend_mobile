package com.mobilebackend.ungdunglapkehoachdulich.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthRes {
    private Integer userId;
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String role;
    private String message;
}