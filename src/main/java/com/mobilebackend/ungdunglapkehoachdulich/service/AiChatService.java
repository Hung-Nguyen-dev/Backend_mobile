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
/**
 * Service xử lý hội thoại chatbot du lịch.
 * Lớp này chịu trách nhiệm lưu lịch sử chat, tạo ngữ cảnh và gọi mô hình AI.
 */
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

    /** Xử lý toàn bộ một lượt hội thoại của người dùng. */
    public String chat(String userMessage, Integer sessionId, Integer userId) {
        // Tìm hoặc tạo phiên chat để lưu lịch sử hội thoại theo từng người dùng.
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

        // Lưu tin nhắn người dùng trước khi gọi AI để có thể truy vết lại lịch sử.
        chatMessageRepo.save(ChatMessage.builder()
                .chatSessionId(session.getId())
                .sender("user")
                .message(userMessage)
                .createdAt(LocalDateTime.now())
                .build());

        // Tạo ngữ cảnh từ toàn bộ lịch sử hội thoại để AI trả lời đúng mạch.
        List<ChatMessage> history = chatMessageRepo.findByChatSessionIdOrderByCreatedAtAsc(session.getId());
        String demographic = detectDemographic(userMessage, history);
        String city = resolveCity(userMessage, history);

        // Nếu chưa bật Gemini hoặc chưa cấu hình API key thì chuyển sang câu trả lời dự phòng.
        if (!aiProperties.isEnabled() || aiProperties.getApiKey().isBlank()) {
            String fallback = buildFallbackResponse(userMessage, demographic, city);
            chatMessageRepo.save(ChatMessage.builder()
                    .chatSessionId(session.getId())
                    .sender("assistant")
                    .message(fallback)
                    .createdAt(LocalDateTime.now())
                    .build());
            return fallback;
        }

        // Nếu người dùng đang hỏi về du lịch nhưng chưa nêu rõ thành phố, yêu cầu làm rõ.
        if (city == null && isTravelPlanningIntent(userMessage)) {
            String clarify = "Mình có thể gợi ý tốt hơn nếu bạn cho biết thành phố cụ thể (ví dụ: Hà Nội, Đà Nẵng, Đà Lạt). " +
                "Bạn đang muốn đi đâu và đi cùng ai (trẻ em/người lớn tuổi) để mình tư vấn đúng hơn nhé?";
            chatMessageRepo.save(ChatMessage.builder()
                .chatSessionId(session.getId())
                .sender("assistant")
                .message(clarify)
                .createdAt(LocalDateTime.now())
                .build());
            return clarify;
        }

        // Ghép tri thức nội bộ + dữ liệu ngoài để tạo prompt đầy đủ cho Gemini.
        String context = retrieveContext(userMessage, demographic, city);
        String prompt = buildPrompt(context, history, demographic, city);

        // Gọi Gemini để sinh phản hồi tự nhiên theo ngữ cảnh.
        String aiResponse = callGemini(prompt);

        // Nếu Gemini báo hết quota thì dùng dữ liệu dự phòng để không làm gián đoạn trải nghiệm.
        if (isQuotaExceededMessage(aiResponse)) {
            String fallback = buildFallbackResponse(userMessage, demographic, city);
            chatMessageRepo.save(ChatMessage.builder()
                .chatSessionId(session.getId())
                .sender("assistant")
                .message(fallback)
                .createdAt(LocalDateTime.now())
                .build());
            return fallback;
        }

        // Lưu phản hồi của AI để lần sau có thể đọc lại ngữ cảnh.
        chatMessageRepo.save(ChatMessage.builder()
                .chatSessionId(session.getId())
                .sender("assistant")
                .message(aiResponse)
                .createdAt(LocalDateTime.now())
                .build());

        return aiResponse;
    }

    /** Tạo câu trả lời dự phòng từ dữ liệu địa điểm nội bộ khi AI không sẵn sàng. */
    private String buildFallbackResponse(String userMessage, String demographic, String city) {
        if (city == null) {
            return "Mình chưa nhận diện được thành phố cụ thể. Bạn cho mình biết muốn đi đâu (ví dụ: Hà Nội, Đà Nẵng, Đà Lạt) và đi cùng ai nhé.";
        }

        String category = detectCategory(userMessage);
        List<CityPlace> places = cityDataService.getPlaces(city, category.toLowerCase());
        if (places.isEmpty()) {
            places = cityDataService.getPlaces(city, "tourism");
        }

        if (places.isEmpty()) {
            return "Mình có thông tin về " + city + " nhưng chưa đủ dữ liệu địa điểm để gợi ý chắc chắn lúc này. Bạn muốn mình gợi ý theo kiểu nhẹ nhàng, có trẻ em hay người lớn tuổi để mình lọc tiếp nhé?";
        }

        StringBuilder sb = new StringBuilder();
        if ("CHILD".equals(demographic)) {
            sb.append("Nếu đi cùng trẻ em ở ").append(city).append(", bạn có thể ưu tiên các điểm an toàn, ít phải đi bộ xa, có không gian thoáng.\n");
        } else if ("ELDERLY".equals(demographic)) {
            sb.append("Nếu đi cùng người lớn tuổi ở ").append(city).append(", bạn nên chọn lịch nhẹ, ít bậc thang, có chỗ nghỉ và di chuyển thuận tiện.\n");
        } else {
            sb.append("Nếu đi ").append(city).append(", đây là vài gợi ý phù hợp cho lịch nhẹ nhàng:\n");
        }

        int limit = Math.min(3, places.size());
        for (int i = 0; i < limit; i++) {
            CityPlace place = places.get(i);
            sb.append(i + 1).append(". ").append(place.title());
            if (place.address() != null && !place.address().isBlank()) {
                sb.append(" - ").append(place.address());
            }
            if (place.description() != null && !place.description().isBlank()) {
                sb.append(". ").append(place.description());
            }
            sb.append("\n");
        }

        sb.append("\nNếu bạn muốn, mình có thể gợi ý lịch 1 ngày hoặc 2 ngày riêng cho trẻ em hoặc người lớn tuổi ở ")
                .append(city)
                .append(".");
        return sb.toString();
    }

    /** Tạo ngữ cảnh dữ liệu thực tế cho prompt, kết hợp nguồn nội bộ và SerpAPI. */
    private String retrieveContext(String query, String demographic, String city) {
        if (city == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("Dưới đây là tri thức từ cơ sở dữ liệu về ").append(city).append(":\n");

        String category = detectCategory(query);

        // Hybrid Data Collection (Local + SerpApi)
        List<CityPlace> localPlaces = cityDataService.getPlaces(city, category.toLowerCase());
        if (localPlaces.isEmpty()) localPlaces = cityDataService.getPlaces(city, "tourism");
        
        String serpLabel = "địa điểm du lịch";
        if ("food".equals(category)) serpLabel = "nhà hàng";
        else if ("cafe".equals(category)) serpLabel = "quán cafe";

        if ("CHILD".equals(demographic)) serpLabel += " phù hợp trẻ em";
        else if ("ELDERLY".equals(demographic)) serpLabel += " cho người cao tuổi";

        List<PlaceNearRes> externalPlaces = new ArrayList<>();
        try {
            log.info("Searching SerpApi for: {} in {}", serpLabel, city);
            externalPlaces = placesSearchService.searchPlacesByCategory(city, serpLabel, 5);
        } catch (Exception e) {
            log.warn("Lỗi khi lấy dữ liệu SerpApi cho RAG: {}", e.getMessage());
        }

        // De-duplication and Context Building
        Set<String> seenNames = new HashSet<>();
        
        // 1. Add Local Places
        if (!localPlaces.isEmpty()) {
            sb.append("- Địa điểm từ hệ thống:\n");
            localPlaces.stream().limit(5).forEach(p -> {
                String name = p.title().toLowerCase().trim();
                if (seenNames.add(name)) {
                    sb.append("  • ").append(p.title()).append(" | Địa chỉ: ").append(p.address() != null ? p.address() : "Thông tin đang cập nhật");
                    sb.append(" | Mô tả: ").append(p.description());
                    if (p.rating() != null) sb.append(" | Rating: ").append(p.rating());
                    if (p.thumbnail() != null) sb.append(" | Image: ").append(p.thumbnail());
                    sb.append("\n");
                }
            });
        }

        // 2. Add External Places (only if not seen before)
        if (!externalPlaces.isEmpty()) {
            sb.append("- Địa điểm cập nhật từ Google Maps (").append(serpLabel).append("):\n");
            externalPlaces.forEach(p -> {
                String name = p.getName().toLowerCase().trim();
                // Basic de-duplication: check if name is already present
                boolean duplicate = seenNames.stream().anyMatch(seen -> seen.contains(name) || name.contains(seen));
                if (!duplicate) {
                    seenNames.add(name);
                    sb.append("  • ").append(p.getName()).append(" | Địa chỉ: ").append(p.getAddressLine() != null ? p.getAddressLine() : "Khu vực " + city);
                    sb.append(" | Mô tả: ").append(p.getDescription() != null ? p.getDescription() : "Nổi tiếng");
                    if (p.getRating() != null) sb.append(" | Rating: ").append(p.getRating());
                    if (p.getPreviewImageUrl() != null) sb.append(" | Image: ").append(p.getPreviewImageUrl());
                    sb.append("\n");
                }
            });
        }

        return sb.toString();
    }

    /** Xác định loại nhu cầu chính của người dùng để chọn dữ liệu phù hợp. */
    private String detectCategory(String query) {
        String q = query.toLowerCase();
        if (containsAny(q, "ăn", "nhà hàng", "quán ăn", "đặc sản", "ngon", "lunch", "dinner", "món")) return "food";
        if (containsAny(q, "cafe", "cà phê", "uống", "chill", "view hồ", "trà")) return "cafe";
        return "tourism";
    }

    /** Nhận diện nhóm người dùng để điều chỉnh gợi ý cho phù hợp. */
    private String detectDemographic(String query) {
        String q = query.toLowerCase();
        if (containsAny(q, "trẻ em", "bé", "con nhỏ", "nhi đồng", "trẻ nhỏ", "con nít")) return "CHILD";
        if (containsAny(q, "người già", "người lớn tuổi", "cao tuổi", "ông bà", "bố mẹ già", "người cao tuổi")) return "ELDERLY";
        return "GENERAL";
    }

    /** Ưu tiên suy luận từ lịch sử hội thoại nếu câu hỏi hiện tại chưa nêu rõ nhóm người dùng. */
    private String detectDemographic(String query, List<ChatMessage> history) {
        String direct = detectDemographic(query);
        if (!"GENERAL".equals(direct)) return direct;

        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage msg = history.get(i);
            if (!"user".equalsIgnoreCase(msg.getSender())) continue;
            String inferred = detectDemographic(msg.getMessage());
            if (!"GENERAL".equals(inferred)) return inferred;
        }
        return "GENERAL";
    }

    /** Kiểm tra xem chuỗi có chứa bất kỳ từ khóa nào trong danh sách hay không. */
    private boolean containsAny(String text, String... keywords) {
        for (String k : keywords) {
            if (text.contains(k)) return true;
        }
        return false;
    }

    /** Tìm tên thành phố xuất hiện trong câu hỏi của người dùng. */
    private String detectCity(String query) {
        String q = query.toLowerCase();
        
        // 1. Check hardcoded special cities first
        for (String city : CITIES) {
            if (q.contains(city.toLowerCase())) return city;
        }
        
        // 2. Try to extract city name after keywords
        Pattern pattern = Pattern.compile("(?:ở|tại|đến|đi|vùng|thành phố|tỉnh)\\s+([A-ZÀ-ỹ][a-zà-ỹ]+(?:\\s+[A-ZÀ-ỹ][a-zà-ỹ]+)*)");
        var matcher = pattern.matcher(query);
        if (matcher.find()) {
            String found = matcher.group(1);
            // Basic validation: at least 2 chars and not a common word
            if (found.length() > 2) return found;
        }
        
        return null;
    }

    /** Suy luận thành phố từ câu hiện tại hoặc từ lịch sử hội thoại gần nhất. */
    private String resolveCity(String userMessage, List<ChatMessage> history) {
        String city = detectCity(userMessage);
        if (city != null) return city;

        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage msg = history.get(i);
            if (!"user".equalsIgnoreCase(msg.getSender())) continue;
            String inferred = detectCity(msg.getMessage());
            if (inferred != null) return inferred;
        }
        return null;
    }

    /** Phát hiện xem người dùng có đang nói về kế hoạch du lịch hay không. */
    private boolean isTravelPlanningIntent(String query) {
        String q = query.toLowerCase();
        return containsAny(q,
                "đi đâu", "ăn gì", "chơi gì", "du lịch", "lịch trình", "gợi ý", "gợi ý giúp",
                "địa điểm", "thăm quan", "tham quan", "review chỗ", "quán", "nhà hàng", "cafe");
    }

    /** Ghép prompt cuối cùng gửi cho Gemini, có kèm ngữ cảnh và quy tắc trả lời. */
    private String buildPrompt(String context, List<ChatMessage> history, String demographic, String city) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là trợ lý du lịch thông minh thiết kế riêng cho người Việt Nam.\n");
        sb.append("Bạn thân thiện, hữu ích và luôn cố gắng cung cấp thông tin chính xác.\n");
        sb.append("Nếu thiếu dữ liệu chắc chắn thì nói rõ giới hạn và hỏi lại 1 câu làm rõ, không được bịa thông tin.\n");

        if (city != null) {
            sb.append("BỐI CẢNH THÀNH PHỐ HIỆN TẠI: ").append(city).append(".\n");
        }

        if ("CHILD".equals(demographic)) {
            sb.append("ĐƯỢC LƯU Ý: Người dùng đang đi cùng TRẺ EM. Hãy ưu tiên các địa điểm an toàn, có khu vui chơi, ít đi bộ xa và thân thiện với trẻ nhỏ.\n");
        } else if ("ELDERLY".equals(demographic)) {
            sb.append("ĐƯỢC LƯU Ý: Người dùng đang đi cùng NGƯỜI LỚN TUỔI. Hãy ưu tiên các địa điểm dễ di chuyển (ít bậc thang), không khí thoáng đãng, yên tĩnh và có chỗ nghỉ ngơi.\n");
        }
        
        if (!context.isEmpty()) {
            sb.append("\nKNOWLEDGE CONTEXT (Thông tin thực tế):\n").append(context).append("\n");
            sb.append("Hãy sử dụng thông tin này để gợi ý các địa điểm có thật và mô tả chính xác.\n");
        } else {
            sb.append("\nHiện chưa có knowledge context đáng tin cậy từ hệ thống.\n");
            sb.append("Không được tạo [PLACE: ...] với địa chỉ cụ thể nếu không chắc chắn.\n");
        }

        sb.append("\nQUY TẮC HIỂN THỊ ĐỊA ĐIỂM (CỰC KỲ QUAN TRỌNG):\n");
        sb.append("- Khi gợi ý một địa điểm (dù là từ ngữ cảnh trên hay từ kiến thức của bạn), hãy dùng cú pháp sau để hiển thị thẻ đẹp mắt:\n");
        sb.append("  [PLACE: Tên địa điểm | URL ảnh | Số sao | Địa chỉ | Mô tả ngắn]\n");
        sb.append("- Trong đó:\n");
        sb.append("  • URL ảnh: Lấy từ 'Image' trong context (nếu có), nếu không có ảnh hãy để trống.\n");
        sb.append("  • Số sao: Lấy từ 'Rating' trong context (nếu có), nếu không có hãy để '4.5'.\n");
        sb.append("  • Địa chỉ: Lấy từ 'Địa chỉ' trong context. KHÔNG ĐƯỢC BỎ TRỐNG TRƯỜNG NÀY.\n");
        sb.append("- Nếu không có địa chỉ đáng tin cậy thì KHÔNG được xuất [PLACE: ...], thay vào đó đề xuất theo khu vực và hỏi thêm thông tin.\n");
        sb.append("- Luôn đan xen các điểm tham quan với các quán cafe và nhà hàng dựa trên lịch trình người dùng yêu cầu.\n");
        sb.append("- TRẢ LỜI TRỰC TIẾP: Nếu người dùng hỏi 'đi đâu', 'ăn gì' ở một thành phố cụ thể, hãy cung cấp danh sách gợi ý ngay lập tức dựa trên context, đừng chỉ ghi nhận thông tin.\n");
        sb.append("- TRÌNH BÀY: Phản hồi bằng tiếng Việt thân thiện. Nếu có nhiều địa điểm, hãy đánh số thứ tự hoặc chia theo buổi.\n");

        sb.append("\nLỊCH SỬ HỘI THOẠI:\n");
        int start = Math.max(0, history.size() - 12);
        for (int i = start; i < history.size(); i++) {
            ChatMessage msg = history.get(i);
            String role = "user".equals(msg.getSender()) ? "User" : "Assistant";
            sb.append(role).append(": ").append(msg.getMessage()).append("\n");
        }
        
        sb.append("\nAssistant: ");
        return sb.toString();
    }

    /** Gọi API Gemini và đọc câu trả lời sinh ra từ mô hình. */
    private String callGemini(String prompt) {
        int maxRetries = 3;
        int retryDelayMs = 1500;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
            String baseUrl = trimTrailingSlash(aiProperties.getBaseUrl());
            String model = aiProperties.getModel() == null || aiProperties.getModel().isBlank()
                ? "gemini-2.5-flash"
                : aiProperties.getModel().trim();
            String url = baseUrl + "/models/" + model + ":generateContent?key=" + aiProperties.getApiKey();
                
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
                    JsonNode candidates = root.path("candidates");
                    if (candidates.isArray() && !candidates.isEmpty()) {
                        JsonNode firstCandidate = candidates.get(0);
                        JsonNode parts = firstCandidate.path("content").path("parts");
                        if (parts.isArray() && !parts.isEmpty()) {
                            String aiText = parts.get(0).path("text").asText();
                            if (aiText != null && !aiText.isBlank()) {
                                return aiText;
                            }
                        }
                    }
                    
                    log.warn("Gemini blocked response or returned no text: {}", response.body());
                    return "Mình chưa thể tạo phản hồi rõ ràng từ AI lúc này. Bạn có thể nói rõ hơn điểm đến, số ngày, và bạn đi cùng ai để mình tư vấn chính xác hơn.";
                } else if (response.statusCode() == 429 && isQuotaExceededMessage(response.body())) {
                    log.warn("Gemini quota exhausted: {}", response.body());
                    return "QUOTA_EXCEEDED: Gemini quota exhausted";
                } else if ((response.statusCode() == 503 || response.statusCode() == 429) && attempt < maxRetries) {
                    log.warn("Gemini API Busy ({}). Attempt {}/{} - Retrying in {}ms...", 
                            response.statusCode(), attempt, maxRetries, retryDelayMs);
                    Thread.sleep(retryDelayMs);
                    retryDelayMs *= 2; // Exponential backoff
                } else {
                    log.error("Gemini API Error: {} - {}", response.statusCode(), response.body());
                    if (response.statusCode() == 429) {
                        return "QUOTA_EXCEEDED: Gemini quota exhausted";
                    }
                    return "Lỗi AI (HTTP " + response.statusCode() + "): " + response.body();
                }
            } catch (Exception e) {
                log.error("Exception calling Gemini (Attempt {}): ", attempt, e);
                if (attempt == maxRetries) {
                    return "Xin lỗi, AI đang gặp trục trặc kỹ thuật kết nối. Chi tiết: " + e.getMessage();
                }
                try { Thread.sleep(retryDelayMs); } catch (InterruptedException ignored) {}
            }
        }
        return "Xin lỗi, hệ thống AI hiện đang quá tải. Vui lòng thử lại sau giây lát.";
    }

    /** Bỏ dấu gạch chéo cuối của URL để nối endpoint cho đúng. */
    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "https://generativelanguage.googleapis.com/v1";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    /** Nhận diện thông báo lỗi quota phổ biến từ Gemini. */
    private boolean isQuotaExceededMessage(String message) {
        if (message == null) return false;
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("quota exhausted")
                || normalized.contains("resource_exhausted")
                || normalized.contains("quota exceeded")
                || normalized.contains("generate_content_free_tier")
                || normalized.contains("limit: 0");
    }
}
