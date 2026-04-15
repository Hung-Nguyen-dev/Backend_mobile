package com.mobilebackend.ungdunglapkehoachdulich.repo;

import com.mobilebackend.ungdunglapkehoachdulich.model.BookingFlight;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookingFlightRepo extends JpaRepository<BookingFlight, Integer> {
    Optional<BookingFlight> findByBookingMasterId(Integer bookingMasterId);
}

