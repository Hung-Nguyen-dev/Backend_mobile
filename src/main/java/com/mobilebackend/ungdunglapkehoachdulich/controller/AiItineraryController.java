package com.mobilebackend.ungdunglapkehoachdulich.controller;

import com.mobilebackend.ungdunglapkehoachdulich.dto.ai.AiItineraryDtos.AiItineraryRequest;
import com.mobilebackend.ungdunglapkehoachdulich.dto.ai.AiItineraryDtos.AiItineraryResponse;
import com.mobilebackend.ungdunglapkehoachdulich.service.AiItineraryService;
import com.mobilebackend.ungdunglapkehoachdulich.service.CityDataService;
import com.mobilebackend.ungdunglapkehoachdulich.service.CityDataService.CityPlace;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
@CrossOrigin(origins = "*")
public class AiItineraryController {

    private final AiItineraryService aiItineraryService;
    private final CityDataService cityDataService;

    public AiItineraryController(AiItineraryService aiItineraryService, CityDataService cityDataService) {
        this.aiItineraryService = aiItineraryService;
        this.cityDataService = cityDataService;
    }

    @PostMapping("/itinerary")
    public AiItineraryResponse suggest(@RequestBody AiItineraryRequest req) {
        return aiItineraryService.generate(req);
    }

    @GetMapping("/city-data")
    public List<CityPlace> cityData(
            @RequestParam String destination,
            @RequestParam String category,
            @RequestParam(required = false) String query
    ) {
        return cityDataService.searchPlaces(destination, category, query);
    }
}
