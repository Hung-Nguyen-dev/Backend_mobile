package com.mobilebackend.ungdunglapkehoachdulich.controller;

import com.mobilebackend.ungdunglapkehoachdulich.dto.TripReq;
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


}