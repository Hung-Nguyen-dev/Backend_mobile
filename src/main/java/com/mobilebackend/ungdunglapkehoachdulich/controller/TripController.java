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

    @PostMapping("/create-trip")
    public void TripCreateController(@PathVariable("userId") Integer userId, @RequestBody TripReq tripReq){
        try{
            tripService.TripCreateService(userId,tripReq);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}