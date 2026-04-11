package com.example.travel.osm;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OverpassClient {

  private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";
  private final RestTemplate restTemplate = new RestTemplate();

  public OverpassResponse query(String overpassQl) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.TEXT_PLAIN);
    HttpEntity<String> entity = new HttpEntity<>(overpassQl, headers);

    ResponseEntity<OverpassResponse> res =
        restTemplate.postForEntity(OVERPASS_URL, entity, OverpassResponse.class);

    if (!res.getStatusCode().is2xxSuccessful() || res.getBody() == null) {
      throw new IllegalStateException("Overpass error: " + res.getStatusCode());
    }
    return res.getBody();
  }
}

