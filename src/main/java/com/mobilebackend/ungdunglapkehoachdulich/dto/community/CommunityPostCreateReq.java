package com.mobilebackend.ungdunglapkehoachdulich.dto.community;

import lombok.Data;

@Data
public class CommunityPostCreateReq {
    private Integer tripId;
    private String title;
    private String content;
    private String imageUrl;
    private String location;
    private Integer budget;
}
