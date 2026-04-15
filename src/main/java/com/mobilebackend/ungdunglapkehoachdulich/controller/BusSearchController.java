package com.mobilebackend.ungdunglapkehoachdulich.controller;

import com.mobilebackend.ungdunglapkehoachdulich.dto.bus.BusSearchRes;
import com.mobilebackend.ungdunglapkehoachdulich.service.BusSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bus")
@RequiredArgsConstructor
public class BusSearchController {
    private final BusSearchService busSearchService;

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam String departureDate,
            @RequestParam(required = false) Integer limit
    ) {
        try {
            BusSearchRes result = busSearchService.search(from, to, departureDate, limit);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(503).body("Khong the tim kiem ve xe luc nay");
        }
    }
}
