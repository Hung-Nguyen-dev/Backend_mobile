package com.mobilebackend.ungdunglapkehoachdulich.service;

import com.mobilebackend.ungdunglapkehoachdulich.dto.TripReq;
import com.mobilebackend.ungdunglapkehoachdulich.model.*;
import com.mobilebackend.ungdunglapkehoachdulich.repo.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {
    private final TripRepo tripRepo;
    private final ItineraryRepo itineraryRepo;
    private final ItineraryDetailRepo itineraryDetailRepo;
    private final PostItineraryDetailRepo postItineraryDetailRepo;

    @Transactional
    public void TripCreateService(Integer userId, TripReq tripReq){
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

//        save itineraryDetail
        for(int i=0;i < itineraryList.size(); i++){
            for(int j=0; j<tripReq.getItineraryReqs().size(); j++){
                ItineraryDetail itineraryDetail = ItineraryDetail.builder()
                    .visitTime(tripReq.getItineraryReqs().get(j).getItineraryDetail().getVisitTime())
                    .note(tripReq.getItineraryReqs().get(j).getItineraryDetail().getNote())
                    .itineraryId(itineraryList.get(i).getId())
                    .build();
                itineraryDetail = itineraryDetailRepo.save(itineraryDetail);

//                save PostItineraryDetail
                for(Map.Entry<Integer, Integer> entry : tripReq.getPostId().entrySet()){
                    Integer day = entry.getKey();
                    if(day == itineraryList.get(i).getDayNumber()){
                        PostItineraryDetail postItineraryDetail = PostItineraryDetail.builder()
                                .itineraryDetailId(itineraryDetail.getId())
                                .postId(entry.getValue())
                                .status("0")
                                .userId(userId)
                                .build();
                        postItineraryDetailRepo.save(postItineraryDetail);
                    }
                }
            }
        }

    }


}
