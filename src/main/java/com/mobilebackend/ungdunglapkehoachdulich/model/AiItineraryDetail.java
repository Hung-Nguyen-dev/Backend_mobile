package com.mobilebackend.ungdunglapkehoachdulich.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ai_itinerary_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiItineraryDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "day_number")
    private Integer dayNumber;

    @Column(name = "ai_itinerary_id")
    private Integer aiItineraryId;

    @Column(name = "location_id")
    private Integer locationId;
}
