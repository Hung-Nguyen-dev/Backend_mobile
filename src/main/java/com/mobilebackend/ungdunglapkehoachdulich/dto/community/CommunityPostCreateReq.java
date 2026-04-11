package com.mobilebackend.ungdunglapkehoachdulich.dto.community;

import lombok.Data;

import java.util.List;

@Data
public class CommunityPostCreateReq {
    private Integer tripId;
    private String title;
    private String content;
    private String imageUrl;
    private List<String> imageUrls;
    private String location;
    private Integer budget;
}
