package com.example.travel.ai;

import com.example.travel.ai.AiItineraryDtos.AiItineraryRequest;
import com.example.travel.ai.AiItineraryDtos.AiItineraryResponse;
import com.example.travel.ai.AiItineraryDtos.SuggestedActivity;
import com.example.travel.ai.AiItineraryDtos.SuggestedDay;
import com.example.travel.ai.AiItineraryDtos.SuggestedRestaurant;
import com.example.travel.places.Place;
import com.example.travel.places.PlaceRepository;
import com.example.travel.places.PlaceSyncService;
import com.example.travel.places.PlaceType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import org.springframework.stereotype.Service;

@Service
public class AiItineraryService {

  private final PlaceRepository placeRepository;
  private final PlaceSyncService placeSyncService;

  public AiItineraryService(PlaceRepository placeRepository, PlaceSyncService placeSyncService) {
    this.placeRepository = placeRepository;
    this.placeSyncService = placeSyncService;
  }

  public AiItineraryResponse generate(AiItineraryRequest req) {
    String city = req.getDestination() == null ? "" : req.getDestination().trim();
    if (city.isEmpty()) {
      throw new IllegalArgumentException("destination is required");
    }
    int days = Math.max(1, Math.min(req.getDayCount(), 14));

    if (!hasAnyPlaces(city)) {
      placeSyncService.syncCity(city);
    }

    List<Place> attractions = placeRepository.findByCityIgnoreCaseAndType(city, PlaceType.ATTRACTION);
    List<Place> restaurants = placeRepository.findByCityIgnoreCaseAndType(city, PlaceType.RESTAURANT);
    List<Place> cafes = placeRepository.findByCityIgnoreCaseAndType(city, PlaceType.CAFE);

    Random rand = new Random();
    List<SuggestedDay> dayList = new ArrayList<>();

    for (int d = 1; d <= days; d++) {
      int dayIndex = d;
      List<SuggestedActivity> acts = new ArrayList<>();

      pickPlaces(attractions, 2, rand)
          .forEach(p -> acts.add(toActivity("attr", dayIndex, p, "3–4h", "08:00")));
      pickPlaces(cafes, 1, rand)
          .forEach(p -> acts.add(toActivity("cafe", dayIndex, p, "1–2h", "15:00")));

      if (acts.isEmpty()) {
        acts.add(
            new SuggestedActivity(
                "fallback-" + dayIndex,
                "Khám phá trung tâm " + city,
                "Dạo quanh các điểm nổi bật (thiếu dữ liệu chi tiết).",
                "Cả ngày",
                "08:00"));
      }

      List<SuggestedRestaurant> restList = new ArrayList<>();
      pickPlaces(restaurants, 2, rand)
          .forEach(p -> restList.add(toRestaurant("rest", dayIndex, p)));

      String label = buildDayLabel(req.getStartDate(), dayIndex);
      dayList.add(new SuggestedDay(dayIndex, label, acts, restList));
    }

    String budgetVi =
        "low".equalsIgnoreCase(req.getBudgetTier())
            ? "tiết kiệm"
            : "high".equalsIgnoreCase(req.getBudgetTier()) ? "thoải mái" : "vừa phải";
    String prefLine =
        req.getPreferences() == null || req.getPreferences().isEmpty()
            ? "đa dạng"
            : String.join(", ", req.getPreferences());

    String summary =
        "Lịch gợi ý "
            + days
            + " ngày tại "
            + city
            + ", ngân sách tham khảo: "
            + budgetVi
            + ". Sở thích: "
            + prefLine
            + ". Dữ liệu điểm đến lấy từ OpenStreetMap (lưu trong MySQL).";

    return new AiItineraryResponse(summary, dayList, Instant.now().toString());
  }

  private boolean hasAnyPlaces(String city) {
    return !placeRepository.findByCityIgnoreCaseAndType(city, PlaceType.ATTRACTION).isEmpty()
        || !placeRepository.findByCityIgnoreCaseAndType(city, PlaceType.RESTAURANT).isEmpty()
        || !placeRepository.findByCityIgnoreCaseAndType(city, PlaceType.CAFE).isEmpty();
  }

  private List<Place> pickPlaces(List<Place> src, int max, Random rand) {
    if (src == null || src.isEmpty() || max <= 0) return Collections.emptyList();
    List<Place> shuffled = new ArrayList<>(src);
    Collections.shuffle(shuffled, rand);
    return shuffled.subList(0, Math.min(max, shuffled.size()));
  }

  private SuggestedActivity toActivity(
      String prefix, int dayIndex, Place p, String duration, String defaultStart) {
    return new SuggestedActivity(
        prefix + "-" + dayIndex + "-" + p.getId(),
        p.getName(),
        p.getAddress(),
        duration,
        defaultStart);
  }

  private SuggestedRestaurant toRestaurant(String prefix, int dayIndex, Place p) {
    String note = p.getAddress() == null ? "" : p.getAddress();
    return new SuggestedRestaurant(
        prefix + "-" + dayIndex + "-" + p.getId(),
        p.getName(),
        note.trim().isEmpty() ? null : note);
  }

  private String buildDayLabel(String startDate, int dayIndex) {
    if (startDate == null || startDate.trim().isEmpty()) return "Ngày " + dayIndex;
    try {
      LocalDate base = LocalDate.parse(startDate);
      LocalDate d = base.plusDays(dayIndex - 1L);
      DateTimeFormatter fmt =
          DateTimeFormatter.ofPattern("EEE dd/MM", new Locale("vi", "VN"));
      return "Ngày " + dayIndex + " — " + d.format(fmt);
    } catch (Exception e) {
      return "Ngày " + dayIndex;
    }
  }
}

