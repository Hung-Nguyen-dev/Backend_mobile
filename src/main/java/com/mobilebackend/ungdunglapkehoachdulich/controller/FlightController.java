package com.mobilebackend.ungdunglapkehoachdulich.controller;

import com.mobilebackend.ungdunglapkehoachdulich.dto.flight.FlightSearchReq;
import com.mobilebackend.ungdunglapkehoachdulich.service.flight.FlightService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.function.Supplier;

@RestController
@RequestMapping("/api/v1/flights")
@RequiredArgsConstructor
public class FlightController {
    private final FlightService flightService;

    @GetMapping("/search")
    public ResponseEntity<?> searchFlights(@ModelAttribute FlightSearchReq req) {
        return process(() -> flightService.searchFlights(req));
    }

    @PostMapping("/price")
    public ResponseEntity<?> priceFlight(@RequestBody String flightOfferPayload) {
        return process(() -> flightService.priceFlightOffer(flightOfferPayload));
    }

    @PostMapping("/book")
    public ResponseEntity<?> bookFlight(@RequestBody String bookingRequest) {
        return process(() -> flightService.bookFlight(bookingRequest));
    }

    private ResponseEntity<?> process(Supplier<Object> action) {
        try {
            return ResponseEntity.ok(action.get());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Loi he thong");
        }
    }
}



