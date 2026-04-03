package com.mobilebackend.ungdunglapkehoachdulich.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "trip_members")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripMember {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "member_role")
    private Integer memberRole;

    @Column(name = "trip_id")
    private Integer tripId;

    @Column(name = "user_id")
    private Integer userId;

    private Integer status; // 0: khong tham gia, 1: tham gia
}
