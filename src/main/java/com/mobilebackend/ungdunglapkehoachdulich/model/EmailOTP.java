package com.mobilebackend.ungdunglapkehoachdulich.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Email OTP entity for registration and password reset flows.
 */
@Entity
@Table(name = "email_otps")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailOTP {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String otpCode;

    @Column(nullable = false)
    private Long expiresAt;

    @Column(nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean used = false;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Long createdAt = System.currentTimeMillis();

    @Column(name = "verified_at")
    private Long verifiedAt;
}
