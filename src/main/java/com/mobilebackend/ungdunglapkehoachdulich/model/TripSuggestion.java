package com.mobilebackend.ungdunglapkehoachdulich.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trip_suggestions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripSuggestion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "trip_id")
    private Integer tripId;

    @Column(name = "location_id")
    private Integer locationId;
}
