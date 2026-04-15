package com.mobilebackend.ungdunglapkehoachdulich.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Loi ghi JSON (vd. NaN trong so thuc) xay ra sau khi controller return — khong vao catch trong controller.
 */
@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(HttpMessageNotWritableException.class)
    public ResponseEntity<Map<String, Object>> handleNotWritable(HttpMessageNotWritableException e) {
        log.warn("Khong serialize duoc JSON phan hoi: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "SERIALIZATION",
                "message", "Khong ghi duoc JSON (kiem tra log server: NaN/Infinity trong so thuc hoac DTO)."
        ));
    }
}
