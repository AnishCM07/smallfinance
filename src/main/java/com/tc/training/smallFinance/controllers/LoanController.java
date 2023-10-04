package com.tc.training.smallFinance.controllers;

import com.tc.training.smallFinance.dtos.inputs.LoanInputDto;
import com.tc.training.smallFinance.dtos.outputs.LoanOutputDto;
import com.tc.training.smallFinance.service.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/loan")
public class LoanController {
    @Autowired
    private LoanService loanService;

    @PostMapping("/apply")
    public LoanOutputDto addLoan(@RequestBody LoanInputDto loanInputDto){
        return loanService.addLoan(loanInputDto);
    }

    @GetMapping("/getById")
    public LoanOutputDto getAll(@RequestParam UUID id){
        return loanService.getById(id);
    }

    @GetMapping("/getAllByUser")
    public List<LoanOutputDto> getAllByUser(@RequestParam(required = false) Long accNo){
        return loanService.getAllByUser(accNo);
    }

    @GetMapping("/getAll")
    public List<LoanOutputDto> getAll(){
        return loanService.getAll();
    }

    @PutMapping("/set")
    public LoanOutputDto setLoan(@RequestParam UUID id,@RequestParam String status){
       return loanService.setLoan(id,status);
    }

    @PostMapping("/repay")
    public ResponseEntity repayLoan(@RequestParam UUID loanId, @RequestParam Double paymentAmount){
        return loanService.repay(loanId,paymentAmount);
    }

    @GetMapping("/getByType")
    public List<LoanOutputDto> getByType(@RequestParam Long accNo , @RequestParam String type){
        return loanService.getBytype(accNo,type);
    }

    @GetMapping("/getTotalLoanAmount")
    public Double getTotalLoanAmount(@RequestParam Long accNo){

        return loanService.getTotalLoanAmount(accNo);

    }
    @GetMapping("/getAllPending")
    public List<LoanOutputDto> getAllPending(){
        return loanService.getAllPending();
    }

    @GetMapping("/getAllByNotPending")
    public List<LoanOutputDto> getAllByNotPending(){
        return loanService.getAllByNotPending();
    }
    @GetMapping("/getAllByStatus")
    public List<LoanOutputDto> getAllByStatus(@RequestParam String status){
        return loanService.getAllByStatus(status);
    }

    @PutMapping(value = "/uploadSuppliments/{id}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public void uploadSuppliment(@RequestParam MultipartFile file1, @RequestParam MultipartFile file2,  @PathVariable UUID id){
        loanService.uploadSuppliment(file1,file2,id);
    }

}