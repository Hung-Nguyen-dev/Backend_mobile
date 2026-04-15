package com.mobilebackend.ungdunglapkehoachdulich.service;

import com.mobilebackend.ungdunglapkehoachdulich.dto.TripReq;
import com.mobilebackend.ungdunglapkehoachdulich.dto.UpdateTripStopReq;
import com.mobilebackend.ungdunglapkehoachdulich.model.*;
import com.mobilebackend.ungdunglapkehoachdulich.repo.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {
    private final TripRepo tripRepo;
    private final ItineraryRepo itineraryRepo;
    private final ItineraryDetailRepo itineraryDetailRepo;
    private final PostItineraryDetailRepo postItineraryDetailRepo;
    private final TripMemberRepo tripMemberRepo;
    private final PostRepo postRepo;
    private final ItineraryItemRepo itineraryItemRepo;
    private final BudgetRepo budgetRepo;
    private final ExpenseRepo expenseRepo;
    private final ExpenseSplitRepo expenseSplitRepo;
    private final BookingMasterRepo bookingMasterRepo;
    private final BookingFlightRepo bookingFlightRepo;
    private final BookingHotelRepo bookingHotelRepo;
    private final BookingCoachRepo bookingCoachRepo;
    private final BookingRestaurantRepo bookingRestaurantRepo;
    private final PaymentRepo paymentRepo;

    @Transactional
    public Integer TripCreateService(Integer userId, TripReq tripReq){
//        validate
        if(Objects.isNull(userId)){
           log.error("Thieu userid");
        }
        if(!Objects.isNull(tripReq)){
            if(Objects.isNull(tripReq.getTripName())){
                log.error("vui long nhap ten chuyen di");
            }
            if(Objects.isNull(tripReq.getStartDate()) || Objects.isNull(tripReq.getEndDate())){
                log.error("Thieu start data hoac end date");
            }
        }
//        busseness logic
//        save trip
        Trip trip = Trip.builder()
                .userId(userId)
                .status("1")
                .tripName(tripReq.getTripName())
                .destination(tripReq.getDestination())
                .startDate(tripReq.getStartDate())
                .endDate(tripReq.getEndDate())
                .build();
        tripRepo.save(trip);
        Integer tripId = trip.getId();

//        save itinerary
        long days = ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate()) + 1;
        List<Itinerary> itineraryList = new ArrayList<>();
        for (int i=1; i<=days; i++){
            Itinerary itinerary = new Itinerary();
            itinerary.setDayNumber(i);
            itinerary.setDate(trip.getStartDate().plusDays(i - 1));
            itinerary.setTripId(trip.getId());
            itineraryList.add(itineraryRepo.save(itinerary));
        }

//        save itineraryDetail and post
        if (tripReq.getActiveDays() != null && !tripReq.getActiveDays().isEmpty()) {
            for (TripReq.DailyPlan dailyPlan : tripReq.getActiveDays()) {
                // Find itinerary created above for this dayIndex (assuming dayIndex 0 = dayNumber 1)
                Itinerary matchingIti = itineraryList.stream()
                        .filter(iti -> iti.getDayNumber() == (dailyPlan.getDayIndex() + 1))
                        .findFirst()
                        .orElse(null);

                if (matchingIti != null && dailyPlan.getPlaces() != null) {
                    for (TripReq.PlaceDraft place : dailyPlan.getPlaces()) {
                        // 1. Save new post for this location (since it comes from UI mocked places)
                        Post post = Post.builder()
                                .userId(userId)
                                .title(place.getName())
                                .content(place.getDescription())
                                .location(trip.getDestination())
                                .latitude(place.getLatitude())
                                .longitude(place.getLongitude())
                                .imageUrl(place.getImageUrl())
                                .build();
                        post = postRepo.save(post);

                        java.time.LocalTime parsedTime = null;
                        if (place.getSuggestTime() != null && !place.getSuggestTime().trim().isEmpty()) {
                            try {
                                parsedTime = java.time.LocalTime.parse(place.getSuggestTime().trim());
                            } catch (Exception e) {
                                log.warn("Không parse được thời gian: " + place.getSuggestTime());
                            }
                        }

                        // 2. Save Itinerary Detail
                        ItineraryDetail itineraryDetail = ItineraryDetail.builder()
                                .visitTime(parsedTime)
                                .note(place.getDescription())
                                .itineraryId(matchingIti.getId())
                                .build();
                        itineraryDetail = itineraryDetailRepo.save(itineraryDetail);

                        // 3. Save Post Itinerary Detail mapping
                        PostItineraryDetail postItineraryDetail = PostItineraryDetail.builder()
                                .itineraryDetailId(itineraryDetail.getId())
                                .postId(post.getId())
                                .status("0") // chua di
                                .userId(userId)
                                .build();
                        postItineraryDetailRepo.save(postItineraryDetail);
                    }
                }
            }
        }

        return tripId;
    }

    @Transactional
    public void deleteTrip(Integer tripId, Integer userId) {
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip không tồn tại"));

        if (!trip.getUserId().equals(userId)) {
            throw new RuntimeException("Bạn không có quyền xóa chuyến đi này");
        }

        // clear itinerary_items (new place detail add-to-trip feature)
        itineraryItemRepo.deleteByTripId(tripId);

        // clear finance data
        List<Expense> expenses = expenseRepo.findByTripId(tripId);
        List<Integer> expenseIds = expenses.stream().map(Expense::getId).filter(Objects::nonNull).collect(Collectors.toList());
        if (!expenseIds.isEmpty()) {
            expenseSplitRepo.deleteByExpenseIdIn(expenseIds);
        }
        expenseRepo.deleteByTripId(tripId);
        budgetRepo.deleteByTripId(tripId);

        // clear bookings + payments by booking_master
        List<BookingMaster> bookingMasters = bookingMasterRepo.findByTripId(tripId);
        for (BookingMaster bookingMaster : bookingMasters) {
            Integer bookingMasterId = bookingMaster.getId();
            if (bookingMasterId == null) continue;
            paymentRepo.deleteByBookingMasterId(bookingMasterId);
            bookingFlightRepo.deleteByBookingMasterId(bookingMasterId);
            bookingHotelRepo.deleteByBookingMasterId(bookingMasterId);
            bookingCoachRepo.deleteByBookingMasterId(bookingMasterId);
            bookingRestaurantRepo.deleteByBookingMasterId(bookingMasterId);
        }
        bookingMasterRepo.deleteAll(bookingMasters);

        // cascade delete TripMembers
        tripMemberRepo.deleteAll(tripMemberRepo.findByTripId(tripId));

        // cascade delete Itineraries -> ItineraryDetails -> PostItineraryDetails
        List<Itinerary> itineraries = itineraryRepo.findByTripIdOrderByDayNumberAsc(tripId);
        for (Itinerary iti : itineraries) {
            List<ItineraryDetail> details = itineraryDetailRepo.findByItineraryIdOrderByVisitTimeAsc(iti.getId());
            for (ItineraryDetail det : details) {
                postItineraryDetailRepo.deleteAll(postItineraryDetailRepo.findByItineraryDetailId(det.getId()));
            }
            itineraryDetailRepo.deleteAll(details);
        }
        itineraryRepo.deleteAll(itineraries);

        // Finally delete the trip itself
        tripRepo.delete(trip);
    }

    public com.mobilebackend.ungdunglapkehoachdulich.dto.JournalRes getTripJournal(Integer tripId, Integer userId) {
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip không tồn tại"));

        // Only allow members or owner
        if (!trip.getUserId().equals(userId)) {
            tripMemberRepo.findByTripIdAndUserId(tripId, userId)
                .filter(tm -> tm.getStatus() == 1)
                .orElseThrow(() -> new RuntimeException("Bạn không có quyền truy cập nhật ký chuyến đi này"));
        }

        List<com.mobilebackend.ungdunglapkehoachdulich.dto.JournalRes.DayJournal> dayJournals = new ArrayList<>();
        List<Itinerary> itineraries = itineraryRepo.findByTripIdOrderByDayNumberAsc(tripId);

        for (Itinerary iti : itineraries) {
            List<ItineraryDetail> details = itineraryDetailRepo.findByItineraryIdOrderByVisitTimeAsc(iti.getId());
            List<com.mobilebackend.ungdunglapkehoachdulich.dto.JournalRes.StopJournal> stops = new ArrayList<>();

            for (ItineraryDetail det : details) {
                List<PostItineraryDetail> postDetails = postItineraryDetailRepo.findByItineraryDetailId(det.getId());
                if (!postDetails.isEmpty()) {
                    // Usually there's only 1 mapped post per detail in typical mapping, but let's take the first
                    PostItineraryDetail pid = postDetails.get(0);
                    com.mobilebackend.ungdunglapkehoachdulich.model.Post post = postRepo.findById(pid.getPostId()).orElse(null);

                    stops.add(com.mobilebackend.ungdunglapkehoachdulich.dto.JournalRes.StopJournal.builder()
                            .itineraryDetailId(det.getId())
                            .visitTime(det.getVisitTime() != null ? det.getVisitTime().toString() : null)
                            .note(det.getNote())
                            .postItineraryDetailId(pid.getId())
                            .status(pid.getStatus())
                            .post(post)
                            .build());
                } else {
                    // Stop without a post linked
                    stops.add(com.mobilebackend.ungdunglapkehoachdulich.dto.JournalRes.StopJournal.builder()
                            .itineraryDetailId(det.getId())
                            .visitTime(det.getVisitTime() != null ? det.getVisitTime().toString() : null)
                            .note(det.getNote())
                            .build());
                }
            }

            dayJournals.add(com.mobilebackend.ungdunglapkehoachdulich.dto.JournalRes.DayJournal.builder()
                    .itineraryId(iti.getId())
                    .dayNumber(iti.getDayNumber())
                    .date(iti.getDate() != null ? iti.getDate().toString() : null)
                    .stops(stops)
                    .build());
        }

        return com.mobilebackend.ungdunglapkehoachdulich.dto.JournalRes.builder()
                .trip(trip)
                .days(dayJournals)
                .build();
    }

    @Transactional
    public void checkInLocation(Integer postItineraryDetailId, Integer userId) {
        PostItineraryDetail pid = postItineraryDetailRepo.findById(postItineraryDetailId)
                .orElseThrow(() -> new RuntimeException("Điểm dừng không tồn tại"));

        ItineraryDetail detail = itineraryDetailRepo.findById(pid.getItineraryDetailId())
                .orElseThrow(() -> new RuntimeException("Chi tiết lịch trình không tồn tại"));

        Itinerary iti = itineraryRepo.findById(detail.getItineraryId())
                .orElseThrow(() -> new RuntimeException("Ngày lịch trình không tồn tại"));

        Trip trip = tripRepo.findById(iti.getTripId())
                .orElseThrow(() -> new RuntimeException("Chuyến đi không tồn tại"));

        if (!trip.getUserId().equals(userId)) {
            tripMemberRepo.findByTripIdAndUserId(trip.getId(), userId)
                .filter(tm -> tm.getStatus() == 1)
                .orElseThrow(() -> new RuntimeException("Bạn không có quyền check-in ở chuyến đi này"));
        }

        pid.setStatus("1");
        postItineraryDetailRepo.save(pid);
    }

    private Trip validateTripAccessByDetail(Integer itineraryDetailId, Integer userId) {
        ItineraryDetail detail = itineraryDetailRepo.findById(itineraryDetailId)
                .orElseThrow(() -> new RuntimeException("Chi tiết lịch trình không tồn tại"));
        Itinerary iti = itineraryRepo.findById(detail.getItineraryId())
                .orElseThrow(() -> new RuntimeException("Ngày lịch trình không tồn tại"));
        Trip trip = tripRepo.findById(iti.getTripId())
                .orElseThrow(() -> new RuntimeException("Chuyến đi không tồn tại"));

        if (!trip.getUserId().equals(userId)) {
            tripMemberRepo.findByTripIdAndUserId(trip.getId(), userId)
                    .filter(tm -> tm.getStatus() == 1)
                    .orElseThrow(() -> new RuntimeException("Bạn không có quyền sửa lịch trình chuyến đi này"));
        }
        return trip;
    }

    @Transactional
    public void updateTripStop(Integer itineraryDetailId, Integer userId, UpdateTripStopReq req) {
        validateTripAccessByDetail(itineraryDetailId, userId);
        ItineraryDetail detail = itineraryDetailRepo.findById(itineraryDetailId)
                .orElseThrow(() -> new RuntimeException("Chi tiết lịch trình không tồn tại"));

        if (req != null) {
            String timeRaw = req.getVisitTime();
            if (timeRaw != null) {
                String normalized = timeRaw.trim();
                if (normalized.isEmpty()) {
                    detail.setVisitTime(null);
                } else {
                    try {
                        detail.setVisitTime(java.time.LocalTime.parse(normalized));
                    } catch (Exception e) {
                        throw new RuntimeException("Giờ không hợp lệ, định dạng đúng là HH:mm");
                    }
                }
            }
            detail.setNote(req.getNote());
        }
        itineraryDetailRepo.save(detail);
    }

    @Transactional
    public void deleteTripStop(Integer itineraryDetailId, Integer userId) {
        validateTripAccessByDetail(itineraryDetailId, userId);
        List<PostItineraryDetail> mappings = postItineraryDetailRepo.findByItineraryDetailId(itineraryDetailId);
        postItineraryDetailRepo.deleteAll(mappings);
        itineraryDetailRepo.deleteById(itineraryDetailId);
    }
}
