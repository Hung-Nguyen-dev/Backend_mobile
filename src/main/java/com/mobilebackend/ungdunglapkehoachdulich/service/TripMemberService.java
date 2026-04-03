package com.mobilebackend.ungdunglapkehoachdulich.service;

import com.mobilebackend.ungdunglapkehoachdulich.dto.InviteCompanionReq;
import com.mobilebackend.ungdunglapkehoachdulich.dto.InviteCompanionRes;
import com.mobilebackend.ungdunglapkehoachdulich.model.Trip;
import com.mobilebackend.ungdunglapkehoachdulich.model.TripMember;
import com.mobilebackend.ungdunglapkehoachdulich.model.User;
import com.mobilebackend.ungdunglapkehoachdulich.repo.TripMemberRepo;
import com.mobilebackend.ungdunglapkehoachdulich.repo.TripRepo;
import com.mobilebackend.ungdunglapkehoachdulich.repo.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripMemberService {

    private final TripMemberRepo tripMemberRepo;
    private final TripRepo tripRepo;
    private final UserRepo userRepo;

    // ----------------------------------------------------------------
    // 1. MỜI BẠN ĐỒNG HÀNH
    // ----------------------------------------------------------------

    /**
     * Chủ chuyến đi mời người dùng khác tham gia.
     * Business Rules:
     *  - Trip phải tồn tại và người gửi phải là chủ trip.
     *  - Người được mời phải tồn tại trong hệ thống.
     *  - Không được tự mời bản thân.
     *  - Không mời người đã là thành viên hoặc đang chờ xác nhận.
     */
    @Transactional
    public InviteCompanionRes inviteCompanion(Integer inviterId, InviteCompanionReq req) {

        // --- Validate input ---
        if (inviterId == null) {
            throw new IllegalArgumentException("inviterId không được để trống");
        }
        if (req == null || req.getTripId() == null || req.getInviteeEmail() == null) {
            throw new IllegalArgumentException("Thông tin mời không hợp lệ");
        }

        // --- Kiểm tra trip tồn tại ---
        Trip trip = tripRepo.findById(req.getTripId())
                .orElseThrow(() -> new IllegalArgumentException("Chuyến đi không tồn tại: " + req.getTripId()));

        // --- Kiểm tra quyền: chỉ chủ trip mới được mời ---
        if (!trip.getUserId().equals(inviterId)) {
            throw new SecurityException("Bạn không có quyền mời thành viên cho chuyến đi này");
        }

        // --- Kiểm tra người được mời tồn tại ---
        User invitee = userRepo.findByEmail(req.getInviteeEmail())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng với email: " + req.getInviteeEmail()));

        // --- Không được tự mời bản thân ---
        if (invitee.getId().equals(inviterId)) {
            throw new IllegalArgumentException("Bạn không thể mời chính mình tham gia chuyến đi");
        }

        // --- Kiểm tra trùng lặp (đã là thành viên hoặc đang chờ) ---
        Optional<TripMember> existingMember = tripMemberRepo.findByTripIdAndUserId(req.getTripId(), invitee.getId());
        if (existingMember.isPresent()) {
            Integer currentStatus = existingMember.get().getStatus();
            if (currentStatus == 1) {
                throw new IllegalStateException("Người dùng đã là thành viên của chuyến đi này");
            } else {
                throw new IllegalStateException("Đã có lời mời đang chờ xác nhận cho người dùng này");
            }
        }

        // --- Xác định memberRole mặc định nếu không truyền ---
        Integer memberRole = (req.getMemberRole() != null) ? req.getMemberRole() : 1;

        // --- Lưu TripMember với status = 0 (chờ xác nhận) ---
        TripMember newMember = TripMember.builder()
                .tripId(req.getTripId())
                .userId(invitee.getId())
                .memberRole(memberRole)
                .status(0) // 0: chờ xác nhận
                .build();
        TripMember saved = tripMemberRepo.save(newMember);

        log.info("Đã gửi lời mời thành công: tripId={}, inviteeId={}", req.getTripId(), invitee.getId());

        return buildResponse(saved, trip, invitee);
    }

    // ----------------------------------------------------------------
    // 2. CHẤP NHẬN LỜI MỜI
    // ----------------------------------------------------------------

    /**
     * Người được mời chấp nhận lời mời tham gia chuyến đi.
     * Business Rules:
     *  - TripMember phải tồn tại và đang ở trạng thái chờ (status=0).
     *  - Chỉ người được mời (userId trùng) mới có thể chấp nhận.
     */
    @Transactional
    public InviteCompanionRes acceptInvitation(Integer currentUserId, Integer memberId) {

        // --- Lấy TripMember ---
        TripMember tripMember = tripMemberRepo.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Lời mời không tồn tại: " + memberId));

        // --- Kiểm tra quyền: chỉ người được mời mới được chấp nhận ---
        if (!tripMember.getUserId().equals(currentUserId)) {
            throw new SecurityException("Bạn không có quyền chấp nhận lời mời này");
        }

        // --- Kiểm tra trạng thái: phải đang chờ ---
        if (tripMember.getStatus() != 0) {
            throw new IllegalStateException("Lời mời này đã được xử lý trước đó");
        }

        // --- Cập nhật status = 1 (tham gia) ---
        tripMember.setStatus(1);
        TripMember updated = tripMemberRepo.save(tripMember);

        Trip trip = tripRepo.findById(tripMember.getTripId())
                .orElseThrow(() -> new IllegalArgumentException("Chuyến đi không tồn tại"));
        User user = userRepo.findById(tripMember.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Người dùng không tồn tại"));

        log.info("Đã chấp nhận lời mời: memberId={}, userId={}, tripId={}", memberId, currentUserId, tripMember.getTripId());

        return buildResponse(updated, trip, user);
    }

    // ----------------------------------------------------------------
    // 3. TỪ CHỐI LỜI MỜI
    // ----------------------------------------------------------------

    /**
     * Người được mời từ chối lời mời tham gia chuyến đi.
     * Business Rules:
     *  - TripMember phải tồn tại và đang ở trạng thái chờ (status=0).
     *  - Chỉ người được mời (userId trùng) mới có thể từ chối.
     *  - Bản ghi sẽ bị xóa khỏi hệ thống.
     */
    @Transactional
    public void declineInvitation(Integer currentUserId, Integer memberId) {

        // --- Lấy TripMember ---
        TripMember tripMember = tripMemberRepo.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Lời mời không tồn tại: " + memberId));

        // --- Kiểm tra quyền ---
        if (!tripMember.getUserId().equals(currentUserId)) {
            throw new SecurityException("Bạn không có quyền từ chối lời mời này");
        }

        // --- Chỉ từ chối được lời mời đang chờ ---
        if (tripMember.getStatus() != 0) {
            throw new IllegalStateException("Lời mời này đã được xử lý trước đó");
        }

        // --- Xóa bản ghi ---
        tripMemberRepo.delete(tripMember);

        log.info("Đã từ chối lời mời: memberId={}, userId={}", memberId, currentUserId);
    }

    // ----------------------------------------------------------------
    // 4. XEM DANH SÁCH THÀNH VIÊN CHUYẾN ĐI
    // ----------------------------------------------------------------

    /**
     * Lấy danh sách thành viên của một chuyến đi (chỉ những người đã tham gia, status=1).
     * Business Rules:
     *  - Trip phải tồn tại.
     *  - Người yêu cầu phải là thành viên hoặc chủ của chuyến đi.
     */
    public List<InviteCompanionRes> getMembers(Integer currentUserId, Integer tripId) {

        // --- Kiểm tra trip tồn tại ---
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Chuyến đi không tồn tại: " + tripId));

        // --- Kiểm tra quyền: chủ trip hoặc thành viên ---
        boolean isOwner = trip.getUserId().equals(currentUserId);
        boolean isMember = tripMemberRepo.findByTripIdAndUserId(tripId, currentUserId)
                .map(m -> m.getStatus() == 1)
                .orElse(false);

        if (!isOwner && !isMember) {
            throw new SecurityException("Bạn không có quyền xem danh sách thành viên của chuyến đi này");
        }

        // --- Lấy danh sách thành viên đã tham gia ---
        List<TripMember> members = tripMemberRepo.findByTripId(tripId)
                .stream()
                .filter(m -> m.getStatus() == 1)
                .collect(Collectors.toList());

        return members.stream()
                .map(member -> {
                    User user = userRepo.findById(member.getUserId()).orElse(null);
                    return buildResponse(member, trip, user);
                })
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    // 5. DANH SÁCH LỜI MỜI ĐANG CHỜ CỦA USER
    // ----------------------------------------------------------------

    /**
     * Lấy danh sách lời mời đang chờ xác nhận của user hiện tại (status=0).
     */
    public List<InviteCompanionRes> getPendingInvitations(Integer currentUserId) {

        if (currentUserId == null) {
            throw new IllegalArgumentException("userId không được để trống");
        }

        List<TripMember> pending = tripMemberRepo.findByUserIdAndStatus(currentUserId, 0);

        return pending.stream()
                .map(member -> {
                    Trip trip = tripRepo.findById(member.getTripId()).orElse(null);
                    User user = userRepo.findById(member.getUserId()).orElse(null);
                    return buildResponse(member, trip, user);
                })
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    // HELPER: Tạo response DTO
    // ----------------------------------------------------------------

    private InviteCompanionRes buildResponse(TripMember member, Trip trip, User user) {
        return InviteCompanionRes.builder()
                .memberId(member.getId())
                .tripId(member.getTripId())
                .tripName(trip != null ? trip.getTripName() : null)
                .userId(member.getUserId())
                .inviteeName(user != null ? user.getFullName() : null)
                .inviteeEmail(user != null ? user.getEmail() : null)
                .memberRole(member.getMemberRole())
                .status(member.getStatus())
                .build();
    }
}
