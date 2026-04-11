package com.example.travel.ai;

import com.example.travel.ai.AiItineraryDtos.AiItineraryRequest;
import com.example.travel.ai.AiItineraryDtos.AiItineraryResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai")
public class AiItineraryController {

  private final AiItineraryService aiItineraryService;

  public AiItineraryController(AiItineraryService aiItineraryService) {
    this.aiItineraryService = aiItineraryService;
  }

  @PostMapping("/itinerary")
  public AiItineraryResponse suggest(@RequestBody AiItineraryRequest req) {
    return aiItineraryService.generate(req);
  }
}

