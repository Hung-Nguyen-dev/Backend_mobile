package com.mobilebackend.ungdunglapkehoachdulich.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "travel.payment")
public class PaymentGatewayProperties {
    private Vnpay vnpay = new Vnpay();
    private Momo momo = new Momo();

    @Data
    public static class Vnpay {
        private String baseUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
        private String tmnCode;
        private String hashSecret;
        private String returnUrl;
        private String locale = "vn";
        private String orderType = "other";
    }

    @Data
    public static class Momo {
        private String baseUrl = "https://test-payment.momo.vn";
        private String partnerCode;
        private String accessKey;
        private String secretKey;
        private String redirectUrl;
        private String ipnUrl;
        private String requestType = "captureWallet";
        private String lang = "vi";
    }
}

