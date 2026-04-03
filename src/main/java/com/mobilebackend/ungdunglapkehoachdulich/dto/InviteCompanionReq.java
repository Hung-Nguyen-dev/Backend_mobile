package com.mobilebackend.ungdunglapkehoachdulich.dto;

import lombok.Data;

@Data
public class InviteCompanionReq {
    private Integer tripId;
    private String inviteeEmail; // Email của người được mời
    private Integer memberRole;  // 1: đồng hành, 2: trợ lý, ...
}
