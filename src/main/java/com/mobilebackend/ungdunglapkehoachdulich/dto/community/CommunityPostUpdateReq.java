package com.mobilebackend.ungdunglapkehoachdulich.dto.community;

import lombok.Data;

@Data
public class CommunityPostUpdateReq {
    private String title;
    private String content;
    private String imageUrl;
    private String location;
    private Integer budget;
}
