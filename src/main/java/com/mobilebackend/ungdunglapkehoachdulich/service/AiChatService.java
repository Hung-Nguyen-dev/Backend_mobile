package com.mobilebackend.ungdunglapkehoachdulich.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mobilebackend.ungdunglapkehoachdulich.config.GoogleAiProperties;
import com.mobilebackend.ungdunglapkehoachdulich.model.ChatMessage;
import com.mobilebackend.ungdunglapkehoachdulich.model.ChatSession;
import com.mobilebackend.ungdunglapkehoachdulich.repo.ChatMessageRepo;
import com.mobilebackend.ungdunglapkehoachdulich.repo.ChatSessionRepo;
import com.mobilebackend.ungdunglapkehoachdulich.service.CityDataService.CityPlace;
import com.mobilebackend.ungdunglapkehoachdulich.dto.places.PlaceNearRes;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiChatService {
    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private final GoogleAiProperties aiProperties;
    private final CityDataService cityDataService;
    private final PlacesSearchService placesSearchService;
    private final ChatSessionRepo chatSessionRepo;
    private final ChatMessageRepo chatMessageRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final List<String> CITIES = List.of(
            "Hà Nội", "TP.HCM", "Đà Nẵng", "Nha Trang", "Đà Lạt", "Huế", "Phú Quốc", 
            "Ninh Bình", "Hạ Long", "Thanh Hóa", "Cao Bằng", "Hà Giang"
    );

    public String chat(String userMessage, Integer sessionId, Integer userId) {
        if (!aiProperties.isEnabled() || aiProperties.getApiKey().isBlank()) {
            return "Hệ thống AI hiện đang tạm ngưng. Vui lòng thử lại sau.";
        }

        // 1. Handle Session
        ChatSession session = null;
        if (sessionId != null) {
            session = chatSessionRepo.findBySessionId(sessionId).orElse(null);
        }
        if (session == null) {
            session = chatSessionRepo.save(ChatSession.builder()
                    .sessionId(sessionId != null ? sessionId : new Random().nextInt(1000000))
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build());
        }

        // 2. Save User Message
        chatMessageRepo.save(ChatMessage.builder()
                .chatSessionId(session.getId())
                .sender("user")
                .message(userMessage)
                .createdAt(LocalDateTime.now())
                .build());

        // 3. Retrieve Context (Hybrid RAG)
        String demographic = detectDemographic(userMessage);
        String context = retrieveContext(userMessage, demographic);

        // 4. Build Prompt with History
        List<ChatMessage> history = chatMessageRepo.findByChatSessionIdOrderByCreatedAtAsc(session.getId());
        String prompt = buildPrompt(context, history, demographic);

        // 5. Call Gemini
        String aiResponse = callGemini(prompt);

        // 6. Save AI Response
        chatMessageRepo.save(ChatMessage.builder()
                .chatSessionId(session.getId())
                .sender("assistant")
                .message(aiResponse)
                .createdAt(LocalDateTime.now())
                .build());

        return aiResponse;
    }

    private String retrieveContext(String query, String demographic) {
        String city = detectCity(query);
        if (city == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("Dưới đây là tri thức từ cơ sở dữ liệu về ").append(city).append(":\n");

        String category = detectCategory(query);

        // Local Data (Always pull tourism as baseline)
        List<CityPlace> localPlaces = cityDataService.getPlaces(city, category.toLowerCase());
        if (localPlaces.isEmpty()) localPlaces = cityDataService.getPlaces(city, "tourism");
        
        if (!localPlaces.isEmpty()) {
            sb.append("- Địa điểm từ hệ thống:\n");
            localPlaces.stream().limit(5).forEach(p -> {
                sb.append("  • ").append(p.title()).append(": ").append(p.description());
                if (p.rating() != null) sb.append(" | Rating: ").append(p.rating());
                if (p.thumbnail() != null) sb.append(" | Image: ").append(p.thumbnail());
                sb.append("\n");
            });
        }

        // Real-time Data (SerpApi)
        try {
            String serpLabel = "địa điểm du lịch";
            if ("food".equals(category)) serpLabel = "nhà hàng";
            else if ("cafe".equals(category)) serpLabel = "quán cafe";

            // Add demographic modifier
            if ("CHILD".equals(demographic)) serpLabel += " phù hợp trẻ em";
            else if ("ELDERLY".equals(demographic)) serpLabel += " cho người cao tuổi";

            List<PlaceNearRes> t = placesSearchService.searchPlacesByCategory(city, serpLabel, 5);
            if (!t.isEmpty()) {
                sb.append("- Địa điểm cập nhật từ Google Maps (").append(serpLabel).append("):\n");
                t.forEach(p -> {
                    sb.append("  • ").append(p.getName()).append(": ").append(p.getDescription() != null ? p.getDescription() : "Nổi tiếng");
                    if (p.getRating() != null) sb.append(" | Rating: ").append(p.getRating());
                    if (p.getPreviewImageUrl() != null) sb.append(" | Image: ").append(p.getPreviewImageUrl());
                    sb.append("\n");
                });
            }
        } catch (Exception e) {
            log.warn("Lỗi khi lấy dữ liệu SerpApi cho RAG: {}", e.getMessage());
        }

        return sb.toString();
    }

    private String detectCategory(String query) {
        String q = query.toLowerCase();
        if (containsAny(q, "ăn", "nhà hàng", "quán ăn", "đặc sản", "ngon", "lunch", "dinner", "món")) return "food";
        if (containsAny(q, "cafe", "cà phê", "uống", "chill", "view hồ", "trà")) return "cafe";
        return "tourism";
    }

    private String detectDemographic(String query) {
        String q = query.toLowerCase();
        if (containsAny(q, "trẻ em", "bé", "con nhỏ", "nhi đồng", "trẻ nhỏ", "con nít")) return "CHILD";
        if (containsAny(q, "người già", "người lớn tuổi", "cao tuổi", "ông bà", "bố mẹ già", "người cao tuổi")) return "ELDERLY";
        return "GENERAL";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String k : keywords) {
            if (text.contains(k)) return true;
        }
        return false;
    }

    private String detectCity(String query) {
        String q = query.toLowerCase();
        for (String city : CITIES) {
            if (q.contains(city.toLowerCase())) return city;
        }
        return null;
    }

    private String buildPrompt(String context, List<ChatMessage> history, String demographic) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là trợ lý du lịch thông minh thiết kế riêng cho người Việt Nam.\n");
        sb.append("Bạn thân thiện, hữu ích và luôn cố gắng cung cấp thông tin chính xác.\n");

        if ("CHILD".equals(demographic)) {
            sb.append("ĐƯỢC LƯU Ý: Người dùng đang đi cùng TRẺ EM. Hãy ưu tiên các địa điểm an toàn, có khu vui chơi, ít đi bộ xa và thân thiện với trẻ nhỏ.\n");
        } else if ("ELDERLY".equals(demographic)) {
            sb.append("ĐƯỢC LƯU Ý: Người dùng đang đi cùng NGƯỜI LỚN TUỔI. Hãy ưu tiên các địa điểm dễ di chuyển (ít bậc thang), không khí thoáng đãng, yên tĩnh và có chỗ nghỉ ngơi.\n");
        }
        
        if (!context.isEmpty()) {
            sb.append("\nKNOWLEDGE CONTEXT:\n").append(context).append("\n");
            sb.append("Hãy sử dụng thông tin trên để trả lời chính xác và chuyên sâu hơn.\n");
        sb.append("QUY TẮC HIỂN THỊ ĐỊA ĐIỂM:\n");
        sb.append("- Khi gợi ý một địa điểm có sẵn 'Image' và 'Rating' trong ngữ cảnh, hãy dùng cú pháp sau để hiển thị thẻ đẹp mắt:\n");
        sb.append("  [PLACE: Tên địa điểm | URL ảnh | Số sao | Mô tả ngắn]\n");
        sb.append("- Nếu không có ảnh, hãy để trống phần URL ảnh.\n");
        sb.append("- Luôn đan xen các điểm tham quan với các quán cafe và nhà hàng dựa trên lịch trình người dùng yêu cầu.\n");
        sb.append("- Nếu người dùng yêu cầu 'lịch trình x ngày', hãy trình bày rõ ràng theo từng ngày (Ngày 1, Ngày 2...).\n");
        }

        sb.append("\nLỊCH SỬ HỘI THOẠI:\n");
        for (ChatMessage msg : history) {
            String role = "user".equals(msg.getSender()) ? "User" : "Assistant";
            sb.append(role).append(": ").append(msg.getMessage()).append("\n");
        }
        
        sb.append("\nAssistant: ");
        return sb.toString();
    }

    private String callGemini(String prompt) {
        try {
            String url = aiProperties.getBaseUrl() + "?key=" + aiProperties.getApiKey();
            
            Map<String, Object> part = Map.of("text", prompt);
            Map<String, Object> content = Map.of("parts", List.of(part));
            Map<String, Object> bodyMap = Map.of("contents", List.of(content));
            
            String jsonBody = objectMapper.writeValueAsString(bodyMap);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                return root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            } else {
                log.error("Gemini API Error: {} - {}", response.statusCode(), response.body());
                return "Xin lỗi, AI đang gặp trục trặc kỹ thuật. Vui lòng thử lại sau.";
            }
        } catch (Exception e) {
            log.error("Exception calling Gemini: ", e);
            return "Đã xảy ra lỗi khi kết nối với AI.";
        }
    }
}
