package com.mobilebackend.ungdunglapkehoachdulich.config;

import com.mobilebackend.ungdunglapkehoachdulich.model.User;
import com.mobilebackend.ungdunglapkehoachdulich.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class AdminAccountSeederConfig {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_EMAIL = "admin@gmail.com";
    private static final String ADMIN_PASSWORD = "admin";

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner seedDefaultAdminAccount() {
        return args -> {
            User adminUser = userRepo.findByEmail(ADMIN_EMAIL)
                    .or(() -> userRepo.findByUsername(ADMIN_USERNAME))
                    .orElseGet(User::new);

            adminUser.setUsername(ADMIN_USERNAME);
            adminUser.setEmail(ADMIN_EMAIL);
            adminUser.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
            adminUser.setRole("ADMIN");
            adminUser.setFullName("System Admin");
            adminUser.setIsEmailVerified(true);
            adminUser.setEmailVerifiedAt(System.currentTimeMillis());

            userRepo.save(adminUser);
        };
    }
}
