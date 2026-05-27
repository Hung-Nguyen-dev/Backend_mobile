package com.mobilebackend.ungdunglapkehoachdulich.dto.community;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response payload for community post with interaction metadata.
 */
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
    private List<String> imageUrls;
    private String location;
    private Integer budget;
    private LocalDateTime createdAt;
    private String authorUsername;
    private String authorFullName;
    private String authorAvatarUrl;
    private long likeCount;
    private long saveCount;
    private Integer isLiked;
    private Integer isSaved;
}
