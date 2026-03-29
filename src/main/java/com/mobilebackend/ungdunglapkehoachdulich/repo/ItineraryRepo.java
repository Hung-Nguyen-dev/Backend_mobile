package com.mobilebackend.ungdunglapkehoachdulich.repo;

import com.mobilebackend.ungdunglapkehoachdulich.model.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItineraryRepo extends JpaRepository<Integer, Itinerary> {
    Itinerary save(Itinerary itinerary);
}
