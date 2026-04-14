package com.mobilebackend.ungdunglapkehoachdulich.dto;

import com.mobilebackend.ungdunglapkehoachdulich.model.Post;
import com.mobilebackend.ungdunglapkehoachdulich.model.Trip;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JournalRes {
    private Trip trip;
    private List<DayJournal> days;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayJournal {
        private Integer itineraryId;
        private Integer dayNumber;
        private String date;
        private List<StopJournal> stops;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StopJournal {
        private Integer itineraryDetailId;
        private String visitTime;
        private String note;
        private Integer postItineraryDetailId;
        private String status;
        private Post post;
    }
}
