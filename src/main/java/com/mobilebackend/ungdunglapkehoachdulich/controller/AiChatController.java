package com.mobilebackend.ungdunglapkehoachdulich.controller;

import com.mobilebackend.ungdunglapkehoachdulich.dto.ai.AiChatDtos.ChatRequest;
import com.mobilebackend.ungdunglapkehoachdulich.dto.ai.AiChatDtos.ChatResponse;
import com.mobilebackend.ungdunglapkehoachdulich.service.AiChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
/**
 * Controller cho chatbot hội thoại tự do.
 * API ở đây dùng để người dùng hỏi đáp tự nhiên, còn logic xử lý nằm trong AiChatService.
 */
public class AiChatController {

    private final AiChatService aiChatService;

    /** Nhận câu hỏi của người dùng, gọi service sinh phản hồi và trả về kèm sessionId. */
    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest req) {
        String reply = aiChatService.chat(req.getMessage(), req.getSessionId(), req.getUserId());
        return new ChatResponse(reply, req.getSessionId());
    }
}
