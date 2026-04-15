package com.mobilebackend.ungdunglapkehoachdulich.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AiChatDtos {

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChatRequest {
        private String message;
        private Integer sessionId;
        private Integer userId;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChatResponse {
        private String reply;
        private Integer sessionId;
    }
}
