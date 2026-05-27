package com.mobilebackend.ungdunglapkehoachdulich.controller;

import com.mobilebackend.ungdunglapkehoachdulich.dto.auth.*;
import com.mobilebackend.ungdunglapkehoachdulich.service.AuthService;
import com.mobilebackend.ungdunglapkehoachdulich.service.OTPService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
    private final OTPService otpService;

    @Value("${app.otp.expiry-minutes:10}")
    private int otpExpiryMinutes;

    /**
     * Register step 1: create user account without OTP verification.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthRes> register(@RequestBody RegisterReq req) {
        return ResponseEntity.ok(authService.register(req));
    }

    /**
     * Send OTP to user email for registration.
     */
    @PostMapping("/send-otp")
    public ResponseEntity<OTPRes> sendOTP(@RequestBody SendOTPReq req) {
        try {
            if (req == null || req.getEmail() == null || req.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(
                    OTPRes.builder()
                        .message("Email không được để trống")
                        .build()
                );
            }

            authService.validateEmailCanRegister(req.getEmail().trim());
            
            otpService.generateAndSendOTP(req.getEmail().trim());
            
            return ResponseEntity.ok(OTPRes.builder()
                    .message("OTP đã được gửi đến email của bạn")
                    .email(req.getEmail().trim())
                    .expiryMinutes(otpExpiryMinutes)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                OTPRes.builder()
                    .message(e.getMessage())
                    .build()
            );
        }
    }

    /**
     * Verify OTP for registration.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOTP(@RequestBody VerifyOTPReq req) {
        try {
            if (req == null || req.getEmail() == null || req.getOtpCode() == null) {
                return ResponseEntity.badRequest().body(
                    new ErrorRes("Email hoặc OTP không được để trống")
                );
            }
            
            otpService.verifyOTP(req.getEmail().trim(), req.getOtpCode().trim());
            
            return ResponseEntity.ok(new SuccessRes("OTP xác thực thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new ErrorRes(e.getMessage())
            );
        }
    }

    /**
     * Complete registration after OTP verification.
     */
    @PostMapping("/complete-registration")
    public ResponseEntity<?> completeRegistration(@RequestBody CompleteRegistrationReq req) {
        try {
            return ResponseEntity.ok(authService.completeRegistration(req));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                new ErrorRes(e.getMessage())
            );
        }
    }

    /**
     * Login by username/email and password.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthRes> login(@RequestBody LoginReq req) {
        return ResponseEntity.ok(authService.login(req));
    }

    /**
     * Send OTP for forgot-password flow.
     */
    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<?> sendForgotPasswordOtp(@RequestBody ForgotPasswordReq req) {
        try {
            if (req == null || req.getEmail() == null || req.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorRes("Email khong duoc de trong"));
            }

            authService.sendForgotPasswordOtp(req.getEmail().trim());
            return ResponseEntity.ok(OTPRes.builder()
                    .message("OTP da duoc gui den email cua ban")
                    .email(req.getEmail().trim())
                    .expiryMinutes(otpExpiryMinutes)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorRes(e.getMessage()));
        }
    }

    /**
     * Verify OTP for forgot-password flow.
     */
    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<?> verifyForgotPasswordOtp(@RequestBody VerifyOTPReq req) {
        try {
            if (req == null || req.getEmail() == null || req.getOtpCode() == null) {
                return ResponseEntity.badRequest().body(new ErrorRes("Email hoac OTP khong duoc de trong"));
            }

            authService.verifyForgotPasswordOtp(req.getEmail().trim(), req.getOtpCode().trim());
            return ResponseEntity.ok(new SuccessRes("OTP hop le"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorRes(e.getMessage()));
        }
    }

    /**
     * Reset password after OTP verification.
     */
    @PostMapping("/forgot-password/reset")
    public ResponseEntity<?> resetForgotPassword(@RequestBody ResetPasswordReq req) {
        try {
            authService.resetPassword(req);
            return ResponseEntity.ok(new SuccessRes("Doi mat khau thanh cong"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(new ErrorRes(e.getMessage()));
        }
    }

    /**
     * Error response wrapper for simple messages.
     */
    public static class ErrorRes {
        public String message;

        public ErrorRes(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    /**
     * Success response wrapper for simple messages.
     */
    public static class SuccessRes {
        public String message;

        public SuccessRes(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}