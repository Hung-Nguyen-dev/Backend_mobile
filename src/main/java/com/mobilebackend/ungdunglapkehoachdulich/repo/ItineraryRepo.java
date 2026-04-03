package com.mobilebackend.ungdunglapkehoachdulich.repo;

import com.mobilebackend.ungdunglapkehoachdulich.model.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItineraryRepo extends JpaRepository<Itinerary, Integer> {

    /** Lấy tất cả các ngày của một chuyến đi, sắp xếp theo số ngày */
    List<Itinerary> findByTripIdOrderByDayNumberAsc(Integer tripId);
}
