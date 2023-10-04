package com.tc.training.smallFinance.controllers;

import com.tc.training.smallFinance.dtos.inputs.FixedDepositInputDto;
import com.tc.training.smallFinance.dtos.outputs.FDDetails;
import com.tc.training.smallFinance.dtos.outputs.FixedDepositOutputDto;
import com.tc.training.smallFinance.service.FixedDepositService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/fd")
public class FixedDepositController {
    @Autowired
    private FixedDepositService fixedDepositService;
    @PostMapping("/createFixedDeposit")
    public FixedDepositOutputDto createFixedDeposit(@RequestBody  FixedDepositInputDto fixedDepositInputDto){
        return fixedDepositService.createFixedDeposit(fixedDepositInputDto);
    }

    @GetMapping("/getAllByUser")
    public List<FixedDepositOutputDto> getAllFixedDeposit(@RequestParam Long accNo){
        return fixedDepositService.getAllFixedDeposit(accNo);
    }

    @GetMapping("/getDetails")
    public FDDetails getFDDetails(@RequestParam Long accNo){
        return fixedDepositService.getFDDetails(accNo);
    }

    @PostMapping("/break")
    public FixedDepositOutputDto breakFixedDeposit(@RequestParam String id){

        return fixedDepositService.breakFixedDeposit(id);
    }

    @GetMapping("/getAll")
    public List<FixedDepositOutputDto> getAll(){
       return fixedDepositService.getAll();
    }

    @GetMapping("/getbyId")
    public FixedDepositOutputDto getById(@RequestParam UUID id){
        return fixedDepositService.getById(id);
    }

    @GetMapping("/getAllActive")
    public List<FixedDepositOutputDto> getAllActive(Long accNo){
        return fixedDepositService.getAllActive(accNo);
    }

}
