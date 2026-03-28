package com.mobilebackend.ungdunglapkehoachdulich.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "expenses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Float amount;

    private String category;

    private String description;

    @Column(name = "trip_id")
    private Integer tripId;

    @Column(name = "user_id")
    private Integer userId;
}
