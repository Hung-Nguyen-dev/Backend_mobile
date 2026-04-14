package com.mobilebackend.ungdunglapkehoachdulich.repo;

import com.mobilebackend.ungdunglapkehoachdulich.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TripRepo extends JpaRepository<Trip, Integer> {
    List<Trip> findByUserIdOrderByIdDesc(Integer userId);

    @Query("SELECT DISTINCT t FROM Trip t LEFT JOIN TripMember tm ON t.id = tm.tripId " +
           "WHERE t.userId = :userId OR (tm.userId = :userId AND tm.status = 1) ORDER BY t.id DESC")
    List<Trip> findAllByOwnerOrMemberOrderByIdDesc(@Param("userId") Integer userId);
}
