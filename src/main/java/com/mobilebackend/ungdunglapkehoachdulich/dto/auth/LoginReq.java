package com.mobilebackend.ungdunglapkehoachdulich.dto.auth;

import lombok.Data;

/**
 * Request payload for login by username/email and password.
 */
@Data
public class LoginReq {
    private String identifier;
    private String password;
}