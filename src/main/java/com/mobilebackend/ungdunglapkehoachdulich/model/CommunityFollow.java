package com.mobilebackend.ungdunglapkehoachdulich.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Follow relationship entity for community users.
 */
@Entity
@Table(name = "community_follows")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityFollow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "follower_user_id", nullable = false)
    private Integer followerUserId;

    @Column(name = "followee_user_id", nullable = false)
    private Integer followeeUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
