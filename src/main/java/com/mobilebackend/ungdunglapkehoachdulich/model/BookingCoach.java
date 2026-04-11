package com.mobilebackend.ungdunglapkehoachdulich.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

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

    @Column(name = "plate_number")
    private String plateNumber;

    @Column(name = "departure_date")
    private LocalDate departureDate;

    @Column(name = "departure_time")
    private LocalTime departureTime;

    @Column(name = "booking_master_id")
    private Integer bookingMasterId;
}
