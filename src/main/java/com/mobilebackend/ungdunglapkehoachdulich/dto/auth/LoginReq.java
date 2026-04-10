package com.mobilebackend.ungdunglapkehoachdulich.dto.auth;

import lombok.Data;

@Data
public class LoginReq {
    private String identifier;
    private String password;
}