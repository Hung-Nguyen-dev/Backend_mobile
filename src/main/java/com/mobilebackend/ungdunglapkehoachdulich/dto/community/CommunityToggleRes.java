package com.mobilebackend.ungdunglapkehoachdulich.dto.community;

import lombok.Builder;
import lombok.Data;

/**
 * Response payload for like/save/follow toggle operations.
 */
@Data
@Builder
public class CommunityToggleRes {
    private Integer userId;
    private Integer targetId;
    private String action;
    private Integer active;
    private long totalCount;
}
