package com.mobilebackend.ungdunglapkehoachdulich.repo;

import com.mobilebackend.ungdunglapkehoachdulich.model.ItineraryItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItineraryItemRepo extends JpaRepository<ItineraryItem, Integer> {
    List<ItineraryItem> findByTripIdOrderByCreatedAtDesc(Integer tripId);
    void deleteByTripId(Integer tripId);
}
