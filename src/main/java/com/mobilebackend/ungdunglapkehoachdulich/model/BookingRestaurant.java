package com.mobilebackend.ungdunglapkehoachdulich.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Entity
@Table(name = "booking_restaurants")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRestaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String address;

    @Column(name = "reservation_time")
    private LocalTime reservationTime;

    @Column(name = "number_of_guests")
    private Integer numberOfGuests;

    @Column(name = "booking_master_id")
    private Integer bookingMasterId;
}
