package com.mobilebackend.ungdunglapkehoachdulich.controller;

import com.mobilebackend.ungdunglapkehoachdulich.dto.JournalDayRes;
import com.mobilebackend.ungdunglapkehoachdulich.dto.JournalDetailRes;
import com.mobilebackend.ungdunglapkehoachdulich.dto.UpdateJournalReq;
import com.mobilebackend.ungdunglapkehoachdulich.service.JournalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class JournalController {

    private final JournalService journalService;

    /**
     * GET /api/v1/trips/{tripId}/journal
     * Xem toàn bộ nhật ký hành trình (tất cả các ngày).
     * Header: X-User-Id = id người dùng hiện tại
     */
    @GetMapping("/{tripId}/journal")
    public ResponseEntity<List<JournalDayRes>> getJournal(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable Integer tripId) {

        List<JournalDayRes> journal = journalService.getJournal(userId, tripId);
        return ResponseEntity.ok(journal);
    }

    /**
     * GET /api/v1/trips/{tripId}/journal/day/{dayNumber}
     * Xem nhật ký của một ngày cụ thể trong chuyến đi.
     * Header: X-User-Id = id người dùng hiện tại
     */
    @GetMapping("/{tripId}/journal/day/{dayNumber}")
    public ResponseEntity<JournalDayRes> getDayJournal(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable Integer tripId,
            @PathVariable Integer dayNumber) {

        JournalDayRes day = journalService.getDayJournal(userId, tripId, dayNumber);
        return ResponseEntity.ok(day);
    }

    /**
     * PUT /api/v1/trips/journal/mark
     * Đánh dấu địa điểm đã đi ("1") hoặc chưa đi ("0").
     * Header: X-User-Id = id người dùng hiện tại
     * Body: { "postItineraryDetailId": 5, "status": "1" }
     */
    @PutMapping("/journal/mark")
    public ResponseEntity<JournalDetailRes> markVisited(
            @RequestHeader("X-User-Id") Integer userId,
            @RequestBody UpdateJournalReq req) {

        JournalDetailRes result = journalService.markVisited(userId, req);
        return ResponseEntity.ok(result);
    }

    /**
     * PUT /api/v1/trips/journal/details/{detailId}/note
     * Cập nhật ghi chú cá nhân cho một hoạt động trong ngày.
     * Header: X-User-Id = id người dùng hiện tại
     * Body: "Quán ăn ngon, nên đặt trước"
     */
    @PutMapping("/journal/details/{detailId}/note")
    public ResponseEntity<JournalDayRes> updateNote(
            @RequestHeader("X-User-Id") Integer userId,
            @PathVariable Integer detailId,
            @RequestBody String note) {

        JournalDayRes updatedDay = journalService.updateNote(userId, detailId, note);
        return ResponseEntity.ok(updatedDay);
    }
}
