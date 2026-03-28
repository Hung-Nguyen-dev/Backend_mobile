package com.mobilebackend.ungdunglapkehoachdulich.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "itinerary_details")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "visit_time")
    private LocalTime visitTime;

    private String note;

    @Column(name = "itinerary_id")
    private Integer itineraryId;
}
