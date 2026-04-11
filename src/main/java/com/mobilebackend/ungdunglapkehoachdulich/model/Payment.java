package com.mobilebackend.ungdunglapkehoachdulich.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "payments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "transaction_no")
    private String transactionNo;

    @Column(name = "provider")
    private String provider;

    @Column(name = "status")
    private String status;

    private Float amount;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "booking_master_id")
    private Integer bookingMasterId;
}
