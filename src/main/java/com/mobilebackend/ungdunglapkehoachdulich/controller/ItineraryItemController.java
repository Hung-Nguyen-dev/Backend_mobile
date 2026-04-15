package com.mobilebackend.ungdunglapkehoachdulich.controller;

import com.mobilebackend.ungdunglapkehoachdulich.dto.itinerary.ItineraryItemCreateReq;
import com.mobilebackend.ungdunglapkehoachdulich.dto.itinerary.ItineraryItemRes;
import com.mobilebackend.ungdunglapkehoachdulich.service.ItineraryItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/itinerary-items")
@RequiredArgsConstructor
public class ItineraryItemController {
    private final ItineraryItemService itineraryItemService;

    @PostMapping
    public ResponseEntity<?> create(
            @RequestHeader("X-User-Id") Integer userId,
            @RequestBody ItineraryItemCreateReq req
    ) {
        try {
            ItineraryItemRes created = itineraryItemService.create(userId, req);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Khong the them vao hanh trinh");
        }
    }
}
