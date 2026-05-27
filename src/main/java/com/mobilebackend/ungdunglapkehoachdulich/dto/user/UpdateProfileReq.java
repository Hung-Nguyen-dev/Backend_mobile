package com.mobilebackend.ungdunglapkehoachdulich.dto.user;

import lombok.Data;

/**
 * Request payload for updating user profile.
 */
@Data
public class UpdateProfileReq {
    private String username;
    private String email;
    private String fullName;
    private String avatarUrl;
    private String currentPassword;
    private String password;
}