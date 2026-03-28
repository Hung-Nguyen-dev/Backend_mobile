package com.mobilebackend.ungdunglapkehoachdulich.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_itineraries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiItinerary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String title;

    @Column(name = "generated_content", columnDefinition = "TEXT")
    private String generatedContent;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "trip_id")
    private Integer tripId;
}
