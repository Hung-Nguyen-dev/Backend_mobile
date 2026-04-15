package com.mobilebackend.ungdunglapkehoachdulich.controller;

import com.mobilebackend.ungdunglapkehoachdulich.dto.chat.ChatDtos.ChatRequest;
import com.mobilebackend.ungdunglapkehoachdulich.dto.chat.ChatDtos.ChatResponse;
import com.mobilebackend.ungdunglapkehoachdulich.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/send")
    public ChatResponse send(@RequestBody ChatRequest req) {
        return chatService.processChat(req);
    }
}
