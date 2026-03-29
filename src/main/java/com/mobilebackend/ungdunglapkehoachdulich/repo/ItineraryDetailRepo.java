package com.mobilebackend.ungdunglapkehoachdulich.repo;

import com.mobilebackend.ungdunglapkehoachdulich.model.ItineraryDetail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItineraryDetailRepo extends JpaRepository<Integer, ItineraryDetail> {
    ItineraryDetail save(ItineraryDetail itineraryDetail);
}
