package com.mobilebackend.ungdunglapkehoachdulich.dto.finance;

import lombok.Data;

@Data
public class BudgetUpsertReq {
    private String category;
    private Float limitAmount;
}

