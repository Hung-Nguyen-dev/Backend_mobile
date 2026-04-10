package com.mobilebackend.ungdunglapkehoachdulich.controller;

import com.mobilebackend.ungdunglapkehoachdulich.dto.AuthRes;
import com.mobilebackend.ungdunglapkehoachdulich.dto.LoginReq;
import com.mobilebackend.ungdunglapkehoachdulich.dto.RegisterReq;
import com.mobilebackend.ungdunglapkehoachdulich.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthRes> register(@RequestBody RegisterReq req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthRes> login(@RequestBody LoginReq req) {
        return ResponseEntity.ok(authService.login(req));
    }
}