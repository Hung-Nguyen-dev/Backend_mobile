package com.mobilebackend.ungdunglapkehoachdulich.dto.post;

import lombok.Data;

@Data
public class PostCreateReq {
    private String title;
    private String content;
    private String location;
    private Integer budget;
}