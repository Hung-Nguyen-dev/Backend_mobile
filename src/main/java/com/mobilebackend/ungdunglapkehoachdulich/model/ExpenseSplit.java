package com.mobilebackend.ungdunglapkehoachdulich.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "expense_splits")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpenseSplit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "owed_amount")
    private Float owedAmount;

    @Column(name = "is_settled")
    private Integer isSettled;

    @Column(name = "expense_id")
    private Integer expenseId;

    @Column(name = "user_id")
    private Integer userId;
}
