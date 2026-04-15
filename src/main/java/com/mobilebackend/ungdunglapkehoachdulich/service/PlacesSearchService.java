package com.mobilebackend.ungdunglapkehoachdulich.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobilebackend.ungdunglapkehoachdulich.config.TravelPlacesProperties;
import com.mobilebackend.ungdunglapkehoachdulich.dto.places.PlaceMapsEnrichRes;
import com.mobilebackend.ungdunglapkehoachdulich.dto.places.PlaceNearRes;
import com.mobilebackend.ungdunglapkehoachdulich.dto.places.PlaceReviewRes;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlacesSearchService {
    private static final Logger log = LoggerFactory.getLogger(PlacesSearchService.class);

    private final TravelPlacesProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private HttpClient client() {
        int sec = properties.getTimeoutSeconds() != null && properties.getTimeoutSeconds() > 0
                ? properties.getTimeoutSeconds()
                : 25;
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.min(sec, 60)))
                .build();
    }

    /**
     * @param locationQuery tên địa điểm (geocode bằng Photon/Nominatim) — bỏ trống nếu đã có lat/lon
     * @param lat           vĩ độ GPS (WGS84), dùng cùng {@code lon} để tìm quanh vị trí hiện tại
     * @param lon           kinh độ GPS
     */
    public List<PlaceNearRes> searchNearby(
            String locationQuery,
            Double lat,
            Double lon,
            String kind,
            Integer radiusMeters,
            Integer limit
    ) throws IOException, InterruptedException {
        String k = kind == null ? "hotel" : kind.trim().toLowerCase(Locale.ROOT);
        if (!"hotel".equals(k) && !"restaurant".equals(k)) {
            throw new IllegalArgumentException("kind chi ho tro hotel hoac restaurant");
        }

        int radius = radiusMeters != null && radiusMeters > 0 ? radiusMeters
                : (properties.getDefaultRadiusMeters() != null ? properties.getDefaultRadiusMeters() : 4000);
        int max = limit != null && limit > 0 ? Math.min(limit, 50) : 25;

        double centerLat;
        double centerLon;
        if (lat != null && lon != null) {
            if (!Double.isFinite(lat) || !Double.isFinite(lon)) {
                throw new IllegalArgumentException("lat/lon khong hop le");
            }
            if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) {
                throw new IllegalArgumentException("lat/lon ngoai pham vi WGS84");
            }
            centerLat = lat;
            centerLon = lon;
        } else if (locationQuery != null && !locationQuery.isBlank()) {
            double[] center = geocode(locationQuery.trim());
            centerLat = center[0];
            centerLon = center[1];
        } else {
            throw new IllegalArgumentException("Can tham so location hoac cap lat + lon (GPS)");
        }

        TravelPlacesProperties.SerpApi serp = properties.getSerpApi();
        if (serp != null && serp.isEnabled()
                && serp.getApiKey() != null && !serp.getApiKey().isBlank()) {
            try {
                List<PlaceNearRes> fromSerp = searchNearbySerp(centerLat, centerLon, k, max, radius);
                if (!fromSerp.isEmpty()) {
                    return fromSerp;
                }
                log.info("SerpAPI (google_maps) tra ve 0 ket qua — {}", serp.isFallbackToOsm() ? "fallback OSM" : "ket thuc");
                if (!serp.isFallbackToOsm()) {
                    return List.of();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (!serp.isFallbackToOsm()) {
                    throw e;
                }
                log.warn("SerpAPI interrupted, fallback OSM");
            } catch (Exception e) {
                if (!serp.isFallbackToOsm()) {
                    if (e instanceof IOException io) {
                        throw io;
                    }
                    throw new IOException(Objects.toString(e.getMessage(), "SerpAPI loi"), e);
                }
                log.warn("SerpAPI places failed, fallback OSM: {}", e.toString());
            }
        }

        String overpassBody = buildOverpassQuery(k, centerLat, centerLon, radius);
        String json = postOverpass(overpassBody);
        return parseOverpass(json, k, max);
    }

    /**
     * Bo sung thong tin tu Google Maps (SerpAPI) khi client chi co OSM hoac thieu truong.
     * Khong nem exception — tra ve {@link PlaceMapsEnrichRes#empty()} khi loi / tat Serp.
     */
    public PlaceMapsEnrichRes mapsEnrich(
            String placeName, String address, double lat, double lon, String placeIdHint
    ) {
        try {
            return mapsEnrich0(placeName, address, lat, lon, placeIdHint);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return PlaceMapsEnrichRes.empty();
        } catch (Exception e) {
            log.warn("mapsEnrich: {}", e.toString());
            return PlaceMapsEnrichRes.empty();
        }
    }

    private PlaceMapsEnrichRes mapsEnrich0(
            String placeName, String address, double lat, double lon, String placeIdHint
    ) throws IOException, InterruptedException {
        TravelPlacesProperties.SerpApi serp = properties.getSerpApi();
        if (serp == null || !serp.isEnabled()
                || serp.getApiKey() == null || serp.getApiKey().isBlank()) {
            return PlaceMapsEnrichRes.empty();
        }
        String base = trimSlash(serp.getBaseUrl());
        if (base.isEmpty()) {
            return PlaceMapsEnrichRes.empty();
        }
        if (!Double.isFinite(lat) || !Double.isFinite(lon)
                || lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) {
            return PlaceMapsEnrichRes.empty();
        }

        String q = buildEnrichSearchQuery(placeName, address);
        if (q.isBlank()) {
            return PlaceMapsEnrichRes.empty();
        }
        if (log.isTraceEnabled() && placeIdHint != null && !placeIdHint.isBlank()) {
            log.trace("mapsEnrich placeIdHint len={}", placeIdHint.length());
        }

        String ll = "@" + lat + "," + lon + ",3000m";
        String url = base + "/search.json"
                + "?engine=google_maps"
                + "&type=search"
                + "&api_key=" + URLEncoder.encode(serp.getApiKey(), StandardCharsets.UTF_8)
                + "&q=" + URLEncoder.encode(q, StandardCharsets.UTF_8)
                + "&ll=" + URLEncoder.encode(ll, StandardCharsets.UTF_8)
                + "&hl=" + URLEncoder.encode(Objects.toString(serp.getHl(), "vi"), StandardCharsets.UTF_8)
                + "&gl=" + URLEncoder.encode(Objects.toString(serp.getGl(), "vn"), StandardCharsets.UTF_8)
                + "&num=10";

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("Accept", "application/json")
                .header("User-Agent", properties.getUserAgent())
                .GET()
                .build();

        HttpResponse<String> response = client().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) {
            log.warn("mapsEnrich Serp HTTP {}", response.statusCode());
            return PlaceMapsEnrichRes.empty();
        }
        JsonNode root = objectMapper.readTree(response.body());
        if (root.hasNonNull("error")) {
            log.warn("mapsEnrich Serp error {}", root.path("error").asText());
            return PlaceMapsEnrichRes.empty();
        }
        JsonNode local = root.path("local_results");
        if (!local.isArray() || local.isEmpty()) {
            return PlaceMapsEnrichRes.empty();
        }
        JsonNode best = pickBestSerpLocal(local, placeName, lat, lon);
        if (best == null || best.isMissingNode()) {
            return PlaceMapsEnrichRes.empty();
        }
        SerpLocalDetails d = SerpLocalDetails.from(best);
        String snippet = d.snippet();
        if (d.typeLabel() != null && (d.reviewsCount() != null || d.rating() != null)) {
            snippet = null;
        }
        return PlaceMapsEnrichRes.builder()
                .phone(d.phone())
                .website(d.website())
                .bookingLink(d.bookingLink())
                .openingHours(d.openingHours())
                .openNow(d.openNow())
                .openState(d.openState())
                .rating(d.rating())
                .reviewsCount(d.reviewsCount())
                .gpsLat(d.gpsLat())
                .gpsLon(d.gpsLon())
                .amenities(d.amenities())
                .reviews(d.reviews())
                .typeLabel(d.typeLabel())
                .snippet(snippet)
                .description(d.placeDescription())
                .price(d.price())
                .build();
    }

    private static String buildEnrichSearchQuery(String placeName, String address) {
        String n = placeName == null ? "" : placeName.trim();
        String a = address == null ? "" : address.trim();
        if (!n.isEmpty() && !a.isEmpty()) {
            return n + " " + a;
        }
        if (!n.isEmpty()) {
            return n;
        }
        return a;
    }

    /** Chon ket qua gan nhat / trung ten voi placeName. */
    private static JsonNode pickBestSerpLocal(JsonNode localArray, String placeName, double lat, double lon) {
        String pn = placeName == null ? "" : placeName.trim().toLowerCase(Locale.ROOT);
        if (!pn.isEmpty()) {
            for (JsonNode el : localArray) {
                String t = el.path("title").asText("").trim().toLowerCase(Locale.ROOT);
                if (t.equals(pn)) {
                    return el;
                }
            }
            if (pn.length() >= 4) {
                for (JsonNode el : localArray) {
                    String t = el.path("title").asText("").trim().toLowerCase(Locale.ROOT);
                    if (!t.isEmpty() && t.contains(pn)) {
                        return el;
                    }
                }
            }
        }
        JsonNode best = null;
        double bestD = Double.MAX_VALUE;
        for (JsonNode el : localArray) {
            JsonNode gps = el.path("gps_coordinates");
            double plat = gps.path("latitude").asDouble(Double.NaN);
            double plon = gps.path("longitude").asDouble(Double.NaN);
            if (Double.isNaN(plat)) {
                plat = gps.path("lat").asDouble(Double.NaN);
            }
            if (Double.isNaN(plon)) {
                plon = gps.path("lng").asDouble(Double.NaN);
            }
            if (Double.isNaN(plat) || Double.isNaN(plon)) {
                continue;
            }
            double d = haversineMeters(lat, lon, plat, plon);
            if (d < bestD && d <= 8_000.0) {
                bestD = d;
                best = el;
            }
        }
        return best != null ? best : localArray.get(0);
    }

    private record SerpLocalDetails(
            String phone,
            String website,
            String bookingLink,
            String openingHours,
            Boolean openNow,
            String openState,
            Double rating,
            Integer reviewsCount,
            Double gpsLat,
            Double gpsLon,
            List<String> amenities,
            List<PlaceReviewRes> reviews,
            String typeLabel,
            String snippet,
            String placeDescription,
            String price
    ) {
        static SerpLocalDetails from(JsonNode el) {
            String phone = nullIfEmpty(firstNonEmpty(
                    el.path("phone").asText("").trim(),
                    el.path("phone_number").asText("").trim()
            ));
            String website = nullIfEmpty(extractWebsiteFromSerp(el));
            String bookingLink = nullIfEmpty(extractBookingLinkFromSerp(el));
            String opening = nullIfEmpty(buildOpeningHoursFromSerp(el));
            String openState = nullIfEmpty(el.path("open_state").asText("").trim());
            Boolean openNow = extractOpenNow(openState);
            Double rating = parseRatingFromNode(el.path("rating"));
            int rc = parseReviewsCount(el);
            Integer reviewsCount = rc > 0 ? rc : null;
            double[] gps = extractGps(el.path("gps_coordinates"));
            Double gpsLat = Double.isNaN(gps[0]) ? null : gps[0];
            Double gpsLon = Double.isNaN(gps[1]) ? null : gps[1];
            List<String> amenities = parseAmenities(el);
            List<PlaceReviewRes> reviews = parseHighlightedReviews(el);
            String rawType = el.path("type").asText("").trim();
            String typesFallback = typesPreview(el.path("types"), 3);
            String typeLabel = nullIfEmpty(firstNonEmpty(rawType, typesFallback));
            String snippet = nullIfEmpty(buildSerpSnippet(typeLabel, rc, rating));
            String placeDescription = nullIfEmpty(truncate(el.path("description").asText("").trim(), 500));
            String price = nullIfEmpty(el.path("price").asText("").trim());
            return new SerpLocalDetails(
                    phone, website, bookingLink, opening, openNow, openState, rating, reviewsCount,
                    gpsLat, gpsLon, amenities, reviews, typeLabel, snippet, placeDescription, price);
        }
    }

    /** Lay toi da {@code max} nhan tu mang types (SerpAPI). */
    private static String typesPreview(JsonNode typesArr, int max) {
        if (typesArr == null || !typesArr.isArray() || typesArr.isEmpty() || max <= 0) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        for (JsonNode t : typesArr) {
            if (parts.size() >= max) {
                break;
            }
            if (t.isTextual()) {
                String s = t.asText().trim();
                if (!s.isEmpty()) {
                    parts.add(s);
                }
            }
        }
        return parts.isEmpty() ? "" : String.join(", ", parts);
    }

    private static String extractWebsiteFromSerp(JsonNode el) {
        String w = el.path("website").asText("").trim();
        if (!w.isEmpty()) {
            return w;
        }
        JsonNode links = el.path("links");
        if (links.isObject()) {
            w = links.path("website").asText("").trim();
            if (!w.isEmpty()) {
                return w;
            }
        }
        return "";
    }

    private static String extractBookingLinkFromSerp(JsonNode el) {
        String direct = firstNonEmpty(
                el.path("booking_link").asText("").trim(),
                el.path("hotel_booking_link").asText("").trim()
        );
        if (!direct.isEmpty()) {
            return direct;
        }
        JsonNode links = el.path("links");
        if (links.isObject()) {
            return firstNonEmpty(
                    links.path("booking").asText("").trim(),
                    links.path("hotel_booking").asText("").trim(),
                    links.path("reservations").asText("").trim()
            );
        }
        return "";
    }

    private static Boolean extractOpenNow(String openState) {
        if (openState == null || openState.isBlank()) {
            return null;
        }
        String s = openState.toLowerCase(Locale.ROOT);
        if (s.contains("open") || s.contains("mở")) {
            return true;
        }
        if (s.contains("closed") || s.contains("đóng")) {
            return false;
        }
        return null;
    }

    private static double[] extractGps(JsonNode gps) {
        if (gps == null || gps.isMissingNode()) {
            return new double[]{Double.NaN, Double.NaN};
        }
        double lat = gps.path("latitude").asDouble(Double.NaN);
        double lon = gps.path("longitude").asDouble(Double.NaN);
        if (Double.isNaN(lat)) {
            lat = gps.path("lat").asDouble(Double.NaN);
        }
        if (Double.isNaN(lon)) {
            lon = gps.path("lng").asDouble(Double.NaN);
        }
        return new double[]{lat, lon};
    }

    private static List<String> parseAmenities(JsonNode el) {
        List<String> out = new ArrayList<>();
        JsonNode amenities = el.path("amenities");
        if (amenities.isArray()) {
            for (JsonNode node : amenities) {
                if (node.isTextual()) {
                    String value = node.asText().trim();
                    if (!value.isEmpty()) {
                        out.add(value);
                    }
                }
            }
        }
        JsonNode serviceOptions = el.path("service_options");
        if (serviceOptions.isArray()) {
            for (JsonNode node : serviceOptions) {
                if (node.isTextual()) {
                    String value = node.asText().trim();
                    if (!value.isEmpty()) {
                        out.add(value);
                    }
                }
            }
        }
        return out.stream().distinct().limit(10).collect(Collectors.toList());
    }

    private static List<PlaceReviewRes> parseHighlightedReviews(JsonNode el) {
        JsonNode reviews = el.path("reviews");
        if (!reviews.isArray()) {
            return List.of();
        }
        List<PlaceReviewRes> out = new ArrayList<>();
        for (JsonNode review : reviews) {
            if (!review.isObject()) {
                continue;
            }
            String summary = firstNonEmpty(
                    review.path("snippet").asText("").trim(),
                    review.path("summary").asText("").trim(),
                    review.path("text").asText("").trim()
            );
            if (summary.isEmpty()) {
                continue;
            }
            out.add(PlaceReviewRes.builder()
                    .user(nullIfEmpty(firstNonEmpty(
                            review.path("user").asText("").trim(),
                            review.path("user_name").asText("").trim(),
                            review.path("author").asText("").trim()
                    )))
                    .summary(truncate(summary, 220))
                    .rating(parseRatingFromNode(review.path("rating")))
                    .relativeDate(nullIfEmpty(firstNonEmpty(
                            review.path("date").asText("").trim(),
                            review.path("published").asText("").trim()
                    )))
                    .build());
            if (out.size() >= 5) {
                break;
            }
        }
        return out;
    }

    private static String buildOpeningHoursFromSerp(JsonNode el) {
        StringBuilder sb = new StringBuilder();
        String openState = el.path("open_state").asText("").trim();
        if (!openState.isEmpty()) {
            sb.append(openState);
        }
        JsonNode hours = el.path("hours");
        /*
         * SerpAPI: hours thuong la chuoi tom tat ("Open ..."), operating_hours la object theo ngay.
         * Truoc day chi xu ly hours kieu object/array — bo sot hau het ket qua that.
         */
        if (hours.isTextual()) {
            String ht = hours.asText().trim();
            if (!ht.isEmpty() && !ht.equalsIgnoreCase(openState)) {
                if (!sb.isEmpty()) {
                    sb.append('\n');
                }
                sb.append(ht);
            }
        }
        appendSerpOperatingHours(sb, el.path("operating_hours"));
        if (hours.isObject()) {
            appendSerpDayHourMap(sb, hours);
        } else if (hours.isArray()) {
            for (JsonNode x : hours) {
                if (x.isTextual()) {
                    String t = x.asText().trim();
                    if (!t.isEmpty()) {
                        if (!sb.isEmpty()) {
                            sb.append('\n');
                        }
                        sb.append(t);
                    }
                }
            }
        }
        JsonNode ext = el.path("extensions");
        if (ext.isArray()) {
            for (JsonNode x : ext) {
                if (x.isTextual()) {
                    String t = x.asText().trim();
                    if (!t.isEmpty()) {
                        if (!sb.isEmpty()) {
                            sb.append('\n');
                        }
                        sb.append(t);
                    }
                }
            }
        }
        return truncate(sb.toString().trim(), 650);
    }

    private static void appendSerpOperatingHours(StringBuilder sb, JsonNode op) {
        if (op == null || !op.isObject()) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> it = op.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            JsonNode v = e.getValue();
            if (!v.isTextual()) {
                continue;
            }
            String slot = v.asText().trim();
            if (slot.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append('\n');
            }
            String day = e.getKey();
            if (!day.isEmpty()) {
                sb.append(day.substring(0, 1).toUpperCase(Locale.ROOT));
                if (day.length() > 1) {
                    sb.append(day.substring(1).toLowerCase(Locale.ROOT));
                }
                sb.append(": ");
            }
            sb.append(slot);
        }
    }

    private static void appendSerpDayHourMap(StringBuilder sb, JsonNode hoursObj) {
        if (hoursObj == null || !hoursObj.isObject()) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> it = hoursObj.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> e = it.next();
            JsonNode v = e.getValue();
            if (!v.isTextual()) {
                continue;
            }
            String slot = v.asText().trim();
            if (slot.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append('\n');
            }
            sb.append(e.getKey()).append(": ").append(slot);
        }
    }

    private static int parseReviewsCount(JsonNode el) {
        JsonNode r = el.path("reviews");
        if (r.isInt() || r.isLong()) {
            return Math.max(0, r.asInt());
        }
        if (r.isTextual()) {
            try {
                String digits = r.asText().replaceAll("[^0-9]", "");
                if (!digits.isEmpty()) {
                    return Math.max(0, Integer.parseInt(digits));
                }
            } catch (NumberFormatException ignored) {
                // keep 0
            }
        }
        JsonNode u = el.path("user_review_count");
        if (u.isInt() || u.isLong()) {
            return Math.max(0, u.asInt());
        }
        return 0;
    }

    private static Double parseRatingFromNode(JsonNode n) {
        if (n == null || n.isMissingNode()) {
            return null;
        }
        if (n.isNumber()) {
            double v = n.asDouble();
            return Double.isFinite(v) ? v : null;
        }
        if (n.isTextual()) {
            try {
                double v = Double.parseDouble(n.asText().replace(',', '.'));
                return Double.isFinite(v) ? v : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private static String buildSerpSnippet(String typeLabel, int reviewsCount, Double rating) {
        StringBuilder d = new StringBuilder();
        if (typeLabel != null && !typeLabel.isEmpty()) {
            d.append(typeLabel);
        }
        if (reviewsCount > 0) {
            if (!d.isEmpty()) {
                d.append(" · ");
            }
            d.append(reviewsCount).append(" đánh giá (Google Maps)");
        } else if (rating != null) {
            if (!d.isEmpty()) {
                d.append(" · ");
            }
            d.append(String.format(Locale.ROOT, "%.1f★", rating));
        }
        return d.toString().trim();
    }

    /**
     * SerpAPI engine=google_maps, type=search — {@code local_results}.
     * Google Maps hay tra ca “hot” xa tam — loc theo {@code radiusMeters} (haversine), sap xep gan nhat truoc.
     */
    private List<PlaceNearRes> searchNearbySerp(
            double centerLat, double centerLon, String kind, int maxResults, int radiusMeters
    ) throws IOException, InterruptedException {
        TravelPlacesProperties.SerpApi serp = properties.getSerpApi();
        String base = trimSlash(serp.getBaseUrl());
        if (base.isEmpty()) {
            throw new IllegalStateException("serp-api.base-url trong");
        }
        String qKeyword = "restaurant".equals(kind) ? "restaurant" : "hotel";
        /*
         * ll dang @lat,lon,{met}m — khung ban do gan dung bang kinh tim kiem (SerpAPI / Google hay bo qua zoom lon).
         */
        long viewMl = Math.clamp(Math.round((double) radiusMeters * 2.2), 1_200L, 12_000L);
        int viewM = (int) viewMl;
        String ll = "@" + centerLat + "," + centerLon + "," + viewM + "m";
        int num = 20;

        String url = base + "/search.json"
                + "?engine=google_maps"
                + "&type=search"
                + "&api_key=" + URLEncoder.encode(serp.getApiKey(), StandardCharsets.UTF_8)
                + "&q=" + URLEncoder.encode(qKeyword, StandardCharsets.UTF_8)
                + "&ll=" + URLEncoder.encode(ll, StandardCharsets.UTF_8)
                + "&hl=" + URLEncoder.encode(Objects.toString(serp.getHl(), "vi"), StandardCharsets.UTF_8)
                + "&gl=" + URLEncoder.encode(Objects.toString(serp.getGl(), "vn"), StandardCharsets.UTF_8)
                + "&num=" + num;

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("Accept", "application/json")
                .header("User-Agent", properties.getUserAgent())
                .GET()
                .build();

        HttpResponse<String> response = client().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("SerpAPI HTTP " + response.statusCode());
        }
        List<PlaceNearRes> raw = parseSerpGoogleMapsLocal(response.body(), kind);
        return filterSerpByRadius(raw, centerLat, centerLon, radiusMeters, maxResults);
    }

    /** Chi giu dia diem trong ban kinh (cong them ~12% cho sai so GPS). */
    private static List<PlaceNearRes> filterSerpByRadius(
            List<PlaceNearRes> raw, double centerLat, double centerLon, int radiusMeters, int maxResults
    ) {
        if (raw.isEmpty()) {
            return List.of();
        }
        double limitM = Math.max(100.0, radiusMeters) * 1.12;
        record Row(PlaceNearRes p, double dist) {}
        List<Row> rows = new ArrayList<>();
        for (PlaceNearRes p : raw) {
            double d = haversineMeters(centerLat, centerLon, p.getLat(), p.getLon());
            if (d <= limitM) {
                rows.add(new Row(p, d));
            }
        }
        rows.sort(Comparator.comparingDouble(Row::dist));
        List<PlaceNearRes> out = new ArrayList<>();
        for (int i = 0; i < rows.size() && i < maxResults; i++) {
            out.add(rows.get(i).p());
        }
        return out;
    }

    private static double haversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double r = 6371000.0;
        double p1 = Math.toRadians(lat1);
        double p2 = Math.toRadians(lat2);
        double dp = Math.toRadians(lat2 - lat1);
        double dl = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dp / 2) * Math.sin(dp / 2)
                + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(Math.max(0.0, 1.0 - a)));
        return r * c;
    }

    private List<PlaceNearRes> parseSerpGoogleMapsLocal(String jsonBody, String kind) throws IOException {
        JsonNode root = objectMapper.readTree(jsonBody);
        if (root.hasNonNull("error")) {
            String err = root.path("error").asText("SerpAPI error");
            throw new IllegalStateException(err);
        }
        JsonNode local = root.path("local_results");
        if (!local.isArray()) {
            return List.of();
        }

        List<PlaceNearRes> out = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (JsonNode el : local) {
            String title = el.path("title").asText("").trim();
            if (title.isEmpty()) {
                title = "Dia diem (Maps)";
            }
            JsonNode gps = el.path("gps_coordinates");
            double plat = gps.path("latitude").asDouble(Double.NaN);
            double plon = gps.path("longitude").asDouble(Double.NaN);
            if (Double.isNaN(plat)) {
                plat = gps.path("lat").asDouble(Double.NaN);
            }
            if (Double.isNaN(plon)) {
                plon = gps.path("lng").asDouble(Double.NaN);
            }
            if (Double.isNaN(plat) || Double.isNaN(plon)) {
                continue;
            }

            String placeId = el.path("place_id").asText("").trim();
            String dedupe = placeId.isEmpty() ? title + "|" + plat + "|" + plon : placeId;
            if (!seen.add(dedupe)) {
                continue;
            }

            String address = el.path("address").asText("").trim();
            String link = el.path("link").asText("").trim();
            if (link.isEmpty()) {
                link = "https://www.google.com/maps/search/?api=1&query="
                        + URLEncoder.encode(title + " " + address, StandardCharsets.UTF_8);
            }

            String thumb = el.path("thumbnail").asText("").trim();
            if (thumb.isEmpty()) {
                thumb = el.path("serpapi_thumbnail").asText("").trim();
            }
            SerpLocalDetails ex = SerpLocalDetails.from(el);
            String listDesc = firstNonEmpty(
                    ex.placeDescription() == null ? "" : ex.placeDescription(),
                    ex.snippet() == null ? "" : ex.snippet()
            );

            out.add(PlaceNearRes.builder()
                    .osmType("serpapi")
                    .osmId(placeId.isEmpty() ? ("p_" + Integer.toHexString(dedupe.hashCode())) : placeId)
                    .kind(kind)
                    .name(title)
                    .addressLine(address)
                    .lat(plat)
                    .lon(plon)
                    .openStreetMapUrl(link)
                    .previewImageUrl(nullIfEmpty(thumb))
                    .phone(ex.phone())
                    .website(ex.website())
                    .openingHours(ex.openingHours())
                    .cuisine(null)
                    .description(nullIfEmpty(listDesc))
                    .rating(ex.rating())
                    .build());
        }
        return out;
    }

    /**
     * Geocode: Photon (Komoot) trước — ít bị 403 hơn Nominatim công khai khi gọi từ server.
     * Nominatim chỉ dùng khi Photon không có kết quả hoặc lỗi mạng.
     */
    private double[] geocode(String q) throws IOException, InterruptedException {
        try {
            return geocodePhoton(q, null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (IllegalArgumentException e) {
            return geocodeNominatim(q);
        } catch (IOException | IllegalStateException e) {
            try {
                return geocodeNominatim(q);
            } catch (Exception nominatimEx) {
                if (e instanceof IOException io) {
                    throw io;
                }
                throw new IOException(Objects.toString(e.getMessage(), "Photon loi"), e);
            }
        }
    }

    private double[] geocodeNominatim(String q) throws IOException, InterruptedException {
        String base = trimSlash(properties.getNominatimBaseUrl());
        if (base.isEmpty()) {
            throw new IllegalStateException("Chua cau hinh nominatim-base-url");
        }
        String url = base + "/search?q=" + URLEncoder.encode(q, StandardCharsets.UTF_8)
                + "&format=json&limit=1";

        HttpRequest.Builder rb = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("User-Agent", properties.getUserAgent())
                .header("Accept", "application/json")
                .header("Accept-Language", "vi,en");
        String from = properties.getNominatimFromEmail();
        if (from != null && !from.isBlank()) {
            rb.header("From", from.trim());
        }
        HttpRequest request = rb.GET().build();

        HttpResponse<String> response = client().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("Nominatim HTTP " + response.statusCode());
        }

        JsonNode arr = objectMapper.readTree(response.body());
        if (!arr.isArray() || arr.isEmpty()) {
            throw new IllegalArgumentException("Khong tim thay dia diem: " + q);
        }
        JsonNode first = arr.get(0);
        double lat = first.path("lat").asDouble(Double.NaN);
        double lon = first.path("lon").asDouble(Double.NaN);
        if (Double.isNaN(lat) || Double.isNaN(lon)) {
            throw new IllegalArgumentException("Toa do dia diem khong hop le");
        }
        return new double[]{lat, lon};
    }

    private double[] geocodePhoton(String q, IllegalArgumentException nominatimMiss) throws IOException, InterruptedException {
        String base = trimSlash(Objects.toString(properties.getPhotonBaseUrl(), ""));
        if (base.isEmpty()) {
            if (nominatimMiss != null) {
                throw nominatimMiss;
            }
            throw new IllegalStateException("Photon chua cau hinh");
        }
        String url = base + "/api/?q=" + URLEncoder.encode(q, StandardCharsets.UTF_8) + "&limit=1&lang=vi";

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("User-Agent", properties.getUserAgent())
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) {
            if (nominatimMiss != null) {
                throw nominatimMiss;
            }
            throw new IllegalStateException("Photon HTTP " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode features = root.path("features");
        if (!features.isArray() || features.isEmpty()) {
            if (nominatimMiss != null) {
                throw nominatimMiss;
            }
            throw new IllegalArgumentException("Khong tim thay dia diem (Photon): " + q);
        }
        JsonNode coords = features.get(0).path("geometry").path("coordinates");
        if (!coords.isArray() || coords.size() < 2) {
            if (nominatimMiss != null) {
                throw nominatimMiss;
            }
            throw new IllegalArgumentException("Photon tra ve toa do khong hop le");
        }
        double lon = coords.get(0).asDouble(Double.NaN);
        double lat = coords.get(1).asDouble(Double.NaN);
        if (Double.isNaN(lat) || Double.isNaN(lon)) {
            if (nominatimMiss != null) {
                throw nominatimMiss;
            }
            throw new IllegalArgumentException("Photon toa do NaN");
        }
        return new double[]{lat, lon};
    }

    private String postOverpass(String body) throws IOException, InterruptedException {
        List<String> urls = new ArrayList<>();
        String primary = trimSlash(properties.getOverpassInterpreterUrl());
        if (!primary.isEmpty()) {
            urls.add(primary);
        }
        if (properties.getOverpassFallbackUrls() != null) {
            for (String u : properties.getOverpassFallbackUrls()) {
                String t = trimSlash(u);
                if (!t.isEmpty() && !urls.contains(t)) {
                    urls.add(t);
                }
            }
        }
        if (urls.isEmpty()) {
            throw new IllegalStateException("Chua cau hinh overpass-interpreter-url");
        }

        Exception last = null;
        for (String url : urls) {
            try {
                return postOverpassOnce(url, body);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            } catch (Exception ex) {
                last = ex;
            }
        }
        if (last instanceof IOException io) {
            throw io;
        }
        if (last instanceof RuntimeException re) {
            throw re;
        }
        throw new IOException(last != null ? last.getMessage() : "Overpass: tat ca mirror deu loi");
    }

    private String postOverpassOnce(String url, String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("Content-Type", "text/plain; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client().send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("Overpass " + url + " HTTP " + response.statusCode());
        }
        return response.body();
    }

    private String buildOverpassQuery(String kind, double lat, double lon, int radiusM) {
        String union;
        if ("hotel".equals(kind)) {
            union = """
                    (
                      node["tourism"~"hotel|guest_house|motel"](around:%d,%.6f,%.6f);
                      way["tourism"~"hotel|guest_house|motel"](around:%d,%.6f,%.6f);
                    );
                    """.formatted(radiusM, lat, lon, radiusM, lat, lon);
        } else {
            union = """
                    (
                      node["amenity"="restaurant"](around:%d,%.6f,%.6f);
                      way["amenity"="restaurant"](around:%d,%.6f,%.6f);
                    );
                    """.formatted(radiusM, lat, lon, radiusM, lat, lon);
        }
        // Gioi han so ket qua xu ly phia Java (Overpass: out center cho way co tam)
        return "[out:json][timeout:25];\n" + union + "out center;\n";
    }

    private List<PlaceNearRes> parseOverpass(String json, String kind, int maxResults) throws IOException {
        JsonNode root = objectMapper.readTree(json);
        JsonNode elements = root.path("elements");
        if (!elements.isArray()) {
            return List.of();
        }

        Set<String> seen = new LinkedHashSet<>();
        List<PlaceNearRes> out = new ArrayList<>();

        for (JsonNode el : elements) {
            if (out.size() >= maxResults) {
                break;
            }
            String type = el.path("type").asText("");
            long id = el.path("id").asLong(0);
            String key = type + ":" + id;
            if (!seen.add(key)) {
                continue;
            }

            double lat;
            double lon;
            if ("node".equals(type)) {
                lat = el.path("lat").asDouble(Double.NaN);
                lon = el.path("lon").asDouble(Double.NaN);
            } else {
                JsonNode c = el.path("center");
                lat = c.path("lat").asDouble(Double.NaN);
                lon = c.path("lon").asDouble(Double.NaN);
            }
            if (Double.isNaN(lat) || Double.isNaN(lon)) {
                continue;
            }

            JsonNode tags = el.path("tags");
            String name = textOr(tags, "name");
            if (name.isEmpty()) {
                name = "Khong co ten (OSM)";
            }
            String addressLine = buildAddress(tags);
            String previewImage = previewImageFromTags(tags);
            String phone = firstNonEmpty(
                    textOr(tags, "phone"),
                    textOr(tags, "contact:phone"),
                    textOr(tags, "contact:mobile")
            );
            String website = firstNonEmpty(
                    textOr(tags, "website"),
                    textOr(tags, "contact:website"),
                    textOr(tags, "url")
            );
            String openingHours = truncate(textOr(tags, "opening_hours"), 650);
            String cuisine = textOr(tags, "cuisine");
            String description = truncate(textOr(tags, "description"), 500);

            String osmUrl = "https://www.openstreetmap.org/" + type + "/" + id;

            out.add(PlaceNearRes.builder()
                    .osmType(type)
                    .osmId(String.valueOf(id))
                    .kind(kind)
                    .name(name)
                    .addressLine(addressLine)
                    .lat(lat)
                    .lon(lon)
                    .openStreetMapUrl(osmUrl)
                    .previewImageUrl(previewImage)
                    .phone(nullIfEmpty(phone))
                    .website(nullIfEmpty(website))
                    .openingHours(nullIfEmpty(openingHours))
                    .cuisine(nullIfEmpty(cuisine))
                    .description(nullIfEmpty(description))
                    .build());
        }

        return out;
    }

    /**
     * Anh tham khao tren OSM: tag {@code image} (URL) hoac {@code wikimedia_commons} (File:...).
     */
    private String previewImageFromTags(JsonNode tags) {
        if (tags == null || !tags.isObject()) {
            return null;
        }
        String image = textOr(tags, "image");
        if (looksLikeHttpUrl(image)) {
            return image.trim();
        }
        String wc = textOr(tags, "wikimedia_commons");
        if (wc.isEmpty()) {
            return null;
        }
        if (wc.startsWith("File:")) {
            String fileName = wc.substring("File:".length()).trim();
            if (fileName.isEmpty()) {
                return null;
            }
            return "https://commons.wikimedia.org/wiki/Special:FilePath/"
                    + URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        }
        return null;
    }

    private static boolean looksLikeHttpUrl(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        String t = s.trim().toLowerCase(Locale.ROOT);
        return t.startsWith("http://") || t.startsWith("https://");
    }

    private static String firstNonEmpty(String... parts) {
        if (parts == null) {
            return "";
        }
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                return p.trim();
            }
        }
        return "";
    }

    private static String truncate(String s, int maxLen) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        if (s.length() <= maxLen) {
            return s;
        }
        return s.substring(0, maxLen) + "...";
    }

    private static String nullIfEmpty(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s.trim();
    }

    private static String textOr(JsonNode tags, String field) {
        if (tags == null || !tags.isObject()) {
            return "";
        }
        JsonNode n = tags.get(field);
        return n == null || !n.isTextual() ? "" : n.asText().trim();
    }

    private static String buildAddress(JsonNode tags) {
        if (tags == null || !tags.isObject()) {
            return "";
        }
        String street = textOr(tags, "addr:street");
        String house = textOr(tags, "addr:housenumber");
        String city = textOr(tags, "addr:city");
        if (city.isEmpty()) {
            city = textOr(tags, "addr:district");
        }
        StringBuilder sb = new StringBuilder();
        if (!house.isEmpty()) {
            sb.append(house).append(' ');
        }
        if (!street.isEmpty()) {
            sb.append(street);
        }
        if (!city.isEmpty()) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(city);
        }
        return sb.toString().trim();
    }

    private static String trimSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
