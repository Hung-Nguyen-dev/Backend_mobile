package com.mobilebackend.ungdunglapkehoachdulich.dto.community;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CommunityToggleRes {
    private Integer userId;
    private Integer targetId;
    private String action;
    private Integer active;
    private long totalCount;
}
