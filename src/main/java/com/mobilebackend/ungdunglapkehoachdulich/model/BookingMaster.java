package com.mobilebackend.ungdunglapkehoachdulich.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "booking_masters")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "total_amount")
    private Float totalAmount;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "trip_id")
    private Integer tripId;

    @Column(name = "user_id")
    private Integer userId;
}
