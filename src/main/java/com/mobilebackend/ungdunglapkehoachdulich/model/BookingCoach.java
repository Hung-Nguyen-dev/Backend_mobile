package com.mobilebackend.ungdunglapkehoachdulich.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "booking_coaches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingCoach {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    private String seat;

    @Column(name = "pick_up")
    private String pickUp;

    @Column(name = "drop_off")
    private String dropOff;

    @Column(name = "booking_master_id")
    private Integer bookingMasterId;
}
