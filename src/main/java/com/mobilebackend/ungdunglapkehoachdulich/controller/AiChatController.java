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
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody ChatRequest req) {
        String reply = aiChatService.chat(req.getMessage(), req.getSessionId(), req.getUserId());
        return new ChatResponse(reply, req.getSessionId());
    }
}
