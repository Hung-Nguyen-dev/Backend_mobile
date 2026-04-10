package com.mobilebackend.ungdunglapkehoachdulich.dto.post;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InteractionStateRes {
    private Integer userId;
    private Integer postId;
    private String actionType;
    private boolean active;
    private long totalCount;
}