package com.mobilebackend.ungdunglapkehoachdulich.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

@Data
@Builder
public class JournalDetailRes {
    private Integer postItineraryDetailId; // id của PostItineraryDetail
    private Integer postId;
    private String  postTitle;
    private String  postLocation;
    private LocalTime visitTime;           // giờ thăm địa điểm
    private String  note;                  // ghi chú cá nhân
    private String  status;               // "0"=chưa đi, "1"=đã đi
}
