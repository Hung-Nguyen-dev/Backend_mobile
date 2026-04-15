package com.mobilebackend.ungdunglapkehoachdulich.controller;

import com.mobilebackend.ungdunglapkehoachdulich.dto.finance.BudgetUpsertReq;
import com.mobilebackend.ungdunglapkehoachdulich.dto.finance.ExpenseCreateReq;
import com.mobilebackend.ungdunglapkehoachdulich.dto.finance.ExpenseSplitReq;
import com.mobilebackend.ungdunglapkehoachdulich.dto.finance.ExpenseUpdateReq;
import com.mobilebackend.ungdunglapkehoachdulich.service.FinanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/v1/trips/{tripId}")
@RequiredArgsConstructor
public class FinanceController {
    private final FinanceService financeService;

    @PutMapping("/budget")
    public ResponseEntity<?> upsertBudget(@PathVariable Integer tripId, @RequestBody BudgetUpsertReq req) {
        return process(() -> financeService.upsertBudget(tripId, req));
    }

    @GetMapping("/budget")
    public ResponseEntity<?> getTripBudgets(@PathVariable Integer tripId) {
        return process(() -> financeService.getTripBudgets(tripId));
    }

    @PostMapping("/expenses")
    public ResponseEntity<?> createExpense(@PathVariable Integer tripId, @RequestBody ExpenseCreateReq req) {
        return process(() -> financeService.createExpense(tripId, req));
    }

    @GetMapping("/expenses")
    public ResponseEntity<?> getTripExpenses(
            @PathVariable Integer tripId,
            @RequestParam(name = "fromDate", required = false) String fromDate,
            @RequestParam(name = "toDate", required = false) String toDate
    ) {
        return process(() -> financeService.getTripExpenses(tripId, fromDate, toDate));
    }

    @PutMapping("/expenses/{expenseId}")
    public ResponseEntity<?> updateExpense(
            @PathVariable Integer tripId,
            @PathVariable Integer expenseId,
            @RequestBody ExpenseUpdateReq req
    ) {
        return process(() -> financeService.updateExpense(tripId, expenseId, req));
    }

    @DeleteMapping("/expenses/{expenseId}")
    public ResponseEntity<?> deleteExpense(
            @PathVariable Integer tripId,
            @PathVariable Integer expenseId
    ) {
        return process(() -> financeService.deleteExpense(tripId, expenseId));
    }

    @PostMapping("/expenses/{expenseId}/splits")
    public ResponseEntity<?> addExpenseSplits(
            @PathVariable Integer tripId,
            @PathVariable Integer expenseId,
            @RequestBody List<ExpenseSplitReq> reqs
    ) {
        return process(() -> financeService.addExpenseSplits(tripId, expenseId, reqs));
    }

    @GetMapping("/expenses/{expenseId}/splits")
    public ResponseEntity<?> getExpenseSplits(
            @PathVariable Integer tripId,
            @PathVariable Integer expenseId
    ) {
        return process(() -> financeService.getExpenseSplits(tripId, expenseId));
    }

    @GetMapping("/finance/balance")
    public ResponseEntity<?> getTripBalance(@PathVariable Integer tripId) {
        return process(() -> financeService.getTripBalance(tripId));
    }

    @GetMapping("/expenses/analytics")
    public ResponseEntity<?> getCategoryAnalytics(
            @PathVariable Integer tripId,
            @RequestParam(name = "groupBy", required = false, defaultValue = "category") String groupBy
    ) {
        if (!"category".equalsIgnoreCase(groupBy)) {
            return ResponseEntity.badRequest().body("Chi ho tro groupBy=category");
        }
        return process(() -> financeService.getCategoryAnalytics(tripId));
    }

    private ResponseEntity<?> process(Supplier<Object> action) {
        try {
            return ResponseEntity.ok(action.get());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Loi he thong");
        }
    }
}

