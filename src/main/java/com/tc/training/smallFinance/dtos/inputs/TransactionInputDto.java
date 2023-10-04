package com.tc.training.smallFinance.dtos.inputs;

import com.tc.training.smallFinance.model.AccountDetails;
import lombok.Data;

@Data
public class TransactionInputDto {

    //private String from;

    private String to;

    private Double amount;

    private String purpose;

    private String accountNumber;

    private String type;

    private String trans;


}
