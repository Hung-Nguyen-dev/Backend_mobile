package com.mobilebackend.ungdunglapkehoachdulich.repo;

import com.mobilebackend.ungdunglapkehoachdulich.model.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripRepo extends JpaRepository<Trip, Integer> {
	List<Trip> findByUserIdOrderByIdDesc(Integer userId);
}
