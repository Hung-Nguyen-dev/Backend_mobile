package com.example.travel.places;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {
  List<Place> findByCityIgnoreCaseAndType(String city, PlaceType type);

  Optional<Place> findBySourceAndSourcePlaceId(String source, String sourcePlaceId);
}

