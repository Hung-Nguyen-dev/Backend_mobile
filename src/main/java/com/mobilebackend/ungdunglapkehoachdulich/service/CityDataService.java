package com.mobilebackend.ungdunglapkehoachdulich.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

@Service
public class CityDataService {
    private static final String RESOURCE_PATTERN = "classpath*:city-data/*/*.json";

    private final PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
    private final Map<String, Map<String, List<CityPlace>>> cache = new LinkedHashMap<>();
    private final AtomicBoolean loaded = new AtomicBoolean(false);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<CityPlace> getPlaces(String destination, String category) {
        ensureLoaded();
        String cityKey = normalizeCityKey(destination);
        String categoryKey = normalizeCategory(category);
        
        if (categoryKey.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<CityPlace>> cityData = cache.get(cityKey);
        
        if (cityData == null) {
            return Collections.emptyList();
        }

        List<CityPlace> places = cityData.getOrDefault(categoryKey, Collections.emptyList());
        List<CityPlace> copy = new ArrayList<>(places);
        copy.sort(Comparator
                .comparing(CityPlace::rating, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CityPlace::reviews, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(CityPlace::title));
        return copy;
    }

    public List<CityPlace> searchPlaces(String destination, String category, String query) {
        List<CityPlace> places = getPlaces(destination, category);
        if (query == null || query.trim().isEmpty()) {
            return places;
        }

        String needle = query.trim().toLowerCase(Locale.ROOT);
        List<CityPlace> result = new ArrayList<>();
        for (CityPlace place : places) {
            if (contains(place.title(), needle)
                    || contains(place.type(), needle)
                    || contains(place.address(), needle)
                    || contains(place.description(), needle)) {
                result.add(place);
            }
        }
        return result;
    }

    public String getCityContext(String destination, String query) {
        ensureLoaded();
        String cityKey = normalizeCityKey(destination);
        if (cityKey.isEmpty()) {
            return "";
        }

        Map<String, List<CityPlace>> cityData = cache.get(cityKey);
        if (cityData == null || cityData.isEmpty()) {
            return "";
        }

        List<String> tokens = extractQueryTokens(query);
        StringBuilder sb = new StringBuilder();
        sb.append("Thành phố: ").append(cityKey).append('\n');
        if (query != null && !query.isBlank()) {
            sb.append("Câu hỏi người dùng: ").append(query.trim()).append('\n');
        }

        appendContextSection(sb, "tourism", "Điểm tham quan", cityData.get("tourism"), tokens);
        appendContextSection(sb, "restaurant", "Nhà hàng", cityData.get("restaurant"), tokens);
        appendContextSection(sb, "cafe", "Quán cà phê", cityData.get("cafe"), tokens);

        return sb.toString().trim();
    }

    private void ensureLoaded() {
        if (loaded.get()) {
            return;
        }

        synchronized (this) {
            if (loaded.get()) {
                return;
            }

            try {
                Resource[] resources = resourceResolver.getResources(RESOURCE_PATTERN);
                for (Resource resource : resources) {
                    loadResource(resource);
                }
                loaded.set(true);
            } catch (IOException ex) {
                throw new IllegalStateException("Khong the doc city-data tu resources", ex);
            }
        }
    }

    private void loadResource(Resource resource) throws IOException {
        if (resource == null || !resource.exists()) {
            return;
        }

        String path = resolveResourcePath(resource);
        if (path == null) {
            return;
        }

        String[] segments = path.split("/");
        if (segments.length < 2) {
            return;
        }

        String cityKey = normalizeCityKey(segments[segments.length - 2]);
        if (cityKey.isEmpty()) {
            return;
        }

        String fileName = segments[segments.length - 1].toLowerCase(Locale.ROOT);
        String categoryKey = normalizeCategory(fileName);
        if (categoryKey.isEmpty()) {
            return;
        }

        try (InputStream inputStream = resource.getInputStream()) {
            JsonNode root = objectMapper.readTree(inputStream);
            JsonNode nodes;
            if (root.isArray()) {
                nodes = root;
            } else {
                nodes = root.path("local_results");
            }

            if (!nodes.isArray()) {
                return;
            }

            List<CityPlace> places = new ArrayList<>();
            for (JsonNode node : nodes) {
                CityPlace place = toCityPlace(cityKey, categoryKey, node);
                if (place != null) {
                    places.add(place);
                }
            }

            if (!places.isEmpty()) {
                cache.computeIfAbsent(cityKey, ignored -> new LinkedHashMap<>())
                        .put(categoryKey, places);
            }
        }
    }

    private CityPlace toCityPlace(String cityKey, String categoryKey, JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        String title = text(node, "title");
        if (title.isEmpty()) {
            return null;
        }

        // Fallback to title + coordinates if place_id is missing to ensure unique IDs for duplicate detection
        String placeId = text(node, "place_id");
        if (placeId.isEmpty()) {
            Double latNode = number(node.path("gps_coordinates"), "latitude");
            Double lonNode = number(node.path("gps_coordinates"), "longitude");
            double lat = latNode != null ? latNode : 0.0;
            double lon = lonNode != null ? lonNode : 0.0;
            String rawId = title + "-" + lat + "-" + lon;
            placeId = "id-" + rawId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "-");
        }

        return new CityPlace(
                cityKey,
                categoryKey,
                title,
                text(node, "type"),
                text(node, "address"),
                text(node, "description"),
                number(node, "rating"),
                integer(node, "reviews"),
                text(node, "thumbnail"),
                placeId,
                text(node, "hours"),
                text(node, "price"),
                number(node.path("gps_coordinates"), "latitude"),
                number(node.path("gps_coordinates"), "longitude")
        );
    }

    private String resolveResourcePath(Resource resource) {
        try {
            URI uri = resource.getURI();
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return null;
            }
            int index = path.indexOf("city-data/");
            return index >= 0 ? path.substring(index) : path;
        } catch (IOException ex) {
            return null;
        }
    }

    private String normalizeCategory(String value) {
        if (value == null) {
            return "";
        }

        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.contains("tourism")) {
            return "tourism";
        }
        if (lower.contains("cafe")) {
            return "cafe";
        }
        if (lower.contains("restaurant") || lower.contains("restaurent")) {
            return "restaurant";
        }
        return "";
    }

    private String normalizeCityKey(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }

        String normalized = removeDiacritics(value).toLowerCase(Locale.ROOT).replaceAll("[\\s_-]+", "");
        
        // Strictly limited to the 11 pre-defined cities with high-quality data
        if (normalized.contains("hanoi")) return "HaNoi";
        if (normalized.contains("hcm") || normalized.contains("hochiminh") || normalized.contains("saigon")) return "HCM";
        if (normalized.contains("danang")) return "DaNang";
        if (normalized.contains("hagiang")) return "HaGiang";
        if (normalized.contains("ninhbinh")) return "NinhBinh";
        if (normalized.contains("quangninh") || normalized.contains("halong")) return "QuangNinh";
        if (normalized.contains("thanhhoa")) return "ThanhHoa";
        if (normalized.contains("nhatrang")) return "NhaTrang";
        if (normalized.contains("phuquoc")) return "PhuQuoc";
        if (normalized.contains("hue")) return "Hue";
        if (normalized.contains("caobang")) return "CaoBang";
        if (normalized.contains("dalat")) return "DaLat";

        return "";
    }

    private String removeDiacritics(String str) {
        if (str == null) {
            return "";
        }
        String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfdNormalizedString)
                .replaceAll("")
                .replace('đ', 'd')
                .replace('Đ', 'D')
                .toLowerCase(Locale.ROOT);
    }

    private void generateMissingCityData(String cityKey, String cityName) {
        synchronized (cache) {
            if (cache.containsKey(cityKey)) return;

            Map<String, List<CityPlace>> mockData = new LinkedHashMap<>();
            // Tourism
            mockData.put("tourism", createMockPlaces(cityKey, "tourism", cityName, "Điểm tham quan nổi tiếng", 5));
            // Restaurant
            mockData.put("restaurant", createMockPlaces(cityKey, "restaurant", cityName, "Nhà hàng đặc sản", 5));
            // Cafe
            mockData.put("cafe", createMockPlaces(cityKey, "cafe", cityName, "Quán cafe view đẹp", 5));

            cache.put(cityKey, mockData);
        }
    }

    private List<CityPlace> createMockPlaces(String cityKey, String category, String cityName, String type, int count) {
        List<CityPlace> list = new ArrayList<>();
        // Base lat/long for the city (random-ish)
        double baseLat = 10.0 + (cityKey.hashCode() % 1000) / 100.0;
        double baseLon = 106.0 + (cityKey.hashCode() % 1000) / 100.0;

        for (int i = 1; i <= count; i++) {
            String title = category.equals("tourism") ? "Danh thắng " + cityName + " " + i :
                           category.equals("restaurant") ? "Ẩm thực " + cityName + " " + i :
                           "Coffee " + cityName + " " + i;

            String imageUrl = category.equals("tourism") ? "https://images.unsplash.com/photo-1528127269322-539801943592?q=80&w=800&auto=format&fit=crop" :
                              category.equals("restaurant") ? "https://images.unsplash.com/photo-1567037767914-2886a33744f4?q=80&w=800&auto=format&fit=crop" :
                              "https://images.unsplash.com/photo-1507133750040-4a8f5700e53f?q=80&w=800&auto=format&fit=crop";

            list.add(new CityPlace(
                    cityKey,
                    category,
                    title,
                    type,
                    "Địa chỉ tại " + cityName,
                    "Một địa điểm tuyệt vời để trải nghiệm tại " + cityName + ".",
                    4.0 + (i % 10) / 10.0,
                    100 * i,
                    imageUrl, // thumbnail
                    "mock-id-" + cityKey + "-" + category + "-" + i,
                    "08:00 - 22:00",
                    "$$",
                    baseLat + (i * 0.01),
                    baseLon + (i * 0.01)
            ));
        }
        return list;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText("").trim();
    }

    private Double number(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull() || !value.isNumber()) {
            return null;
        }
        return value.asDouble();
    }

    private Integer integer(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull() || !value.isNumber()) {
            return null;
        }
        return value.asInt();
    }

    private boolean contains(String value, String needle) {
        return value != null && !value.isBlank() && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private void appendContextSection(StringBuilder sb, String categoryKey, String label, List<CityPlace> places, List<String> tokens) {
        if (places == null || places.isEmpty()) {
            return;
        }

        List<CityPlace> selected = selectRelevantPlaces(places, tokens, 4);
        if (selected.isEmpty()) {
            selected = places.subList(0, Math.min(4, places.size()));
        }

        sb.append('\n').append(label).append(':').append('\n');
        for (CityPlace place : selected) {
            sb.append("- ")
                    .append(place.title());
            if (place.address() != null && !place.address().isBlank()) {
                sb.append(" | Địa chỉ: ").append(place.address());
            }
            if (place.type() != null && !place.type().isBlank()) {
                sb.append(" | Loại: ").append(place.type());
            }
            if (place.rating() != null) {
                sb.append(" | Rating: ").append(place.rating());
            }
            if (place.reviews() != null) {
                sb.append(" | Reviews: ").append(place.reviews());
            }
            if (place.hours() != null && !place.hours().isBlank()) {
                sb.append(" | Giờ mở cửa: ").append(place.hours());
            }
            if (place.priceRange() != null && !place.priceRange().isBlank()) {
                sb.append(" | Giá: ").append(place.priceRange());
            }
            if (place.description() != null && !place.description().isBlank()) {
                sb.append(" | Mô tả: ").append(place.description());
            }
            sb.append('\n');
        }
    }

    private List<CityPlace> selectRelevantPlaces(List<CityPlace> places, List<String> tokens, int maxItems) {
        if (places == null || places.isEmpty() || maxItems <= 0) {
            return List.of();
        }

        List<ScoredPlace> scored = new ArrayList<>();
        for (CityPlace place : places) {
            int score = scorePlace(place, tokens);
            scored.add(new ScoredPlace(place, score));
        }

        scored.sort(Comparator
                .comparingInt(ScoredPlace::score).reversed()
                .thenComparing(sp -> sp.place().rating(), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(sp -> sp.place().reviews(), Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(sp -> sp.place().title()));

        List<CityPlace> result = new ArrayList<>();
        for (ScoredPlace item : scored) {
            if (result.size() >= maxItems) {
                break;
            }
            result.add(item.place());
        }
        return result;
    }

    private int scorePlace(CityPlace place, List<String> tokens) {
        if (place == null) {
            return 0;
        }

        String haystack = removeDiacritics(
                (place.title() + " " + place.type() + " " + place.address() + " " + place.description())
                        .toLowerCase(Locale.ROOT)
        );

        int score = 0;
        if (tokens != null) {
            for (String token : tokens) {
                if (token.isBlank()) {
                    continue;
                }
                if (haystack.contains(token)) {
                    score += token.length() >= 5 ? 3 : 2;
                }
            }
        }

        if (place.rating() != null) {
            score += Math.min(5, (int) Math.floor(place.rating()));
        }
        if (place.reviews() != null) {
            score += Math.min(3, place.reviews() / 200);
        }
        return score;
    }

    private List<String> extractQueryTokens(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String normalized = removeDiacritics(query).toLowerCase(Locale.ROOT);
        List<String> tokens = new ArrayList<>();
        Matcher matcher = Pattern.compile("[a-z0-9]{3,}").matcher(normalized);
        while (matcher.find()) {
            String token = matcher.group();
            if (!isStopWord(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private boolean isStopWord(String token) {
        Set<String> stopWords = Set.of(
                "ban", "toi", "minh", "cho", "co", "the", "lam", "gi", "o", "tai",
                "khi", "nay", "di", "den", "noi", "nao", "duoc", "khong",
                "moi", "ngay", "ngan", "sach", "gio", "mua", "an", "u", "ve", "voi",
                "mot", "hai", "ba", "bon", "nam", "sau", "bay", "tam", "chin", "muon"
        );
        return stopWords.contains(token);
    }

    private record ScoredPlace(CityPlace place, int score) {
    }

    public record CityPlace(
            String cityKey,
            String category,
            String title,
            String type,
            String address,
            String description,
            Double rating,
            Integer reviews,
            String thumbnail,
            String placeId,
            String hours,
            String priceRange,
            Double latitude,
            Double longitude
    ) {
    }
}