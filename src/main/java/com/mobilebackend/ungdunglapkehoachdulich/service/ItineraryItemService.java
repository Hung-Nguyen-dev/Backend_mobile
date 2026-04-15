package com.mobilebackend.ungdunglapkehoachdulich.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobilebackend.ungdunglapkehoachdulich.dto.itinerary.ItineraryItemCreateReq;
import com.mobilebackend.ungdunglapkehoachdulich.dto.itinerary.ItineraryItemRes;
import com.mobilebackend.ungdunglapkehoachdulich.model.ItineraryItem;
import com.mobilebackend.ungdunglapkehoachdulich.model.Trip;
import com.mobilebackend.ungdunglapkehoachdulich.repo.ItineraryItemRepo;
import com.mobilebackend.ungdunglapkehoachdulich.repo.TripMemberRepo;
import com.mobilebackend.ungdunglapkehoachdulich.repo.TripRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ItineraryItemService {
    private final ItineraryItemRepo itineraryItemRepo;
    private final TripRepo tripRepo;
    private final TripMemberRepo tripMemberRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public ItineraryItemRes create(Integer userId, ItineraryItemCreateReq req) {
        if (req == null || req.getTripId() == null) {
            throw new IllegalArgumentException("Vui long chon chuyen di");
        }
        if (req.getPlaceName() == null || req.getPlaceName().trim().isEmpty()) {
            throw new IllegalArgumentException("Dia diem khong hop le");
        }
        validateTripAccess(userId, req.getTripId());

        ItineraryItem item = ItineraryItem.builder()
                .tripId(req.getTripId())
                .userId(userId)
                .kind(req.getKind())
                .placeName(req.getPlaceName().trim())
                .address(trim(req.getAddress()))
                .lat(req.getLat())
                .lon(req.getLon())
                .phone(trim(req.getPhone()))
                .website(trim(req.getWebsite()))
                .bookingLink(trim(req.getBookingLink()))
                .rating(req.getRating())
                .openNow(req.getOpenNow())
                .amenitiesJson(toJson(req.getAmenities()))
                .reviewsJson(toJson(req.getReviews()))
                .createdAt(LocalDateTime.now())
                .build();
        ItineraryItem saved = itineraryItemRepo.save(item);
        return map(saved);
    }

    @Transactional(readOnly = true)
    public List<ItineraryItemRes> listByTrip(Integer userId, Integer tripId) {
        if (tripId == null) {
            throw new IllegalArgumentException("Trip id khong hop le");
        }
        validateTripAccess(userId, tripId);
        List<ItineraryItem> items = itineraryItemRepo.findByTripIdOrderByCreatedAtDesc(tripId);
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream().map(this::map).toList();
    }

    private void validateTripAccess(Integer userId, Integer tripId) {
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new IllegalArgumentException("Chuyen di khong ton tai"));
        boolean isOwner = trip.getUserId() != null && trip.getUserId().equals(userId);
        boolean isMember = tripMemberRepo.findByTripIdAndUserId(tripId, userId)
                .map(m -> m.getStatus() != null && m.getStatus() == 1)
                .orElse(false);
        if (!isOwner && !isMember) {
            throw new SecurityException("Ban khong co quyen them vao chuyen di nay");
        }
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String toJson(List<String> value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private ItineraryItemRes map(ItineraryItem item) {
        return ItineraryItemRes.builder()
                .id(item.getId())
                .tripId(item.getTripId())
                .userId(item.getUserId())
                .kind(item.getKind())
                .placeName(item.getPlaceName())
                .address(item.getAddress())
                .lat(item.getLat())
                .lon(item.getLon())
                .phone(item.getPhone())
                .website(item.getWebsite())
                .bookingLink(item.getBookingLink())
                .rating(item.getRating())
                .openNow(item.getOpenNow())
                .createdAt(item.getCreatedAt())
                .build();
    }
}
