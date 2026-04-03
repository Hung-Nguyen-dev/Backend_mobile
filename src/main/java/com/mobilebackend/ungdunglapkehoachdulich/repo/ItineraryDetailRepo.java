package com.mobilebackend.ungdunglapkehoachdulich.repo;

import com.mobilebackend.ungdunglapkehoachdulich.model.ItineraryDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItineraryDetailRepo extends JpaRepository<ItineraryDetail, Integer> {

    /** Lấy tất cả hoạt động theo itinerary (ngày), sắp xếp theo giờ thăm */
    List<ItineraryDetail> findByItineraryIdOrderByVisitTimeAsc(Integer itineraryId);
}
