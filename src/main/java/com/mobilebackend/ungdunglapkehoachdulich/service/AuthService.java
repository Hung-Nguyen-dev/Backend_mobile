package com.mobilebackend.ungdunglapkehoachdulich.service;

import com.mobilebackend.ungdunglapkehoachdulich.dto.auth.AuthRes;
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}