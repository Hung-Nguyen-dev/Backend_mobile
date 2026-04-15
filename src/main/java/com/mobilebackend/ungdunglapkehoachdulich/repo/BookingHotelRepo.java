package com.mobilebackend.ungdunglapkehoachdulich.repo;

import com.mobilebackend.ungdunglapkehoachdulich.model.BookingHotel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookingHotelRepo extends JpaRepository<BookingHotel, Integer> {
    Optional<BookingHotel> findByBookingMasterId(Integer bookingMasterId);
    void deleteByBookingMasterId(Integer bookingMasterId);
}

