package com.mobilebackend.ungdunglapkehoachdulich.dto.community;

import lombok.Data;

import java.util.List;

/**
 * Request payload for updating a community post.
 */
@Data
public class CommunityPostUpdateReq {
    private String title;
    private String content;
    private String imageUrl;
    private List<String> imageUrls;
    private String location;
    private Integer budget;
}
