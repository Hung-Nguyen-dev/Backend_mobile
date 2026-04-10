package com.mobilebackend.ungdunglapkehoachdulich.service;

import com.mobilebackend.ungdunglapkehoachdulich.dto.AuthRes;
import com.mobilebackend.ungdunglapkehoachdulich.dto.LoginReq;
import com.mobilebackend.ungdunglapkehoachdulich.dto.RegisterReq;
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

    @Transactional
    public AuthRes register(RegisterReq req) {
        if (req == null || req.getUsername() == null || req.getPassword() == null || req.getEmail() == null) {
            throw new IllegalArgumentException("Thông tin đăng ký không hợp lệ");
        }
        if (userRepo.existsByUsername(req.getUsername())) {
            throw new IllegalStateException("Username đã tồn tại");
        }
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new IllegalStateException("Email đã tồn tại");
        }

        User user = User.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .email(req.getEmail())
                .fullName(req.getFullName())
                .avatarUrl(req.getAvatarUrl())
                .role("USER")
                .build();
        User saved = userRepo.save(user);
        return toRes(saved, "Đăng ký thành công");
    }

    public AuthRes login(LoginReq req) {
        if (req == null || req.getUsernameOrEmail() == null || req.getPassword() == null) {
            throw new IllegalArgumentException("Thông tin đăng nhập không hợp lệ");
        }

        User user = userRepo.findByUsername(req.getUsernameOrEmail())
                .or(() -> userRepo.findByEmail(req.getUsernameOrEmail()))
                .orElseThrow(() -> new IllegalArgumentException("Sai tài khoản hoặc mật khẩu"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Sai tài khoản hoặc mật khẩu");
        }

        return toRes(user, "Đăng nhập thành công");
    }

    private AuthRes toRes(User user, String message) {
        return AuthRes.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .message(message)
                .build();
    }
}