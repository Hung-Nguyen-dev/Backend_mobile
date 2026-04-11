package com.mobilebackend.ungdunglapkehoachdulich.service;

import com.mobilebackend.ungdunglapkehoachdulich.dto.ai.AiItineraryDtos.AiItineraryRequest;
import com.mobilebackend.ungdunglapkehoachdulich.dto.ai.AiItineraryDtos.AiItineraryResponse;
import com.mobilebackend.ungdunglapkehoachdulich.dto.ai.AiItineraryDtos.SuggestedActivity;
import com.mobilebackend.ungdunglapkehoachdulich.dto.ai.AiItineraryDtos.SuggestedDay;
import com.mobilebackend.ungdunglapkehoachdulich.dto.ai.AiItineraryDtos.SuggestedRestaurant;
import com.mobilebackend.ungdunglapkehoachdulich.service.CityDataService.CityPlace;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.mobilebackend.ungdunglapkehoachdulich.util.GeoUtils;
import org.springframework.stereotype.Service;

@Service
public class AiItineraryService {

    private final CityDataService cityDataService;

    public AiItineraryService(CityDataService cityDataService) {
        this.cityDataService = cityDataService;
    }

    public AiItineraryResponse generate(AiItineraryRequest req) {
        String destination = req.getDestination() == null ? "" : req.getDestination().trim();
        if (destination.isEmpty()) {
            throw new IllegalArgumentException("destination is required");
        }

        int days = Math.max(1, Math.min(req.getDayCount(), 14));
        List<CityPlace> tourism = new ArrayList<>(cityDataService.getPlaces(destination, "tourism"));
        List<CityPlace> restaurants = new ArrayList<>(cityDataService.getPlaces(destination, "restaurant"));
        List<CityPlace> cafes = new ArrayList<>(cityDataService.getPlaces(destination, "cafe"));

        // Sort by preference score
        sortPlacesByPreference(tourism, req.getPreferences());
        sortPlacesByPreference(restaurants, req.getPreferences());
        sortPlacesByPreference(cafes, req.getPreferences());

        List<SuggestedDay> dayList = new ArrayList<>();
        
        // Track used IDs across the whole trip to avoid duplicates
        Set<String> usedPlaceIds = new HashSet<>();

        for (int dayIndex = 1; dayIndex <= days; dayIndex++) {
            List<SuggestedActivity> dayActivities = new ArrayList<>();
            List<SuggestedRestaurant> dayRestaurants = new ArrayList<>();

            Double currentLat = null;
            Double currentLon = null;

            // 1. Morning 1 (Attraction)
            CityPlace m1 = pickBestAvailable(tourism, usedPlaceIds, "08:30", req.getBudgetTier());
            if (m1 != null) {
                dayActivities.add(toActivity(m1, "act-" + dayIndex + "-1", "08:30", "1.5h"));
                currentLat = m1.latitude();
                currentLon = m1.longitude();
                usedPlaceIds.add(m1.placeId());
            }

            // 2. Morning 2 (Attraction)
            CityPlace m2 = pickNearest(currentLat, currentLon, tourism, usedPlaceIds, "10:30", req.getBudgetTier());
            if (m2 != null) {
                dayActivities.add(toActivity(m2, "act-" + dayIndex + "-2", "10:30", "1.5h"));
                currentLat = m2.latitude();
                currentLon = m2.longitude();
                usedPlaceIds.add(m2.placeId());
            }

            // 3. Lunch (Restaurant)
            CityPlace lunch = pickNearest(currentLat, currentLon, restaurants, usedPlaceIds, "12:00", req.getBudgetTier());
            if (lunch != null) {
                dayActivities.add(toActivityAsFood("lunch-" + dayIndex, lunch, "Bữa trưa", "12:00", "1.5h"));
                dayRestaurants.add(toRestaurant("rest-" + dayIndex + "-lunch", lunch, "Bữa trưa"));
                currentLat = lunch.latitude();
                currentLon = lunch.longitude();
                usedPlaceIds.add(lunch.placeId());
            }

            // 4. Afternoon 1 (Attraction)
            CityPlace a1 = pickNearest(currentLat, currentLon, tourism, usedPlaceIds, "14:00", req.getBudgetTier());
            if (a1 != null) {
                dayActivities.add(toActivity(a1, "act-" + dayIndex + "-3", "14:00", "2h"));
                currentLat = a1.latitude();
                currentLon = a1.longitude();
                usedPlaceIds.add(a1.placeId());
            }

            // 5. Afternoon 2 (Attraction)
            CityPlace a2 = pickNearest(currentLat, currentLon, tourism, usedPlaceIds, "16:15", req.getBudgetTier());
            if (a2 != null) {
                dayActivities.add(toActivity(a2, "act-" + dayIndex + "-4", "16:15", "1.5h"));
                currentLat = a2.latitude();
                currentLon = a2.longitude();
                usedPlaceIds.add(a2.placeId());
            }

            // 6. Cafe (Cafe)
            CityPlace coffee = pickNearest(currentLat, currentLon, cafes, usedPlaceIds, "18:00", req.getBudgetTier());
            if (coffee != null) {
                dayActivities.add(toActivityAsFood("cafe-" + dayIndex, coffee, "Thư giãn cafe", "18:00", "1h"));
                dayRestaurants.add(toRestaurant("cafe-" + dayIndex + "-0", coffee, "Cafe"));
                currentLat = coffee.latitude();
                currentLon = coffee.longitude();
                usedPlaceIds.add(coffee.placeId());
            }

            // 7. Dinner (Restaurant)
            CityPlace dinner = pickNearest(currentLat, currentLon, restaurants, usedPlaceIds, "19:30", req.getBudgetTier());
            if (dinner != null) {
                dayActivities.add(toActivityAsFood("dinner-" + dayIndex, dinner, "Bữa tối", "19:30", "1.5h"));
                dayRestaurants.add(toRestaurant("rest-" + dayIndex + "-dinner", dinner, "Bữa tối"));
                currentLat = dinner.latitude();
                currentLon = dinner.longitude();
                usedPlaceIds.add(dinner.placeId());
            }

            // 8. Night (Attraction)
            CityPlace night = pickNearest(currentLat, currentLon, tourism, usedPlaceIds, "21:15", req.getBudgetTier());
            if (night != null) {
                dayActivities.add(toActivity(night, "act-" + dayIndex + "-5", "21:15", "1h"));
                usedPlaceIds.add(night.placeId());
            }

            if (!dayActivities.isEmpty()) {
                dayList.add(new SuggestedDay(
                        dayIndex,
                        buildDayLabel(req.getStartDate(), dayIndex),
                        dayActivities,
                        dayRestaurants
                ));
            }
        }

        boolean totalEmpty = tourism.isEmpty() && restaurants.isEmpty() && cafes.isEmpty();
        String summary = buildSummary(req, destination, days, totalEmpty);

        return new AiItineraryResponse(summary, totalEmpty ? Collections.emptyList() : dayList, Instant.now().toString());
    }

    private void sortPlacesByPreference(List<CityPlace> places, List<String> prefs) {
        if (places.isEmpty() || prefs == null || prefs.isEmpty()) {
            Collections.shuffle(places);
            return;
        }

        places.sort((a, b) -> {
            int scoreA = calculateScore(a, prefs);
            int scoreB = calculateScore(b, prefs);
            if (scoreA != scoreB) return scoreB - scoreA; // Descending
            
            // If scores equal, use rating as secondary
            double rA = a.rating() != null ? a.rating() : 0.0;
            double rB = b.rating() != null ? b.rating() : 0.0;
            return Double.compare(rB, rA);
        });
    }

    private int calculateScore(CityPlace p, List<String> prefs) {
        int score = 0;
        String text = (p.title() + " " + p.type() + " " + p.description()).toLowerCase(Locale.ROOT);
        
        for (String pref : prefs) {
            String pLower = pref.toLowerCase(Locale.ROOT);
            if (pLower.equals("culture") || pLower.equals("văn hóa")) {
                if (containsAny(text, "bảo tàng", "museum", "di tích", "chùa", "đình", "nhà thờ", "lịch sử", "heritage")) score += 10;
            } else if (pLower.equals("adventure") || pLower.equals("mạo hiểm")) {
                if (containsAny(text, "đỉnh", "núi", "rừng", "thác", "trekking", "leo núi", "cáp treo", "hầm")) score += 10;
            } else if (pLower.equals("beach") || pLower.equals("biển")) {
                if (containsAny(text, "biển", "beach", "cát", "vịnh", "đảo", "hải đăng")) score += 10;
            } else if (pLower.equals("family") || pLower.equals("gia đình")) {
                if (containsAny(text, "công viên", "park", "thiếu nhi", "vui chơi", "giải trí", "sở thú", "sun world")) score += 10;
            } else if (pLower.equals("food") || pLower.equals("ẩm thực")) {
                if (containsAny(text, "chợ đêm", "ẩm thực", "ăn uống", "đặc sản", "street food")) score += 10;
            } else if (pLower.equals("chay")) {
                if (containsAny(text, "chay", "vegan", "vegetarian", "thanh tịnh")) score += 10;
            }
        }
        return score;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String k : keywords) {
            if (text.contains(k)) return true;
        }
        return false;
    }


    private CityPlace pickBestAvailable(List<CityPlace> pool, Set<String> usedIds, String activityTime, String budgetTier) {
        // Try with strict filters first
        for (CityPlace p : pool) {
            if (!usedIds.contains(p.placeId()) && isOpenAt(p, activityTime) && matchesBudget(p, budgetTier)) {
                return p;
            }
        }
        // Fallback 1: Ignore budget
        for (CityPlace p : pool) {
            if (!usedIds.contains(p.placeId()) && isOpenAt(p, activityTime)) {
                return p;
            }
        }
        // Fallback 2: Ignore everything except used status
        for (CityPlace p : pool) {
            if (!usedIds.contains(p.placeId())) return p;
        }
        return null;
    }

    private CityPlace pickNearest(Double lat, Double lon, List<CityPlace> pool, Set<String> usedIds, String activityTime, String budgetTier) {
        if (pool.isEmpty()) return null;
        if (lat == null || lon == null) return pickBestAvailable(pool, usedIds, activityTime, budgetTier);

        CityPlace nearest = null;
        double minDistance = Double.MAX_VALUE;

        // Try with strict filters first
        for (CityPlace p : pool) {
            String pid = p.placeId();
            if (usedIds.contains(pid)) continue;
            
            if (!isOpenAt(p, activityTime) || !matchesBudget(p, budgetTier)) continue;

            double dist = GeoUtils.haversineDistance(lat, lon, p.latitude(), p.longitude());
            if (dist < minDistance) {
                minDistance = dist;
                nearest = p;
            }
        }
        
        // Fallback: if no nearest found with filters, use pickBestAvailable fallback logic
        if (nearest == null) {
            return pickBestAvailable(pool, usedIds, activityTime, budgetTier);
        }
        
        return nearest;
    }

    private boolean isOpenAt(CityPlace p, String timeStr) {
        String hours = p.hours();
        if (hours == null || hours.isEmpty()) {
            // Assume open if no data for tourism/restaurants/cafes in typical daylight hours
            return !timeStr.startsWith("23") && !timeStr.startsWith("00") && !timeStr.startsWith("01");
        }

        String hLower = hours.toLowerCase(Locale.ROOT);
        int targetMinutes = parseTimeToMinutes(timeStr);

        // Pattern 1: "Sắp đóng cửa · 22:00" or similar
        if (hLower.contains("đóng cửa")) {
            Integer closeTime = extractTime(hLower);
            if (closeTime != null) {
                // If the target activity starts after closing time, it's NOT open
                return targetMinutes < closeTime;
            }
        }

        // Pattern 2: "Mở cửa lúc 07:00"
        if (hLower.contains("mở cửa lúc")) {
            Integer openTime = extractTime(hLower);
            if (openTime != null) {
                return targetMinutes >= openTime;
            }
        }

        // Pattern 3: "08:00 - 22:00" or "08:00–22:00"
        Pattern rangePattern = Pattern.compile("(\\d{1,2}:\\d{2})\\s*[-–]\\s*(\\d{1,2}:\\d{2})");
        Matcher m = rangePattern.matcher(hLower);
        if (m.find()) {
            int start = parseTimeToMinutes(m.group(1));
            int end = parseTimeToMinutes(m.group(2));
            if (end < start) end += 24 * 60; // Overnight
            return targetMinutes >= start && targetMinutes < end;
        }

        return true; // Default to open if pattern unknown
    }

    private int parseTimeToMinutes(String t) {
        try {
            String[] parts = t.split(":");
            return Integer.parseInt(parts[0].trim()) * 60 + Integer.parseInt(parts[1].trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private Integer extractTime(String text) {
        Pattern timePattern = Pattern.compile("(\\d{1,2}:\\d{2})");
        Matcher matcher = timePattern.matcher(text);
        if (matcher.find()) {
            return parseTimeToMinutes(matcher.group(1));
        }
        return null;
    }

    private boolean matchesBudget(CityPlace p, String tier) {
        if (tier == null || tier.isEmpty() || tier.equalsIgnoreCase("medium")) return true;
        
        String price = p.priceRange();
        String text = (p.title() + " " + p.type() + " " + p.description()).toLowerCase(Locale.ROOT);
        
        if (tier.equalsIgnoreCase("low")) {
            // Check for price range < 150k or keywords
            if (price != null && (price.contains("1-") || price.contains("50.") || price.contains("$") && !price.contains("$$$"))) return true;
            return containsAny(text, "rẻ", "bình dân", "vỉa hè", "giá tốt", "tiết kiệm", "quán ăn nhỏ");
        } else if (tier.equalsIgnoreCase("high")) {
            if (price != null && (price.contains("$$$") || price.contains("500.") || price.contains("1.000."))) return true;
            return containsAny(text, "sang trọng", "cao cấp", "luxury", "fine dining", "đẳng cấp");
        }
        
        return true;
    }

    private SuggestedActivity toActivity(CityPlace source, String id, String start, String duration) {
        return new SuggestedActivity(
                id + "-" + System.nanoTime(),
                source.title(),
                compactDetail(source),
                duration,
                start,
                source.address(),
                source.type(),
                source.hours(),
                source.priceRange(),
                source.rating(),
                source.reviews(),
                source.thumbnail(),
                mapLink(source.title(), source.placeId())
        );
    }

    // addActivity method removed - logic integrated into generate() via Smart Routing

    private SuggestedActivity toActivityAsFood(String id, CityPlace place, String prefix, String start, String duration) {
        String detail = prefix + ": " + compactDetail(place);
        return new SuggestedActivity(
                id,
                place.title(),
                detail,
                duration,
                start,
                place.address(),
                place.type(),
                place.hours(),
                place.priceRange(),
                place.rating(),
                place.reviews(),
                place.thumbnail(),
                mapLink(place.title(), place.placeId())
        );
    }

    private SuggestedRestaurant toRestaurant(String id, CityPlace place, String prefix) {
        return new SuggestedRestaurant(
                id,
                place.title(),
                prefix + ": " + compactDetail(place),
                place.description(),
                place.address(),
                place.type(),
                place.hours(),
                place.priceRange(),
                place.rating(),
                place.reviews(),
                place.thumbnail(),
                mapLink(place.title(), place.placeId())
        );
    }

    private String compactDetail(CityPlace place) {
        List<String> parts = new ArrayList<>();
        if (hasText(place.type())) parts.add(place.type());
        if (hasText(place.description())) parts.add(place.description());
        return parts.isEmpty() ? "Địa điểm nổi bật" : String.join(". ", parts);
    }

    private String buildDayLabel(String startDate, int dayIndex) {
        if (!hasText(startDate)) return "Ngày " + dayIndex;
        try {
            LocalDate target = LocalDate.parse(startDate).plusDays(dayIndex - 1L);
            return "Ngày " + dayIndex + " — " + target.format(DateTimeFormatter.ofPattern("EEE dd/MM", Locale.forLanguageTag("vi-VN")));
        } catch (Exception ex) {
            return "Ngày " + dayIndex;
        }
    }

    private String buildSummary(AiItineraryRequest req, String destination, int days, boolean isEmpty) {
        if (isEmpty) {
            return "Rất tiếc, hiện tại AI chỉ hỗ trợ lên lịch trình cho 11 địa điểm du lịch trọng điểm tại Việt Nam: Hà Nội, TP.HCM, Đà Nẵng, Hà Giang, Ninh Bình, Quảng Ninh (Hạ Long), Thanh Hóa, Nha Trang, Phú Quốc, Huế và Cao Bằng. Bạn vui lòng chọn một trong các địa danh này để bắt đầu nhé!";
        }

        String budgetKey = safeLower(req.getBudgetTier());
        String budgetVi = "low".equals(budgetKey) ? "tiết kiệm" : "high".equals(budgetKey) ? "thoải mái" : "vừa phải";
        String prefLine = (req.getPreferences() == null || req.getPreferences().isEmpty()) ? "đa dạng" : String.join(", ", req.getPreferences());
        
        return String.format("Dưới đây là lịch trình %d ngày tại %s được thiết kế riêng cho bạn. " +
                "Chúng mình đã ưu tiên các địa điểm phù hợp với sở thích '%s' và ngân sách %s của bạn, " +
                "kết hợp hài hòa giữa tham quan và trải nghiệm ẩm thực địa phương.", 
                days, destination, prefLine, budgetVi);
    }

    private CityPlace pickCyclic(List<CityPlace> list, int index) {
        if (list == null || list.isEmpty()) return null;
        return list.get(Math.floorMod(index, list.size()));
    }

    private String mapLink(String title, String placeId) {
        String query = URLEncoder.encode(title == null ? "" : title, StandardCharsets.UTF_8);
        return hasText(placeId) 
            ? "https://www.google.com/maps/search/?api=1&query=" + query + "&query_place_id=" + URLEncoder.encode(placeId, StandardCharsets.UTF_8)
            : "https://www.google.com/maps/search/?api=1&query=" + query;
    }

    private String safeLower(String v) { return v == null ? "" : v.trim().toLowerCase(Locale.ROOT); }
    private boolean hasText(String v) { return v != null && !v.trim().isEmpty(); }
}
