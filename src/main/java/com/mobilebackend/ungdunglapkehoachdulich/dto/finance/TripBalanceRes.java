package com.mobilebackend.ungdunglapkehoachdulich.dto.finance;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TripBalanceRes {
    private Float budgetTotal;
    private Float expenseTotal;
    private Float remainingBalance;
}

