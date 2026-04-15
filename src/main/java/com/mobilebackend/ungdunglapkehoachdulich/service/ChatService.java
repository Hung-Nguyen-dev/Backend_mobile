package com.mobilebackend.ungdunglapkehoachdulich.service;

import com.mobilebackend.ungdunglapkehoachdulich.dto.chat.ChatDtos.*;
import com.mobilebackend.ungdunglapkehoachdulich.model.ChatMessage;
import com.mobilebackend.ungdunglapkehoachdulich.model.ChatSession;
import com.mobilebackend.ungdunglapkehoachdulich.repo.ChatMessageRepo;
import com.mobilebackend.ungdunglapkehoachdulich.repo.ChatSessionRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepo chatSessionRepo;
    private final ChatMessageRepo chatMessageRepo;
    private final GeminiService geminiService;
    private final CityDataService cityDataService;

    @Transactional
    public ChatResponse processChat(ChatRequest req) {
        // 1. Tìm hoặc tạo Session
        ChatSession session;
        if (req.getSessionId() != null) {
            session = chatSessionRepo.findByIdAndUserId(req.getSessionId(), req.getUserId())
                    .orElseGet(() -> createNewSession(req.getUserId()));
        } else {
            session = createNewSession(req.getUserId());
        }

        // 2. Lưu tin nhắn người dùng
        ChatMessage userMsg = ChatMessage.builder()
                .chatSessionId(session.getId())
                .sender("user")
                .message(req.getMessage())
                .createdAt(LocalDateTime.now())
                .build();
        chatMessageRepo.save(userMsg);

        TravelProfile profile = detectTravelProfile(req.getMessage());

        String clarificationQuestion = buildClarificationQuestion(req.getMessage(), profile);
        if (!clarificationQuestion.isEmpty()) {
            ChatMessage aiClarifyMsg = ChatMessage.builder()
                .chatSessionId(session.getId())
                .sender("ai")
                .message(clarificationQuestion)
                .createdAt(LocalDateTime.now())
                .build();
            chatMessageRepo.save(aiClarifyMsg);

            return ChatResponse.builder()
                .sessionId(session.getId())
                .reply(clarificationQuestion)
                .status("need_more_info")
                .build();
        }

        // 3. Chuẩn bị ngữ cảnh (RAG + Context)
        String contextData = "";
        if (req.getCurrentCity() != null && !req.getCurrentCity().isEmpty()) {
            contextData = cityDataService.getCityContext(req.getCurrentCity(), req.getMessage());
        }

        // Lấy 10 tin nhắn gần nhất để AI có ngữ cảnh
        List<ChatMessage> history = chatMessageRepo.findByChatSessionIdOrderByCreatedAtAsc(session.getId());
        List<ChatMessage> recentHistory = history.size() > 10 ? history.subList(history.size() - 10, history.size()) : history;
        String historyStr = recentHistory.stream()
                .map(m -> (m.getSender().equals("user") ? "Người dùng: " : "AI: ") + m.getMessage())
                .collect(Collectors.joining("\n"));

        // 4. Xây dựng Prompt cho Gemini
        String systemPrompt = "Bạn là một chuyên gia tư vấn du lịch Việt Nam chuyên nghiệp, am hiểu sâu sắc về địa phương và vô cùng thân thiện. " +
                "Nhiệm vụ của bạn là hỗ trợ người dùng lên kế hoạch chuyến đi, giải đáp thắc mắc về các địa điểm dựa trên dữ liệu thực tế được cung cấp.\n\n" +
                "QUY TẮC QUAN TRỌNG:\n" +
                "1. Trả lời một cách tự nhiên, chân thực và hữu ích bằng tiếng Việt.\n" +
                "2. TUYỆT ĐỐI KHÔNG trả về định dạng JSON thô. Người dùng không phải là lập trình viên.\n" +
                "3. Khi lên lịch trình hoặc gợi ý danh sách, hãy sử dụng định dạng Markdown (tiêu đề, danh sách có dấu gạch đầu dòng, in đậm) để thông tin rõ ràng và bắt mắt.\n" +
                "4. Nếu dữ liệu thực tế có thông tin về đánh giá (rating), hãy đề cập để tăng độ tin cậy.\n" +
                (profile.budgetTier().isEmpty() ? "" : "5. Ngân sách người dùng đã được nhận diện là: " + budgetLabelVi(profile.budgetTier()) + ". Hãy ưu tiên các địa điểm phù hợp với mức ngân sách này và chọn nơi có rating tốt trong nhóm địa điểm phù hợp.\n") +
                (profile.hasChildren() ? "6. Người dùng đi cùng trẻ em: ưu tiên điểm an toàn, có hoạt động nhẹ, lịch trình không quá dày và hạn chế đi trễ.\n" : "") +
                (profile.hasElderly() ? "7. Người dùng đi cùng người cao tuổi: ưu tiên điểm ít đi bộ, có chỗ nghỉ, tránh di chuyển liên tục và hạn chế leo dốc/bậc thang.\n" : "") +
                (profile.accessibilityNeeded() ? "8. Người dùng có nhu cầu tiếp cận dễ dàng: ưu tiên nơi dễ vào, ít bậc thang, hạ tầng thuận tiện.\n" : "") +
                "9. Luôn kết thúc bằng một câu gợi ý mở hoặc lời chúc tốt đẹp.\n\n" +
                (contextData.isEmpty() ? "" : "DỮ LIỆU ĐỊA PHƯƠNG THỰC TẾ (RAG) ĐỂ BẠN THAM KHẢO:\n" + contextData + "\n\n") +
                profileSummaryLine(profile) +
                "LỊCH SỬ HỘI THOẠI:\n" + historyStr + "\n\n" +
                "CÂU HỎI MỚI NHẤT CỦA NGƯỜI DÙNG: " + req.getMessage();

        // 5. Gọi AI và lưu phản hồi
        String aiReply = geminiService.askGemini(systemPrompt);
        String replyStatus = "success";
        if (aiReply == null || aiReply.isEmpty()) {
            aiReply = "Xin lỗi, mình đang gặp một chút trục rặc khi kết nối với bộ não AI. Bạn thử lại sau ít phút nhé!";
            replyStatus = "ai_unavailable";
        }

        ChatMessage aiMsg = ChatMessage.builder()
                .chatSessionId(session.getId())
                .sender("ai")
                .message(aiReply)
                .createdAt(LocalDateTime.now())
                .build();
        chatMessageRepo.save(aiMsg);

        return ChatResponse.builder()
                .sessionId(session.getId())
                .reply(aiReply)
            .status(replyStatus)
                .build();
    }

    private ChatSession createNewSession(Integer userId) {
        ChatSession session = ChatSession.builder()
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .build();
        return chatSessionRepo.save(session);
    }

    private TravelProfile detectTravelProfile(String message) {
        if (message == null || message.isBlank()) {
            return new TravelProfile("", false, false, false);
        }

        String normalized = removeDiacritics(message).toLowerCase(Locale.ROOT);
        String budgetTier = detectBudgetTier(normalized);
        boolean hasChildren = containsAny(normalized,
                "tre em", "con nho", "em be", "be nho", "thieu nhi", "kid", "kids", "children", "child", "gia dinh co be"
        );
        boolean hasElderly = containsAny(normalized,
                "nguoi gia", "cao tuoi", "ong ba", "bo me gia", "senior", "elderly", "grandparent", "dad me lon tuoi"
        );
        boolean accessibilityNeeded = containsAny(normalized,
                "xe lan", "it bac", "de di", "han che di bo", "khong leo", "accessible", "accessibility", "wheelchair", "khong bac thang"
        );

        return new TravelProfile(budgetTier, hasChildren, hasElderly, accessibilityNeeded);
    }

    private String detectBudgetTier(String normalizedMessage) {
        if (containsAny(normalizedMessage, "tiet kiem", "gia re", "binh dan", "low budget", "toi uu chi phi", "chi phi thap")) {
            return "low";
        }
        if (containsAny(normalizedMessage, "thoai mai", "sang trong", "cao cap", "high budget", "luxury", "co the chi manh")) {
            return "high";
        }
        if (containsAny(normalizedMessage, "vua phai", "trung binh", "medium", "binh thuong", "can bang chi phi")) {
            return "medium";
        }
        return "";
    }

    private String buildClarificationQuestion(String message, TravelProfile profile) {
        if (!isPlanningIntent(message)) {
            return "";
        }

        boolean missingBudget = profile.budgetTier().isEmpty();
        boolean missingCompanionInfo = !profile.hasChildren() && !profile.hasElderly();

        if (!missingBudget && !missingCompanionInfo) {
            return "";
        }

        if (missingBudget && missingCompanionInfo) {
            return "Mình có thể lên lịch trình sát nhu cầu hơn nếu bạn cho mình 2 thông tin nhanh:\n"
                    + "1. Ngân sách: tiết kiệm, vừa phải hay thoải mái?\n"
                    + "2. Nhóm đi cùng có trẻ em hoặc người cao tuổi không?";
        }

        if (missingBudget) {
            return "Để gợi ý chính xác hơn, bạn cho mình biết ngân sách mong muốn: tiết kiệm, vừa phải hay thoải mái nhé?";
        }

        return "Bạn đi cùng nhóm nào để mình tối ưu lịch trình hơn: có trẻ em hoặc người cao tuổi không?";
    }

    private boolean isPlanningIntent(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = removeDiacritics(message).toLowerCase(Locale.ROOT);
        return containsAny(normalized,
                "lich trinh", "ke hoach", "goi y", "du lich", "di choi", "schedule", "plan trip", "itinerary"
        );
    }

    private String profileSummaryLine(TravelProfile profile) {
        List<String> tags = new java.util.ArrayList<>();
        if (!profile.budgetTier().isEmpty()) {
            tags.add("ngân sách=" + budgetLabelVi(profile.budgetTier()));
        }
        if (profile.hasChildren()) {
            tags.add("có trẻ em");
        }
        if (profile.hasElderly()) {
            tags.add("có người cao tuổi");
        }
        if (profile.accessibilityNeeded()) {
            tags.add("cần tiếp cận dễ dàng");
        }
        if (tags.isEmpty()) {
            return "";
        }
        return "HỒ SƠ NHU CẦU ĐÃ NHẬN DIỆN: " + String.join(", ", tags) + "\n\n";
    }

    private String budgetLabelVi(String budgetTier) {
        if ("low".equalsIgnoreCase(budgetTier)) {
            return "tiết kiệm";
        }
        if ("high".equalsIgnoreCase(budgetTier)) {
            return "thoải mái";
        }
        if ("medium".equalsIgnoreCase(budgetTier)) {
            return "vừa phải";
        }
        return budgetTier;
    }

    private String removeDiacritics(String str) {
        if (str == null) {
            return "";
        }
        String nfdNormalizedString = java.text.Normalizer.normalize(str, java.text.Normalizer.Form.NFD);
        return nfdNormalizedString
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replace('đ', 'd')
                .replace('Đ', 'D');
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null || keywords == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private record TravelProfile(String budgetTier, boolean hasChildren, boolean hasElderly, boolean accessibilityNeeded) {
    }
}
