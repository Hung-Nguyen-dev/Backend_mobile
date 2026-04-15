package com.mobilebackend.ungdunglapkehoachdulich.dto.finance;

import lombok.Data;

@Data
public class ExpenseUpdateReq {
    private Float amount;
    private String category;
    private String description;
}
