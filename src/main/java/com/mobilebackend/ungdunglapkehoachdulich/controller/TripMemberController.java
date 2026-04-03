package com.mobilebackend.ungdunglapkehoachdulich.controller;

import com.mobilebackend.ungdunglapkehoachdulich.dto.InviteCompanionReq;
import com.mobilebackend.ungdunglapkehoachdulich.dto.InviteCompanionRes;
import com.mobilebackend.ungdunglapkehoachdulich.service.TripMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trips")
@RequiredArgsConstructor
public class TripMemberController {

    private final TripMemberService tripMemberService;

    /**
     * POST /api/v1/trips/{tripId}/invite
     * Chủ chuyến đi gửi lời mời tới người dùng khác.
     * Header: X-User-Id = id của người gửi lời mời (chủ trip)
     */
    @PostMapping("/{tripId}/invite")
    public ResponseEntity<InviteCompanionRes> inviteCompanion(
            @RequestHeader("X-User-Id") Integer inviterId,
            @PathVariable Integer tripId,
            @RequestBody InviteCompanionReq req) {

        req.setTripId(tripId);
        InviteCompanionRes response = tripMemberService.inviteCompanion(inviterId, req);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/v1/trips/members/{memberId}/accept
     * Người được mời chấp nhận lời mời tham gia chuyến đi.
     * Header: X-User-Id = id của người được mời
     */
    @PutMapping("/members/{memberId}/accept")
    public ResponseEntity<InviteCompanionRes> acceptInvitation(
            @RequestHeader("X-User-Id") Integer currentUserId,
            @PathVariable Integer memberId) {

        InviteCompanionRes response = tripMemberService.acceptInvitation(currentUserId, memberId);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/v1/trips/members/{memberId}/decline
     * Người được mời từ chối lời mời.
     * Header: X-User-Id = id của người được mời
     */
    @DeleteMapping("/members/{memberId}/decline")
    public ResponseEntity<Void> declineInvitation(
            @RequestHeader("X-User-Id") Integer currentUserId,
            @PathVariable Integer memberId) {

        tripMemberService.declineInvitation(currentUserId, memberId);
        return ResponseEntity.noContent().build();
    }

    /**
     * GET /api/v1/trips/{tripId}/members
     * Xem danh sách thành viên đã tham gia chuyến đi.
     * Header: X-User-Id = id người yêu cầu (phải là chủ hoặc thành viên)
     */
    @GetMapping("/{tripId}/members")
    public ResponseEntity<List<InviteCompanionRes>> getMembers(
            @RequestHeader("X-User-Id") Integer currentUserId,
            @PathVariable Integer tripId) {

        List<InviteCompanionRes> members = tripMemberService.getMembers(currentUserId, tripId);
        return ResponseEntity.ok(members);
    }

    /**
     * GET /api/v1/trips/invitations/pending
     * Xem danh sách lời mời đang chờ xác nhận của user hiện tại.
     * Header: X-User-Id = id người dùng hiện tại
     */
    @GetMapping("/invitations/pending")
    public ResponseEntity<List<InviteCompanionRes>> getPendingInvitations(
            @RequestHeader("X-User-Id") Integer currentUserId) {

        List<InviteCompanionRes> pending = tripMemberService.getPendingInvitations(currentUserId);
        return ResponseEntity.ok(pending);
    }
}
