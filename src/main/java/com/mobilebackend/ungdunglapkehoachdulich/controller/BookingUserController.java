package com.mobilebackend.ungdunglapkehoachdulich.controller;

import com.mobilebackend.ungdunglapkehoachdulich.dto.booking.FlightBookingReq;
import com.mobilebackend.ungdunglapkehoachdulich.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.function.Supplier;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BookingUserController {
    private final BookingService bookingService;

    @PostMapping("/bookings/flights")
    public ResponseEntity<?> createFlightBookingWithoutTrip(@RequestBody FlightBookingReq req) {
        return process(() -> bookingService.createFlightBookingWithoutTrip(req));
    }

    @GetMapping("/users/{userId}/bookings")
    public ResponseEntity<?> getUserBookings(@PathVariable Integer userId) {
        return process(() -> bookingService.getUserBookings(userId));
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
