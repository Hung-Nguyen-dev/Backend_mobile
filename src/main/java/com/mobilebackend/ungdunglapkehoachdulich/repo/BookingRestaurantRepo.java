package com.mobilebackend.ungdunglapkehoachdulich.repo;

import com.mobilebackend.ungdunglapkehoachdulich.model.BookingRestaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookingRestaurantRepo extends JpaRepository<BookingRestaurant, Integer> {
    Optional<BookingRestaurant> findByBookingMasterId(Integer bookingMasterId);
    void deleteByBookingMasterId(Integer bookingMasterId);
}

