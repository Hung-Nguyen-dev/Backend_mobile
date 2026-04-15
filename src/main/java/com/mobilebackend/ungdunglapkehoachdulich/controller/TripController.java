package com.mobilebackend.ungdunglapkehoachdulich.controller;

import com.mobilebackend.ungdunglapkehoachdulich.dto.TripReq;
import com.mobilebackend.ungdunglapkehoachdulich.dto.UpdateTripStopReq;
import com.mobilebackend.ungdunglapkehoachdulich.service.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TripController {
    private final TripService tripService;

    @PostMapping("/create-trip/{userId}")
    public ResponseEntity<Integer> TripCreateController(@PathVariable("userId") Integer userId, @RequestBody TripReq tripReq){
        try{
            Integer tripId = tripService.TripCreateService(userId, tripReq);
            return ResponseEntity.ok(tripId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @DeleteMapping("/trips/{tripId}")
    public ResponseEntity<Void> deleteTripController(@PathVariable("tripId") Integer tripId, @RequestHeader("X-User-Id") Integer userId) {
        try {
            tripService.deleteTrip(tripId, userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("/trips/{tripId}/trip-journal")
    public ResponseEntity<com.mobilebackend.ungdunglapkehoachdulich.dto.JournalRes> getTripJournal(@PathVariable("tripId") Integer tripId, @RequestHeader("X-User-Id") Integer userId) {
        try {
            return ResponseEntity.ok(tripService.getTripJournal(tripId, userId));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/itinerary-details/{id}/check-in")
    public ResponseEntity<Void> checkInLocation(@PathVariable("id") Integer postItineraryDetailId, @RequestHeader("X-User-Id") Integer userId) {
        try {
            tripService.checkInLocation(postItineraryDetailId, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @PutMapping("/trips/journal/stops/{itineraryDetailId}")
    public ResponseEntity<Void> updateTripStop(
            @PathVariable Integer itineraryDetailId,
            @RequestHeader("X-User-Id") Integer userId,
            @RequestBody UpdateTripStopReq req
    ) {
        try {
            tripService.updateTripStop(itineraryDetailId, userId, req);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @DeleteMapping("/trips/journal/stops/{itineraryDetailId}")
    public ResponseEntity<Void> deleteTripStop(
            @PathVariable Integer itineraryDetailId,
            @RequestHeader("X-User-Id") Integer userId
    ) {
        try {
            tripService.deleteTripStop(itineraryDetailId, userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}