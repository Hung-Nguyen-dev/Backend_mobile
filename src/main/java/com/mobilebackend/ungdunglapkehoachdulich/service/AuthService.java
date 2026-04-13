package com.mobilebackend.ungdunglapkehoachdulich.service;

import com.mobilebackend.ungdunglapkehoachdulich.dto.auth.AuthRes;
import com.mobilebackend.ungdunglapkehoachdulich.dto.auth.CompleteRegistrationReq;
import com.mobilebackend.ungdunglapkehoachdulich.dto.auth.LoginReq;
import com.mobilebackend.ungdunglapkehoachdulich.dto.auth.RegisterReq;
import com.mobilebackend.ungdunglapkehoachdulich.model.User;
import com.mobilebackend.ungdunglapkehoachdulich.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final OTPService otpService;

    @Transactional
    public AuthRes register(RegisterReq req) {
        if (req == null || isBlank(req.getUsername()) || isBlank(req.getPassword()) || isBlank(req.getEmail())) {
            throw new IllegalArgumentException("Thong tin dang ky khong hop le");
        }
        if (userRepo.existsByUsername(req.getUsername().trim())) {
            throw new IllegalArgumentException("Username da ton tai");
        }
        if (userRepo.existsByEmail(req.getEmail().trim())) {
            throw new IllegalArgumentException("Email da ton tai");
        }

        User user = User.builder()
                .username(req.getUsername().trim())
                .email(req.getEmail().trim())
                .fullName(req.getFullName())
                .avatarUrl(req.getAvatarUrl())
                .password(passwordEncoder.encode(req.getPassword()))
                .role("USER")
                .isEmailVerified(false)
                .build();

        return fromUser(userRepo.save(user));
    }

    @Transactional
    public AuthRes completeRegistration(CompleteRegistrationReq req) throws Exception {
        if (req == null || isBlank(req.getUsername()) || isBlank(req.getPassword()) ||
            isBlank(req.getEmail())) {
            throw new IllegalArgumentException("Thong tin dang ky khong hop le");
        }

        String normalizedEmail = req.getEmail().trim();

        // Người dùng phải xác thực OTP trước khi hoàn tất đăng ký.
        if (!otpService.hasRecentlyVerifiedOTP(normalizedEmail)) {
            throw new IllegalArgumentException("Email chua duoc xac thuc OTP hoac ma da het han");
        }

        // Kiểm tra username và email đã tồn tại
        if (userRepo.existsByUsername(req.getUsername().trim())) {
            throw new IllegalArgumentException("Username da ton tai");
        }
        if (userRepo.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Email da ton tai");
        }

        // Tạo user mới
        User user = User.builder()
                .username(req.getUsername().trim())
            .email(normalizedEmail)
                .fullName(req.getFullName())
                .avatarUrl(req.getAvatarUrl())
                .password(passwordEncoder.encode(req.getPassword()))
                .role("USER")
                .isEmailVerified(true)
                .emailVerifiedAt(System.currentTimeMillis())
                .build();

        return fromUser(userRepo.save(user));
    }

    public AuthRes login(LoginReq req) {
        if (req == null || isBlank(req.getIdentifier()) || isBlank(req.getPassword())) {
            throw new IllegalArgumentException("Thong tin dang nhap khong hop le");
        }

        User user = userRepo.findByUsernameOrEmail(req.getIdentifier().trim(), req.getIdentifier().trim())
                .orElseThrow(() -> new IllegalArgumentException("Sai tai khoan hoac mat khau"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Sai tai khoan hoac mat khau");
        }

        // Chỉ cho phép đăng nhập nếu email đã được xác thực
        if (!user.getIsEmailVerified()) {
            throw new IllegalArgumentException("Email chua duoc xac thuc. Vui long kiem tra email de xac thuc tai khoan.");
        }

        return fromUser(user);
    }

    private AuthRes fromUser(User user) {
        return AuthRes.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .build();
    }

    public void validateEmailCanRegister(String email) {
        if (isBlank(email)) {
            throw new IllegalArgumentException("Email khong hop le");
        }
        if (userRepo.existsByEmail(email.trim())) {
            throw new IllegalArgumentException("Email da ton tai");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}