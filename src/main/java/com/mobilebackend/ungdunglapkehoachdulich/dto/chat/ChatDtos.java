package com.mobilebackend.ungdunglapkehoachdulich.dto.chat;

import lombok.*;

public final class ChatDtos {
    private ChatDtos() {}

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatRequest {
        private Integer userId;
        private Integer sessionId; // Optional: Nếu null sẽ tạo mới
        private String message;
        private String currentCity; // Optional: Để AI biết đang nói về tỉnh nào
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatResponse {
        private Integer sessionId;
        private String reply;
        private String status;
    }
}
