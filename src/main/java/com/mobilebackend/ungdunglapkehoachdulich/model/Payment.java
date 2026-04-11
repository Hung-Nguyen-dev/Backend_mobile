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

    private Float amount;

    @Column(name = "payment_date")
    private LocalDate paymentDate;

    @Column(name = "booking_master_id")
    private Integer bookingMasterId;

    /** VNPAY, MOMO, ... */
    private String provider;

    /** PENDING, SUCCESS, FAILED, ... */
    private String status;
}
