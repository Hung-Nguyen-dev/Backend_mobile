package com.mobilebackend.ungdunglapkehoachdulich.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@travelapp.com}")
    private String fromEmail;

    @Value("${app.mail.otp-template:Mã xác thực của bạn là: %s. Mã này sẽ hết hạn trong 10 phút.}")
    private String otpTemplate;

    /**
     * Gửi email xác thực OTP
     */
    public void sendOTPEmail(String toEmail, String otpCode) throws MessagingException {
        String subject = "Xác thực email của bạn";
        String body = String.format(otpTemplate, otpCode);
        
        sendHtmlEmail(toEmail, subject, body);
        log.info("OTP email sent to: {}", toEmail);
    }

    /**
     * Gửi email HTML
     */
    private void sendHtmlEmail(String toEmail, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(toEmail);
        helper.setSubject(subject);
        
        String fullHtmlContent = buildEmailTemplate(htmlContent);
        helper.setText(fullHtmlContent, true);

        mailSender.send(message);
    }

    /**
     * Build email template HTML
     */
    private String buildEmailTemplate(String content) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; background-color: #f5f5f5; }" +
                ".container { max-width: 600px; margin: 20px auto; background-color: white; padding: 30px; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }" +
                ".header { text-align: center; margin-bottom: 30px; }" +
                ".header h1 { color: #333; margin: 0; }" +
                ".content { color: #555; line-height: 1.6; }" +
                ".otp-box { background-color: #f0f0f0; padding: 15px; margin: 20px 0; border-radius: 6px; text-align: center; }" +
                ".otp-code { font-size: 24px; font-weight: bold; color: #007bff; letter-spacing: 2px; }" +
                ".footer { margin-top: 30px; padding-top: 20px; border-top: 1px solid #ddd; color: #999; font-size: 12px; text-align: center; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1>✈️ Ứng dụng Lập kế hoạch du lịch</h1>" +
                "</div>" +
                "<div class='content'>" +
                content +
                "</div>" +
                "<div class='footer'>" +
                "<p>Đây là email tự động, vui lòng không trả lời email này.</p>" +
                "<p>© 2024 Travel App. All rights reserved.</p>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}
