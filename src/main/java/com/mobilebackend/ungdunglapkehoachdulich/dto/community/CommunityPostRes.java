package com.mobilebackend.ungdunglapkehoachdulich.dto.community;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CommunityPostRes {
    private Integer id;
    private Integer userId;
    private Integer tripId;
    private Integer isTripLinked;
    private String title;
    private String content;
    private String imageUrl;
    private String location;
    private Integer budget;
    private LocalDateTime createdAt;
    private long likeCount;
    private long saveCount;
    private Integer isLiked;
    private Integer isSaved;
}
