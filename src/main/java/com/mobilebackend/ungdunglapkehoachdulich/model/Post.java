package com.mobilebackend.ungdunglapkehoachdulich.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String location;

    private Double latitude;

    private Double longitude;

    private Integer budget;

    @Column(name = "image_url")
    private String imageUrl;
}
