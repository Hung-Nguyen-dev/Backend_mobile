package com.mobilebackend.ungdunglapkehoachdulich.dto;

import lombok.Data;

@Data
public class UpdateJournalReq {
    private Integer postItineraryDetailId; // id của PostItineraryDetail cần cập nhật
    private String  status;                // "0" = chưa đi, "1" = đã đi
    private String  note;                  // ghi chú mới (có thể null)
}
