package com.mobilebackend.ungdunglapkehoachdulich.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobilebackend.ungdunglapkehoachdulich.config.PaymentGatewayProperties;
import com.mobilebackend.ungdunglapkehoachdulich.dto.payment.PaymentInitiateReq;
import com.mobilebackend.ungdunglapkehoachdulich.dto.payment.PaymentInitiateRes;
import com.mobilebackend.ungdunglapkehoachdulich.model.BookingMaster;
import com.mobilebackend.ungdunglapkehoachdulich.model.Payment;
import com.mobilebackend.ungdunglapkehoachdulich.repo.BookingMasterRepo;
import com.mobilebackend.ungdunglapkehoachdulich.repo.PaymentRepo;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private static final DateTimeFormatter VNPAY_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final BookingMasterRepo bookingMasterRepo;
    private final PaymentRepo paymentRepo;
    private final PaymentGatewayProperties paymentGatewayProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Transactional
    public PaymentInitiateRes initiatePayment(Integer tripId, Integer bookingId, PaymentInitiateReq req, String clientIp) {
        BookingMaster booking = bookingMasterRepo.findByIdAndTripId(bookingId, tripId)
                .orElseThrow(() -> new IllegalArgumentException("Booking khong ton tai hoac khong thuoc trip"));

        String provider = normalizeProvider(req == null ? null : req.getProvider());
        float amount = resolveAmount(req == null ? null : req.getAmount(), booking.getTotalAmount());
        String transactionNo = generateTransactionNo(provider);
        String paymentUrl;

        Payment payment = Payment.builder()
                .bookingMasterId(bookingId)
                .transactionNo(transactionNo)
                .provider(provider)
                .amount(amount)
                .status("PENDING")
                .paymentDate(LocalDate.now())
                .build();
        paymentRepo.save(payment);

        if ("VNPAY".equals(provider)) {
            paymentUrl = buildVnpayPayUrl(transactionNo, amount, req == null ? null : req.getOrderInfo(), req == null ? null : req.getReturnUrl(), clientIp);
        } else if ("MOMO".equals(provider)) {
            paymentUrl = createMomoPayment(transactionNo, amount, req == null ? null : req.getOrderInfo(), req == null ? null : req.getReturnUrl(), req == null ? null : req.getIpnUrl(), req == null ? null : req.getRequestType());
        } else {
            throw new IllegalArgumentException("Provider thanh toan khong ho tro. Chi ho tro vnpay, momo");
        }

        return PaymentInitiateRes.builder()
                .bookingId(bookingId)
                .provider(provider)
                .transactionNo(transactionNo)
                .paymentUrl(paymentUrl)
                .status("PENDING")
                .build();
    }

    @Transactional
    public Map<String, Object> handleVnpayReturn(Map<String, String> params) {
        boolean validSignature = verifyVnpaySignature(params);
        String transactionNo = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        if (!validSignature) {
            Map<String, Object> invalid = new LinkedHashMap<>();
            invalid.put("provider", "VNPAY");
            invalid.put("transactionNo", transactionNo);
            invalid.put("signatureValid", false);
            invalid.put("responseCode", responseCode);
            invalid.put("status", "INVALID_SIGNATURE");
            return invalid;
        }
        boolean success = validSignature && "00".equals(responseCode);

        Payment payment = findPayment(transactionNo, "VNPAY");
        updatePaymentAndBookingStatus(payment, success);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", "VNPAY");
        result.put("transactionNo", transactionNo);
        result.put("signatureValid", validSignature);
        result.put("responseCode", responseCode);
        result.put("status", success ? "PAID" : "FAILED");
        return result;
    }

    @Transactional
    public Map<String, String> handleVnpayIpn(Map<String, String> params) {
        if (!verifyVnpaySignature(params)) {
            return vnpayIpnResponse("97", "Invalid signature");
        }
        String transactionNo = params.get("vnp_TxnRef");
        Payment payment = findPayment(transactionNo, "VNPAY");

        if ("SUCCESS".equalsIgnoreCase(payment.getStatus())) {
            return vnpayIpnResponse("00", "Confirm Success");
        }

        boolean success = "00".equals(params.get("vnp_ResponseCode"));
        updatePaymentAndBookingStatus(payment, success);
        return vnpayIpnResponse("00", "Confirm Success");
    }

    @Transactional
    public Map<String, Object> handleMomoWebhook(String requestBody) {
        JsonNode body = parseJson(requestBody);
        boolean validSignature = verifyMomoSignature(body);
        String transactionNo = text(body, "orderId");
        int resultCode = intValue(body, "resultCode", -1);
        if (!validSignature) {
            Map<String, Object> invalid = new LinkedHashMap<>();
            invalid.put("provider", "MOMO");
            invalid.put("transactionNo", transactionNo);
            invalid.put("resultCode", 13);
            invalid.put("message", "Invalid signature");
            return invalid;
        }
        boolean success = validSignature && resultCode == 0;

        Payment payment = findPayment(transactionNo, "MOMO");
        updatePaymentAndBookingStatus(payment, success);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("provider", "MOMO");
        response.put("transactionNo", transactionNo);
        response.put("resultCode", 0);
        response.put("message", "OK");
        return response;
    }

    private String buildVnpayPayUrl(String transactionNo, float amount, String orderInfo, String returnUrl, String clientIp) {
        PaymentGatewayProperties.Vnpay config = paymentGatewayProperties.getVnpay();
        ensureVnpayConfigured(config);
        String hashSecret = vnpaySecret(config);

        String effectiveOrderInfo = isBlank(orderInfo) ? "Thanh toan booking " + transactionNo : orderInfo;
        String effectiveReturnUrl = isBlank(returnUrl) ? config.getReturnUrl() : returnUrl;
        if (isBlank(effectiveReturnUrl)) {
            throw new IllegalStateException("Chua cau hinh returnUrl cho VNPay");
        }

        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDateTime expire = now.plusMinutes(15);

        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", vnpayTmnCode(config));
        params.put("vnp_Amount", String.valueOf(toVndSubunit(amount)));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", transactionNo);
        params.put("vnp_OrderInfo", effectiveOrderInfo);
        params.put("vnp_OrderType", isBlank(config.getOrderType()) ? "other" : config.getOrderType());
        params.put("vnp_Locale", isBlank(config.getLocale()) ? "vn" : config.getLocale());
        params.put("vnp_ReturnUrl", effectiveReturnUrl);
        params.put("vnp_IpAddr", isBlank(clientIp) ? "127.0.0.1" : clientIp);
        params.put("vnp_CreateDate", VNPAY_TIME_FORMAT.format(now));
        params.put("vnp_ExpireDate", VNPAY_TIME_FORMAT.format(expire));

        String hashData = buildVnpayQueryString(params, true);
        String queryData = buildVnpayQueryString(params, true);
        String secureHash = hmac("HmacSHA512", hashSecret, hashData);

        return trimTrailingSlash(config.getBaseUrl()) + "?" + queryData + "&vnp_SecureHash=" + secureHash;
    }

    private String createMomoPayment(String transactionNo, float amount, String orderInfo, String redirectUrl, String ipnUrl, String requestType) {
        PaymentGatewayProperties.Momo config = paymentGatewayProperties.getMomo();
        ensureMomoConfigured(config);

        String effectiveRedirectUrl = isBlank(redirectUrl) ? config.getRedirectUrl() : redirectUrl;
        String effectiveIpnUrl = isBlank(ipnUrl) ? config.getIpnUrl() : ipnUrl;
        if (isBlank(effectiveRedirectUrl) || isBlank(effectiveIpnUrl)) {
            throw new IllegalStateException("Chua cau hinh redirectUrl/ipnUrl cho MoMo");
        }

        String reqType = isBlank(requestType) ? config.getRequestType() : requestType;
        String reqId = "MOMO_" + UUID.randomUUID();
        String effectiveOrderInfo = isBlank(orderInfo) ? "Thanh toan booking " + transactionNo : orderInfo;
        String amountStr = String.valueOf(toVndAmount(amount));

        String rawSignature = "accessKey=" + config.getAccessKey()
                + "&amount=" + amountStr
                + "&extraData="
                + "&ipnUrl=" + effectiveIpnUrl
                + "&orderId=" + transactionNo
                + "&orderInfo=" + effectiveOrderInfo
                + "&partnerCode=" + config.getPartnerCode()
                + "&redirectUrl=" + effectiveRedirectUrl
                + "&requestId=" + reqId
                + "&requestType=" + reqType;

        String signature = hmac("HmacSHA256", config.getSecretKey(), rawSignature);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("partnerCode", config.getPartnerCode());
        payload.put("partnerName", "BackendMobile");
        payload.put("storeId", "BackendMobileStore");
        payload.put("requestId", reqId);
        payload.put("amount", amountStr);
        payload.put("orderId", transactionNo);
        payload.put("orderInfo", effectiveOrderInfo);
        payload.put("redirectUrl", effectiveRedirectUrl);
        payload.put("ipnUrl", effectiveIpnUrl);
        payload.put("lang", isBlank(config.getLang()) ? "vi" : config.getLang());
        payload.put("extraData", "");
        payload.put("requestType", reqType);
        payload.put("signature", signature);

        try {
            String body = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder(URI.create(trimTrailingSlash(config.getBaseUrl()) + "/v2/gateway/api/create"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("MoMo API error " + response.statusCode() + ": " + response.body());
            }

            JsonNode json = parseJson(response.body());
            int resultCode = intValue(json, "resultCode", -1);
            if (resultCode != 0) {
                throw new IllegalStateException("MoMo create payment fail: " + text(json, "message"));
            }
            String payUrl = text(json, "payUrl");
            if (isBlank(payUrl)) {
                throw new IllegalStateException("MoMo response khong co payUrl");
            }
            return payUrl;
        } catch (IOException e) {
            throw new IllegalStateException("Khong the goi MoMo API", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("MoMo API bi ngat", e);
        }
    }

    private Payment findPayment(String transactionNo, String provider) {
        if (isBlank(transactionNo)) {
            throw new IllegalArgumentException("Thieu transactionNo tu callback");
        }
        return paymentRepo.findByTransactionNoAndProvider(transactionNo, provider)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay payment theo transactionNo"));
    }

    private void updatePaymentAndBookingStatus(Payment payment, boolean success) {
        if (payment == null) {
            return;
        }

        String nextPaymentStatus = success ? "SUCCESS" : "FAILED";
        payment.setStatus(nextPaymentStatus);
        paymentRepo.save(payment);

        BookingMaster booking = bookingMasterRepo.findById(payment.getBookingMasterId())
                .orElseThrow(() -> new IllegalArgumentException("Booking khong ton tai"));

        if (success) {
            booking.setPaymentStatus("PAID");
        } else if (!"PAID".equalsIgnoreCase(booking.getPaymentStatus())) {
            booking.setPaymentStatus("FAILED");
        }
        bookingMasterRepo.save(booking);
    }

    private boolean verifyVnpaySignature(Map<String, String> params) {
        PaymentGatewayProperties.Vnpay config = paymentGatewayProperties.getVnpay();
        ensureVnpayConfigured(config);
        String hashSecret = vnpaySecret(config);

        String secureHash = params.get("vnp_SecureHash");
        if (isBlank(secureHash)) {
            return false;
        }

        Map<String, String> filtered = new TreeMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (isBlank(key) || isBlank(value)) {
                continue;
            }
            if (!key.startsWith("vnp_")) {
                continue;
            }
            if ("vnp_SecureHash".equals(key) || "vnp_SecureHashType".equals(key)) {
                continue;
            }
            filtered.put(key, value);
        }

        String hashData = buildVnpayQueryString(filtered, true);
        String computed = hmac("HmacSHA512", hashSecret, hashData);
        return secureHash.equalsIgnoreCase(computed);
    }

    private boolean verifyMomoSignature(JsonNode body) {
        PaymentGatewayProperties.Momo config = paymentGatewayProperties.getMomo();
        ensureMomoConfigured(config);

        String providedSignature = text(body, "signature");
        if (isBlank(providedSignature)) {
            return false;
        }

        String rawSignature = "accessKey=" + config.getAccessKey()
                + "&amount=" + text(body, "amount")
                + "&extraData=" + text(body, "extraData")
                + "&message=" + text(body, "message")
                + "&orderId=" + text(body, "orderId")
                + "&orderInfo=" + text(body, "orderInfo")
                + "&orderType=" + text(body, "orderType")
                + "&partnerCode=" + text(body, "partnerCode")
                + "&payType=" + text(body, "payType")
                + "&requestId=" + text(body, "requestId")
                + "&responseTime=" + text(body, "responseTime")
                + "&resultCode=" + text(body, "resultCode")
                + "&transId=" + text(body, "transId");

        String computedSignature = hmac("HmacSHA256", config.getSecretKey(), rawSignature);
        return providedSignature.equalsIgnoreCase(computedSignature);
    }

    /**
     * Chuỗi ký VNPAY v2.1.0 giống demo PHP: ksort, hashdata = urlencode(key)=urlencode(value)&..., HMAC-SHA512(hashdata, secret).
     * Phải dùng {@link URLEncoder} (khoảng trắng → {@code +}) — không đổi sang %20 / chỉnh ~ (dễ lệch chữ ký so với cổng).
     */
    private String buildVnpayQueryString(Map<String, String> params, boolean encode) {
        return params.entrySet().stream()
                .filter(entry -> !isBlank(entry.getKey()) && !isBlank(entry.getValue()))
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    String key = encode ? vnpPayUrlEncode(entry.getKey()) : entry.getKey();
                    String value = encode ? vnpPayUrlEncode(entry.getValue()) : entry.getValue();
                    return key + "=" + value;
                })
                .reduce((left, right) -> left + "&" + right)
                .orElse("");
    }

    private String vnpPayUrlEncode(String raw) {
        return URLEncoder.encode(Objects.requireNonNullElse(raw, ""), StandardCharsets.UTF_8);
    }

    private String vnpaySecret(PaymentGatewayProperties.Vnpay config) {
        return config.getHashSecret() == null ? "" : config.getHashSecret().trim();
    }

    private String vnpayTmnCode(PaymentGatewayProperties.Vnpay config) {
        return config.getTmnCode() == null ? "" : config.getTmnCode().trim();
    }

    private Map<String, String> vnpayIpnResponse(String code, String message) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("RspCode", code);
        response.put("Message", message);
        return response;
    }

    private long toVndSubunit(float amount) {
        BigDecimal bd = BigDecimal.valueOf(amount).setScale(0, RoundingMode.HALF_UP);
        return bd.longValue() * 100L;
    }

    private long toVndAmount(float amount) {
        return BigDecimal.valueOf(amount).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private float resolveAmount(Float requestedAmount, Float bookingAmount) {
        float amount = requestedAmount != null && requestedAmount > 0F
                ? requestedAmount
                : (bookingAmount == null ? 0F : bookingAmount);
        if (amount <= 0F) {
            throw new IllegalArgumentException("amount phai lon hon 0");
        }
        return amount;
    }

    private String normalizeProvider(String provider) {
        if (isBlank(provider)) {
            throw new IllegalArgumentException("provider thanh toan la bat buoc");
        }
        return provider.trim().toUpperCase(Locale.ROOT);
    }

    private JsonNode parseJson(String body) {
        try {
            return objectMapper.readTree(body == null ? "{}" : body);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Body JSON khong hop le", e);
        }
    }

    private String text(JsonNode node, String field) {
        if (node == null || isBlank(field)) {
            return "";
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? "" : value.asText("");
    }

    private int intValue(JsonNode node, String field, int defaultValue) {
        if (node == null || isBlank(field)) {
            return defaultValue;
        }
        JsonNode value = node.get(field);
        return value == null || !value.isNumber() ? defaultValue : value.intValue();
    }

    private void ensureVnpayConfigured(PaymentGatewayProperties.Vnpay config) {
        if (config == null || isBlank(vnpayTmnCode(config)) || isBlank(vnpaySecret(config))) {
            throw new IllegalStateException("Chua cau hinh travel.payment.vnpay.tmn-code/hash-secret");
        }
    }

    private void ensureMomoConfigured(PaymentGatewayProperties.Momo config) {
        if (config == null || isBlank(config.getPartnerCode()) || isBlank(config.getAccessKey()) || isBlank(config.getSecretKey())) {
            throw new IllegalStateException("Chua cau hinh travel.payment.momo.partner-code/access-key/secret-key");
        }
    }

    private String generateTransactionNo(String provider) {
        return provider + "_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
    }

    private String hmac(String algorithm, String secret, String data) {
        try {
            Mac mac = Mac.getInstance(algorithm);
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm);
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Khong the ky chu ky " + algorithm, e);
        }
    }

    private String trimTrailingSlash(String value) {
        if (isBlank(value)) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}



