package com.mobilebackend.ungdunglapkehoachdulich.controller;

import com.mobilebackend.ungdunglapkehoachdulich.dto.booking.*;
import com.mobilebackend.ungdunglapkehoachdulich.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.function.Supplier;

@RestController
@RequestMapping("/api/v1/trips/{tripId}")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @PostMapping("/bookings/flights")
    public ResponseEntity<?> createFlightBooking(@PathVariable Integer tripId, @RequestBody FlightBookingReq req) {
        return process(() -> bookingService.createFlightBooking(tripId, req));
    }

    @PostMapping("/bookings/hotels")
    public ResponseEntity<?> createHotelBooking(@PathVariable Integer tripId, @RequestBody HotelBookingReq req) {
        return process(() -> bookingService.createHotelBooking(tripId, req));
    }

    @PostMapping("/bookings/coaches")
    public ResponseEntity<?> createCoachBooking(@PathVariable Integer tripId, @RequestBody CoachBookingReq req) {
        return process(() -> bookingService.createCoachBooking(tripId, req));
    }

    @PostMapping("/bookings/restaurants")
    public ResponseEntity<?> createRestaurantBooking(@PathVariable Integer tripId, @RequestBody RestaurantBookingReq req) {
        return process(() -> bookingService.createRestaurantBooking(tripId, req));
    }

    @GetMapping("/bookings")
    public ResponseEntity<?> getTripBookings(@PathVariable Integer tripId) {
        return process(() -> bookingService.getTripBookings(tripId));
    }

    @PatchMapping("/bookings/{bookingId}/payment-status")
    public ResponseEntity<?> updatePaymentStatus(
            @PathVariable Integer tripId,
            @PathVariable Integer bookingId,
            @RequestParam String status
    ) {
        return process(() -> bookingService.updatePaymentStatus(tripId, bookingId, status));
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



