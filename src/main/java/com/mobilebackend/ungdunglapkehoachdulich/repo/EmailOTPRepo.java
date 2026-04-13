package com.mobilebackend.ungdunglapkehoachdulich.repo;

import com.mobilebackend.ungdunglapkehoachdulich.model.EmailOTP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailOTPRepo extends JpaRepository<EmailOTP, Long> {
    
    /**
     * Lấy OTP mới nhất chưa được sử dụng cho một email
     */
    Optional<EmailOTP> findFirstByEmailAndUsedFalseOrderByCreatedAtDesc(String email);

    Optional<EmailOTP> findFirstByEmailAndVerifiedAtIsNotNullOrderByVerifiedAtDesc(String email);
    
    /**
     * Kiểm tra xem email có OTP hợp lệ chưa được sử dụng không
     */
    @Query("SELECT COUNT(e) > 0 FROM EmailOTP e WHERE e.email = :email AND e.used = false AND e.expiresAt > :currentTime")
    boolean hasValidOTP(String email, Long currentTime);
}
