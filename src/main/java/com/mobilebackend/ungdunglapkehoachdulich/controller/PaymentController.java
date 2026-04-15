package com.mobilebackend.ungdunglapkehoachdulich.controller;

import com.mobilebackend.ungdunglapkehoachdulich.dto.payment.PaymentInitiateReq;
import com.mobilebackend.ungdunglapkehoachdulich.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/trips/{tripId}/bookings/{bookingId}/payments/initiate")
    public ResponseEntity<?> initiatePayment(
            @PathVariable Integer tripId,
            @PathVariable Integer bookingId,
            @RequestBody PaymentInitiateReq req,
            HttpServletRequest httpServletRequest
    ) {
        String clientIp = resolveClientIp(httpServletRequest);
        return process(() -> paymentService.initiatePayment(tripId, bookingId, req, clientIp));
    }

    @PostMapping("/users/{userId}/bookings/{bookingId}/payments/initiate")
    public ResponseEntity<?> initiatePaymentWithoutTrip(
            @PathVariable Integer userId,
            @PathVariable Integer bookingId,
            @RequestBody PaymentInitiateReq req,
            HttpServletRequest httpServletRequest
    ) {
        String clientIp = resolveClientIp(httpServletRequest);
        return process(() -> paymentService.initiatePaymentWithoutTrip(userId, bookingId, req, clientIp));
    }

    @GetMapping("/payments/vnpay/callback")
    public ResponseEntity<?> vnpayReturn(@RequestParam Map<String, String> params) {
        try {
            Object result = paymentService.handleVnpayReturn(params);
            String appReturnUrl = params.get("appReturnUrl");
            if (appReturnUrl != null && !appReturnUrl.isBlank() && result instanceof Map<?, ?> resultMap) {
                Map<String, String> query = new LinkedHashMap<>();
                query.put("provider", String.valueOf(resultMap.get("provider") == null ? "VNPAY" : resultMap.get("provider")));
                query.put("status", String.valueOf(resultMap.get("status") == null ? "UNKNOWN" : resultMap.get("status")));
                query.put("transactionNo", String.valueOf(resultMap.get("transactionNo") == null ? "" : resultMap.get("transactionNo")));
                query.put("tripId", params.getOrDefault("tripId", ""));
                query.put("bookingId", params.getOrDefault("bookingId", ""));

                UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(appReturnUrl);
                query.forEach(builder::queryParam);
                URI target = builder.build(true).toUri();
                return ResponseEntity.status(HttpStatus.FOUND).location(target).build();
            }
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Loi he thong");
        }
    }

    @GetMapping("/payments/vnpay/ipn")
    public ResponseEntity<?> vnpayIpn(@RequestParam Map<String, String> params) {
        return process(() -> paymentService.handleVnpayIpn(params));
    }

    @PostMapping("/payments/momo/webhook")
    public ResponseEntity<?> momoWebhook(@RequestBody String body) {
        return process(() -> paymentService.handleMomoWebhook(body));
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "127.0.0.1";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String ip = request.getRemoteAddr();
        return (ip == null || ip.isBlank()) ? "127.0.0.1" : ip;
    }

    private ResponseEntity<?> process(Supplier<Object> action) {
        try {
            return ResponseEntity.ok(action.get());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Loi he thong");
        }
    }
}

