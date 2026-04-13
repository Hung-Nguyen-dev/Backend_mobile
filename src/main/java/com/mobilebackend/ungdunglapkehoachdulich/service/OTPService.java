package com.mobilebackend.ungdunglapkehoachdulich.service;

import com.mobilebackend.ungdunglapkehoachdulich.model.EmailOTP;
import com.mobilebackend.ungdunglapkehoachdulich.repo.EmailOTPRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OTPService {

    private final EmailOTPRepo emailOTPRepo;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.otp.length:6}")
    private int otpLength;

    @Value("${app.otp.expiry-minutes:10}")
    private int otpExpiryMinutes;

    @Value("${app.otp.resend-cooldown-seconds:60}")
    private int resendCooldownSeconds;

    /**
     * Tạo và gửi OTP cho email
     */
    @Transactional
    public void generateAndSendOTP(String email) throws Exception {
        EmailOTP latestOtp = emailOTPRepo.findFirstByEmailAndUsedFalseOrderByCreatedAtDesc(email).orElse(null);
        long now = System.currentTimeMillis();

        if (latestOtp != null && now <= latestOtp.getExpiresAt()) {
            long elapsedMillis = now - latestOtp.getCreatedAt();
            long cooldownMillis = resendCooldownSeconds * 1000L;
            if (elapsedMillis < cooldownMillis) {
                long waitSeconds = Math.max(1, (cooldownMillis - elapsedMillis) / 1000L);
                throw new Exception("OTP vua duoc gui. Vui long thu lai sau " + waitSeconds + " giay");
            }
        }

        // Tạo OTP code
        String otpCode = generateOTPCode();
        
        // Tính thời gian hết hạn (hiện tại + otpExpiryMinutes)
        long expiresAt = now + (otpExpiryMinutes * 60 * 1000L);

        // Lưu OTP vào database
        EmailOTP emailOTP = EmailOTP.builder()
                .email(email)
                .otpCode(otpCode)
                .expiresAt(expiresAt)
                .used(false)
                .createdAt(now)
                .build();
        
        emailOTPRepo.save(emailOTP);
        
        // Gửi email
        try {
            emailService.sendOTPEmail(email, otpCode);
            log.info("OTP sent successfully to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send OTP email to: {}", email, e);
            emailOTPRepo.delete(emailOTP);
            throw new Exception("Gửi email thất bại. Vui lòng thử lại sau.");
        }
    }

    /**
     * Xác thực OTP
     */
    @Transactional
    public void verifyOTP(String email, String otpCode) throws Exception {
        EmailOTP emailOTP = emailOTPRepo.findFirstByEmailAndUsedFalseOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new Exception("Không tìm thấy OTP. Vui lòng gửi lại mã xác thực."));

        // Kiểm tra xem OTP có hết hạn không
        if (System.currentTimeMillis() > emailOTP.getExpiresAt()) {
            throw new Exception("Mã OTP đã hết hạn. Vui lòng gửi lại mã xác thực.");
        }

        // Kiểm tra mã OTP có khớp không
        if (!emailOTP.getOtpCode().equals(otpCode.trim())) {
            throw new Exception("Mã OTP không chính xác.");
        }

        // Đánh dấu OTP là đã sử dụng
        emailOTP.setUsed(true);
        emailOTP.setVerifiedAt(System.currentTimeMillis());
        emailOTPRepo.save(emailOTP);
        
        log.info("OTP verified successfully for: {}", email);
    }

    /**
     * Tạo OTP code ngẫu nhiên
     */
    private String generateOTPCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * Kiểm tra xem email có OTP hợp lệ chưa
     */
    public boolean hasValidOTP(String email) {
        return emailOTPRepo.hasValidOTP(email, System.currentTimeMillis());
    }

    /**
     * Kiểm tra email đã được xác thực OTP gần đây chưa.
     * Dùng cho bước hoàn tất đăng ký sau khi người dùng bấm "Xác nhận OTP" ở frontend.
     */
    public boolean hasRecentlyVerifiedOTP(String email) {
        Optional<EmailOTP> latestVerified = emailOTPRepo
                .findFirstByEmailAndVerifiedAtIsNotNullOrderByVerifiedAtDesc(email);

        if (latestVerified.isEmpty()) {
            return false;
        }

        long maxAgeMillis = otpExpiryMinutes * 60L * 1000L;
        Long verifiedAt = latestVerified.get().getVerifiedAt();
        return verifiedAt != null && (System.currentTimeMillis() - verifiedAt) <= maxAgeMillis;
    }
}
