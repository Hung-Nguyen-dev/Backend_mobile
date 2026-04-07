package com.mobilebackend.ungdunglapkehoachdulich.dto.finance;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CategoryAnalyticsRes {
    private String category;
    private Float spent;
    private Float percentage;
    private Integer transactionCount;
}

