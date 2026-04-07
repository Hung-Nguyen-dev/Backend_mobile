package com.mobilebackend.ungdunglapkehoachdulich.repo;

import com.mobilebackend.ungdunglapkehoachdulich.model.ExpenseSplit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpenseSplitRepo extends JpaRepository<ExpenseSplit, Integer> {
    List<ExpenseSplit> findByExpenseId(Integer expenseId);
}

