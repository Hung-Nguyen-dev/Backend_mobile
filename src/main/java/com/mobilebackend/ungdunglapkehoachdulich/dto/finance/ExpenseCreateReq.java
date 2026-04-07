package com.mobilebackend.ungdunglapkehoachdulich.dto.finance;

import lombok.Data;

@Data
public class ExpenseCreateReq {
    private Integer userId;
    private Float amount;
    private String category;
    private String description;
}

