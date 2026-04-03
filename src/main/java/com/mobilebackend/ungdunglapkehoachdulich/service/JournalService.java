package com.mobilebackend.ungdunglapkehoachdulich.service;

import com.mobilebackend.ungdunglapkehoachdulich.dto.JournalDayRes;
import com.mobilebackend.ungdunglapkehoachdulich.dto.JournalDetailRes;
import com.mobilebackend.ungdunglapkehoachdulich.dto.UpdateJournalReq;
import com.mobilebackend.ungdunglapkehoachdulich.model.*;
import com.mobilebackend.ungdunglapkehoachdulich.repo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JournalService {

    private final TripRepo                tripRepo;
    private final TripMemberRepo          tripMemberRepo;
    private final ItineraryRepo           itineraryRepo;
    private final ItineraryDetailRepo     itineraryDetailRepo;
    private final PostItineraryDetailRepo postItineraryDetailRepo;
    private final PostRepo                postRepo;

    // ----------------------------------------------------------------
    // HELPER: kiểm tra user có quyền truy cập trip không
    // ----------------------------------------------------------------
    private void validateTripAccess(Integer userId, Integer tripId) {
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Chuyến đi không tồn tại: " + tripId));

        boolean isOwner = trip.getUserId().equals(userId);
        boolean isMember = tripMemberRepo.findByTripIdAndUserId(tripId, userId)
                .map(m -> m.getStatus() == 1)
                .orElse(false);

        if (!isOwner && !isMember) {
            throw new SecurityException("Bạn không có quyền xem nhật ký của chuyến đi này");
        }
    }

    // ----------------------------------------------------------------
    // HELPER: xây dựng JournalDetailRes từ ItineraryDetail
    // ----------------------------------------------------------------
    private List<JournalDetailRes> buildDetails(Integer itineraryDetailId, ItineraryDetail detail) {
        List<PostItineraryDetail> pids = postItineraryDetailRepo.findByItineraryDetailId(itineraryDetailId);

        return pids.stream().map(pid -> {
            Post post = postRepo.findById(pid.getPostId()).orElse(null);
            return JournalDetailRes.builder()
                    .postItineraryDetailId(pid.getId())
                    .postId(pid.getPostId())
                    .postTitle(post != null ? post.getTitle() : null)
                    .postLocation(post != null ? post.getLocation() : null)
                    .visitTime(detail.getVisitTime())
                    .note(detail.getNote())
                    .status(pid.getStatus())
                    .build();
        }).collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    // HELPER: xây dựng JournalDayRes từ một Itinerary
    // ----------------------------------------------------------------
    private JournalDayRes buildDay(Itinerary itinerary) {
        List<ItineraryDetail> detailsList =
                itineraryDetailRepo.findByItineraryIdOrderByVisitTimeAsc(itinerary.getId());

        List<JournalDetailRes> allDetails = detailsList.stream()
                .flatMap(d -> buildDetails(d.getId(), d).stream())
                .collect(Collectors.toList());

        return JournalDayRes.builder()
                .itineraryId(itinerary.getId())
                .dayNumber(itinerary.getDayNumber())
                .date(itinerary.getDate())
                .details(allDetails)
                .build();
    }

    // ----------------------------------------------------------------
    // 1. XEM TOÀN BỘ NHẬT KÝ HÀNH TRÌNH
    // ----------------------------------------------------------------

    /**
     * Trả về nhật ký đầy đủ của một chuyến đi (tất cả ngày, tất cả địa điểm).
     * Business Rules:
     *  - Chỉ chủ trip hoặc thành viên đã tham gia (status=1) mới xem được.
     */
    public List<JournalDayRes> getJournal(Integer userId, Integer tripId) {
        validateTripAccess(userId, tripId);

        List<Itinerary> itineraries = itineraryRepo.findByTripIdOrderByDayNumberAsc(tripId);
        if (itineraries.isEmpty()) {
            log.warn("Chuyến đi {} chưa có lịch trình", tripId);
        }

        return itineraries.stream()
                .map(this::buildDay)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    // 2. XEM NHẬT KÝ MỘT NGÀY CỤ THỂ
    // ----------------------------------------------------------------

    /**
     * Trả về nhật ký của một ngày trong chuyến đi.
     * Business Rules:
     *  - dayNumber phải hợp lệ (thuộc chuyến đi đó).
     *  - Chỉ chủ trip hoặc thành viên đã tham gia mới xem được.
     */
    public JournalDayRes getDayJournal(Integer userId, Integer tripId, Integer dayNumber) {
        validateTripAccess(userId, tripId);

        if (dayNumber == null || dayNumber < 1) {
            throw new IllegalArgumentException("Số ngày không hợp lệ");
        }

        List<Itinerary> itineraries = itineraryRepo.findByTripIdOrderByDayNumberAsc(tripId);
        Itinerary itinerary = itineraries.stream()
                .filter(i -> i.getDayNumber().equals(dayNumber))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Không tìm thấy ngày " + dayNumber + " trong chuyến đi " + tripId));

        return buildDay(itinerary);
    }

    // ----------------------------------------------------------------
    // 3. ĐÁNH DẤU ĐỊA ĐIỂM ĐÃ ĐI / CHƯA ĐI
    // ----------------------------------------------------------------

    /**
     * Cập nhật trạng thái "đã đi" / "chưa đi" của một địa điểm trong nhật ký.
     * Business Rules:
     *  - status chỉ được là "0" hoặc "1".
     *  - Chỉ chính user đó (postItineraryDetail.userId == currentUserId) mới được cập nhật.
     */
    @Transactional
    public JournalDetailRes markVisited(Integer userId, UpdateJournalReq req) {

        // Validate status
        if (req.getStatus() == null || (!req.getStatus().equals("0") && !req.getStatus().equals("1"))) {
            throw new IllegalArgumentException("Status chỉ nhận giá trị '0' (chưa đi) hoặc '1' (đã đi)");
        }

        // Lấy bản ghi và kiểm tra quyền sở hữu
        PostItineraryDetail pid = postItineraryDetailRepo
                .findByIdAndUserId(req.getPostItineraryDetailId(), userId)
                .orElseThrow(() -> new SecurityException(
                        "Không tìm thấy địa điểm hoặc bạn không có quyền cập nhật trạng thái này"));

        // Cập nhật status
        pid.setStatus(req.getStatus());
        PostItineraryDetail updated = postItineraryDetailRepo.save(pid);

        log.info("User {} đánh dấu địa điểm {} → status={}", userId, pid.getPostId(), req.getStatus());

        // Lấy thông tin ItineraryDetail để build response
        ItineraryDetail detail = itineraryDetailRepo.findById(updated.getItineraryDetailId())
                .orElse(new ItineraryDetail());
        Post post = postRepo.findById(updated.getPostId()).orElse(null);

        return JournalDetailRes.builder()
                .postItineraryDetailId(updated.getId())
                .postId(updated.getPostId())
                .postTitle(post != null ? post.getTitle() : null)
                .postLocation(post != null ? post.getLocation() : null)
                .visitTime(detail.getVisitTime())
                .note(detail.getNote())
                .status(updated.getStatus())
                .build();
    }

    // ----------------------------------------------------------------
    // 4. CẬP NHẬT GHI CHÚ CÁ NHÂN CHO HOẠT ĐỘNG TRONG NGÀY
    // ----------------------------------------------------------------

    /**
     * Cập nhật ghi chú (note) cho một ItineraryDetail.
     * Business Rules:
     *  - Chỉ chủ trip hoặc thành viên mới được cập nhật ghi chú.
     *  - note có thể null (xóa ghi chú).
     */
    @Transactional
    public JournalDayRes updateNote(Integer userId, Integer itineraryDetailId, String note) {

        ItineraryDetail detail = itineraryDetailRepo.findById(itineraryDetailId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Hoạt động không tồn tại: " + itineraryDetailId));

        // Xác định tripId từ itinerary để kiểm tra quyền
        Itinerary itinerary = itineraryRepo.findById(detail.getItineraryId())
                .orElseThrow(() -> new IllegalArgumentException("Ngày hành trình không tồn tại"));

        validateTripAccess(userId, itinerary.getTripId());

        // Cập nhật ghi chú
        detail.setNote(note);
        itineraryDetailRepo.save(detail);

        log.info("User {} cập nhật ghi chú cho hoạt động {}", userId, itineraryDetailId);

        // Trả về nhật ký ngày chứa hoạt động đó
        return buildDay(itinerary);
    }
}
