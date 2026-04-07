package com.mobilebackend.ungdunglapkehoachdulich.dto.finance;

import lombok.Data;

@Data
public class ExpenseSplitReq {
    private Integer userId;
    private Float owedAmount;
    private Integer isSettled;
}

