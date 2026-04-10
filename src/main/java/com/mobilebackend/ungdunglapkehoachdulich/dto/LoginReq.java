package com.mobilebackend.ungdunglapkehoachdulich.dto;

import lombok.Data;

@Data
public class LoginReq {
    private String usernameOrEmail;
    private String password;
}