package com.mobilebackend.ungdunglapkehoachdulich.service;

import com.mobilebackend.ungdunglapkehoachdulich.dto.finance.*;
import com.mobilebackend.ungdunglapkehoachdulich.model.Budget;
import com.mobilebackend.ungdunglapkehoachdulich.model.Expense;
import com.mobilebackend.ungdunglapkehoachdulich.model.ExpenseSplit;
import com.mobilebackend.ungdunglapkehoachdulich.repo.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class FinanceService {
    private final TripRepo tripRepo;
    private final BudgetRepo budgetRepo;
    private final ExpenseRepo expenseRepo;
    private final ExpenseSplitRepo expenseSplitRepo;

    @Transactional
    public Budget upsertBudget(Integer tripId, BudgetUpsertReq req) {
        validateTrip(tripId);
        if (req == null || isBlank(req.getCategory()) || req.getLimitAmount() == null || req.getLimitAmount() <= 0F) {
            throw new IllegalArgumentException("Thong tin budget khong hop le");
        }

        Budget budget = budgetRepo.findByTripIdAndCategory(tripId, req.getCategory().trim())
                .orElseGet(() -> Budget.builder().tripId(tripId).category(req.getCategory().trim()).build());

        budget.setLimitAmount(req.getLimitAmount());
        return budgetRepo.save(budget);
    }

    public List<Budget> getTripBudgets(Integer tripId) {
        validateTrip(tripId);
        return budgetRepo.findByTripId(tripId);
    }

    @Transactional
    public Expense createExpense(Integer tripId, ExpenseCreateReq req) {
        validateTrip(tripId);
        if (req == null || req.getUserId() == null || req.getAmount() == null || req.getAmount() <= 0F || isBlank(req.getCategory())) {
            throw new IllegalArgumentException("Thong tin chi tieu khong hop le");
        }

        Expense expense = Expense.builder()
                .tripId(tripId)
                .userId(req.getUserId())
                .amount(req.getAmount())
                .category(req.getCategory().trim())
                .description(req.getDescription())
                .build();

        return expenseRepo.save(expense);
    }

    public List<Expense> getTripExpenses(Integer tripId, String fromDate, String toDate) {
        validateTrip(tripId);
        List<Expense> expenses = expenseRepo.findByTripId(tripId);
        LocalDate from = parseDateOptional(fromDate);
        LocalDate to = parseDateOptional(toDate);
        if (from == null && to == null) {
            return expenses;
        }
        return expenses.stream()
                .filter(exp -> exp.getCreatedAt() != null)
                .filter(exp -> {
                    LocalDate d = exp.getCreatedAt().toLocalDate();
                    boolean afterFrom = from == null || !d.isBefore(from);
                    boolean beforeTo = to == null || !d.isAfter(to);
                    return afterFrom && beforeTo;
                })
                .toList();
    }

    @Transactional
    public Expense updateExpense(Integer tripId, Integer expenseId, ExpenseUpdateReq req) {
        validateTrip(tripId);
        Expense expense = expenseRepo.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Expense khong ton tai"));
        if (!Objects.equals(expense.getTripId(), tripId)) {
            throw new IllegalArgumentException("Expense khong thuoc trip nay");
        }
        if (req == null || req.getAmount() == null || req.getAmount() <= 0F || isBlank(req.getCategory())) {
            throw new IllegalArgumentException("Thong tin cap nhat chi tieu khong hop le");
        }

        expense.setAmount(req.getAmount());
        expense.setCategory(req.getCategory().trim());
        expense.setDescription(req.getDescription());
        return expenseRepo.save(expense);
    }

    @Transactional
    public String deleteExpense(Integer tripId, Integer expenseId) {
        validateTrip(tripId);
        Expense expense = expenseRepo.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Expense khong ton tai"));
        if (!Objects.equals(expense.getTripId(), tripId)) {
            throw new IllegalArgumentException("Expense khong thuoc trip nay");
        }
        expenseSplitRepo.deleteAll(expenseSplitRepo.findByExpenseId(expenseId));
        expenseRepo.delete(expense);
        return "Da xoa chi tieu";
    }

    @Transactional
    public List<ExpenseSplit> addExpenseSplits(Integer tripId, Integer expenseId, List<ExpenseSplitReq> reqs) {
        validateTrip(tripId);
        Expense expense = expenseRepo.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Expense khong ton tai"));
        if (!Objects.equals(expense.getTripId(), tripId)) {
            throw new IllegalArgumentException("Expense khong thuoc trip nay");
        }
        if (reqs == null || reqs.isEmpty()) {
            throw new IllegalArgumentException("Danh sach split khong duoc rong");
        }

        float totalSplit = 0F;
        List<ExpenseSplit> splitList = new ArrayList<>();
        for (ExpenseSplitReq req : reqs) {
            if (req.getUserId() == null || req.getOwedAmount() == null || req.getOwedAmount() <= 0F) {
                throw new IllegalArgumentException("Thong tin split khong hop le");
            }
            totalSplit += req.getOwedAmount();

            ExpenseSplit split = ExpenseSplit.builder()
                    .expenseId(expenseId)
                    .userId(req.getUserId())
                    .owedAmount(req.getOwedAmount())
                    .isSettled(req.getIsSettled() == null ? 0 : req.getIsSettled())
                    .build();
            splitList.add(split);
        }

        if (Math.abs(totalSplit - expense.getAmount()) > 0.01F) {
            throw new IllegalArgumentException("Tong split phai bang so tien chi tieu");
        }

        return expenseSplitRepo.saveAll(splitList);
    }

    public List<ExpenseSplit> getExpenseSplits(Integer tripId, Integer expenseId) {
        validateTrip(tripId);
        Expense expense = expenseRepo.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Expense khong ton tai"));
        if (!Objects.equals(expense.getTripId(), tripId)) {
            throw new IllegalArgumentException("Expense khong thuoc trip nay");
        }
        return expenseSplitRepo.findByExpenseId(expenseId);
    }

    public TripBalanceRes getTripBalance(Integer tripId) {
        validateTrip(tripId);
        float budgetTotal = sumBudget(budgetRepo.findByTripId(tripId));
        float expenseTotal = sumExpense(expenseRepo.findByTripId(tripId));
        return new TripBalanceRes(budgetTotal, expenseTotal, budgetTotal - expenseTotal);
    }

    public List<CategoryAnalyticsRes> getCategoryAnalytics(Integer tripId) {
        validateTrip(tripId);
        List<Expense> expenses = expenseRepo.findByTripId(tripId);
        float totalExpense = sumExpense(expenses);

        Map<String, Float> sumByCategory = new HashMap<>();
        Map<String, Integer> countByCategory = new HashMap<>();

        for (Expense expense : expenses) {
            String category = isBlank(expense.getCategory()) ? "UNCATEGORIZED" : expense.getCategory();
            sumByCategory.put(category, sumByCategory.getOrDefault(category, 0F) + expense.getAmount());
            countByCategory.put(category, countByCategory.getOrDefault(category, 0) + 1);
        }

        List<CategoryAnalyticsRes> analytics = new ArrayList<>();
        for (Map.Entry<String, Float> entry : sumByCategory.entrySet()) {
            float percentage = totalExpense == 0F ? 0F : (entry.getValue() * 100F / totalExpense);
            analytics.add(new CategoryAnalyticsRes(
                    entry.getKey(),
                    entry.getValue(),
                    percentage,
                    countByCategory.getOrDefault(entry.getKey(), 0)
            ));
        }

        analytics.sort((a, b) -> Float.compare(b.getSpent(), a.getSpent()));
        return analytics;
    }

    private float sumBudget(List<Budget> budgets) {
        float total = 0F;
        for (Budget budget : budgets) {
            if (budget.getLimitAmount() != null) {
                total += budget.getLimitAmount();
            }
        }
        return total;
    }

    private float sumExpense(List<Expense> expenses) {
        float total = 0F;
        for (Expense expense : expenses) {
            if (expense.getAmount() != null) {
                total += expense.getAmount();
            }
        }
        return total;
    }

    private void validateTrip(Integer tripId) {
        if (tripId == null || !tripRepo.existsById(tripId)) {
            throw new IllegalArgumentException("Trip khong ton tai");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private LocalDate parseDateOptional(String raw) {
        if (isBlank(raw)) return null;
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Ngay loc khong hop le, dung dinh dang YYYY-MM-DD");
        }
    }
}

