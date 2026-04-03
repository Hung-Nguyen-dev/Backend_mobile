package com.mobilebackend.ungdunglapkehoachdulich.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class JournalDayRes {
    private Integer itineraryId;
    private Integer dayNumber;
    private LocalDate date;
    private List<JournalDetailRes> details; // danh sách địa điểm trong ngày
}
