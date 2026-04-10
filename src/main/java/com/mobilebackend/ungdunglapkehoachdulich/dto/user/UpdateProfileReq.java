package com.mobilebackend.ungdunglapkehoachdulich.dto.user;

import lombok.Data;

@Data
public class UpdateProfileReq {
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String password;
}