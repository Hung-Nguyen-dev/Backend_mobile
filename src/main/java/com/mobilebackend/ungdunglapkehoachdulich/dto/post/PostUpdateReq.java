package com.mobilebackend.ungdunglapkehoachdulich.dto.post;

import lombok.Data;

@Data
public class PostUpdateReq {
    private String title;
    private String content;
    private String location;
    private Integer budget;
}