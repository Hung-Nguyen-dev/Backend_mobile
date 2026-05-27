package com.mobilebackend.ungdunglapkehoachdulich.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Like/save interaction entity for community posts.
 */
@Entity
@Table(name = "community_post_interactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityPostInteraction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "post_id", nullable = false)
    private Integer postId;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
