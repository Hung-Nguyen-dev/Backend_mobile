package com.mobilebackend.ungdunglapkehoachdulich.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InviteCompanionRes {
    private Integer memberId;
    private Integer tripId;
    private String tripName;
    private Integer userId;
    private String inviteeName;
    private String inviteeEmail;
    private Integer memberRole;
    private Integer status; // 0: chờ xác nhận, 1: đã tham gia
}
