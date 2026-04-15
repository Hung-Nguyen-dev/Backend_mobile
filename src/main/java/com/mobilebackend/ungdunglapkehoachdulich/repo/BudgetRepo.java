package com.mobilebackend.ungdunglapkehoachdulich.repo;

import com.mobilebackend.ungdunglapkehoachdulich.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepo extends JpaRepository<Budget, Integer> {
    List<Budget> findByTripId(Integer tripId);

    Optional<Budget> findByTripIdAndCategory(Integer tripId, String category);
    void deleteByTripId(Integer tripId);
}

