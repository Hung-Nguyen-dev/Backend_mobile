package com.mobilebackend.ungdunglapkehoachdulich.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "posts_itinerary_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostItineraryDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "post_id")
    private Integer postId;

    @Column(name = "itinerary_detail_id")
    private Integer itineraryDetailId;

    private String status;

    @Column(name = "user_id")
    private Integer userId;
}
