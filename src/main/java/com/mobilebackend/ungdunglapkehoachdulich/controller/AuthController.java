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
     * Đăng ký - Bước 1: Tạo user account (chưa xác thực email)
     */
    @PostMapping("/register")
    public ResponseEntity<AuthRes> register(@RequestBody RegisterReq req) {
        return ResponseEntity.ok(authService.register(req));
    }

    /**
     * Gửi OTP đến email của người dùng
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
     * Xác thực OTP
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
     * Hoàn tất đăng ký - Bước 2: Tạo user sau khi xác thực OTP
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
     * Đăng nhập
     */
    @PostMapping("/login")
    public ResponseEntity<AuthRes> login(@RequestBody LoginReq req) {
        return ResponseEntity.ok(authService.login(req));
    }

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
     * Helper class for error response
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
     * Helper class for success response
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