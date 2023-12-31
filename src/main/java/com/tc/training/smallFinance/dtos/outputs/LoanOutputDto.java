package com.tc.training.smallFinance.dtos.outputs;

import com.tc.training.smallFinance.model.AccountDetails;
import com.tc.training.smallFinance.model.Slabs;
import com.tc.training.smallFinance.utils.Status;
import com.tc.training.smallFinance.utils.TypeOfLoans;
import com.tc.training.smallFinance.utils.TypeOfTransaction;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
public class LoanOutputDto {

    private UUID loanId;

    private String accountNumber;

    private LocalDate appliedDate;

    private Boolean isActive ;

    private String tenure;

    private Status status ;

    private Double loanedAmount;

    private TypeOfLoans typeOfLoan ;

    private LocalDate loanEndDate;

    private String interest;

    private Double interestAmount;

    private Integer monthlyInterestAmount;

    private Double totalAmount;

    private String loanSuppliment1;

    private String loanSuppliment2;

    private String firstName;

    private String lastName;

    private LocalDate dob;

    private Integer age;

    private String email;


}
