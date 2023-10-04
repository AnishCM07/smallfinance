package com.tc.training.smallFinance.model;

import com.tc.training.smallFinance.utils.Tenures;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FixedDeposit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID fdId;

    @ManyToOne
    @JoinColumn(referencedColumnName = "accountNumber")
    private AccountDetails accountNumber;

    @ManyToOne
    @JoinColumn(referencedColumnName = "slabId")
    private Slabs slabs;

    private String interestRate;

    private Double amount;

    private LocalDate depositedDate;

    private LocalDate maturityDate;

    private Boolean isActive = Boolean.TRUE;

    private LocalDate preMatureWithDrawl = null;

    private Double totalAmount;

    private Double interestAmount = 0D;


}
