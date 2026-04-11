package com.example.travel.places;

import java.util.stream.Collectors;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/places")
public class PlaceController {

  private final PlaceRepository placeRepository;
  private final PlaceSyncService placeSyncService;

  public PlaceController(PlaceRepository placeRepository, PlaceSyncService placeSyncService) {
    this.placeRepository = placeRepository;
    this.placeSyncService = placeSyncService;
  }

  @PostMapping("/sync")
  public ResponseEntity<Map<String, Object>> syncCity(@RequestParam String city) {
    int added = placeSyncService.syncCity(city);
    Map<String, Object> body = new HashMap<>();
    body.put("city", city);
    body.put("added", added);
    return ResponseEntity.ok(body);
  }

  @GetMapping
  public List<Place> list(@RequestParam String city, @RequestParam(required = false) PlaceType type) {
    if (type != null) {
      return placeRepository.findByCityIgnoreCaseAndType(city, type);
    }
    return placeRepository.findAll().stream()
        .filter(p -> p.getCity().equalsIgnoreCase(city))
        .collect(Collectors.toList());
  }
}

