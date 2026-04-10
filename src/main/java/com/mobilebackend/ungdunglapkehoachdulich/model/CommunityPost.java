package com.mobilebackend.ungdunglapkehoachdulich.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "trip_id")
    private Integer tripId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    private String location;

    private Integer budget;

    @Column(name = "is_trip_linked", nullable = false)
    private Integer isTripLinked;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
