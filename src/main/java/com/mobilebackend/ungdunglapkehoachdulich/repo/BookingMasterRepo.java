package com.mobilebackend.ungdunglapkehoachdulich.repo;

import com.mobilebackend.ungdunglapkehoachdulich.model.BookingMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingMasterRepo extends JpaRepository<BookingMaster, Integer> {
    List<BookingMaster> findByTripId(Integer tripId);
}

