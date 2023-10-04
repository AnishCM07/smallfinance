package com.tc.training.smallFinance.service.Impl;

import com.google.firebase.auth.UserRecord;
import com.tc.training.smallFinance.dtos.inputs.AccountDetailsInputDto;
import com.tc.training.smallFinance.dtos.inputs.FirebaseUserInputDto;

import com.tc.training.smallFinance.dtos.outputs.AccountDetailsOutputDto;
import com.tc.training.smallFinance.dtos.outputs.FDDetails;
import com.tc.training.smallFinance.dtos.outputs.HomePageOutputDto;
import com.tc.training.smallFinance.dtos.outputs.UserOutputDto;
import com.tc.training.smallFinance.exception.AccountNotFoundException;
import com.tc.training.smallFinance.exception.CustomException;
import com.tc.training.smallFinance.model.AccountDetails;
import com.tc.training.smallFinance.model.User;
import com.tc.training.smallFinance.repository.AccountRepository;
import com.tc.training.smallFinance.service.*;
import org.apache.tomcat.util.codec.binary.Base64;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.*;


@Service
public class AccountServiceDetailsImpl implements AccountServiceDetails {
    @Autowired
    private EmailService emailService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private ModelMapper modelMapper;
    @Autowired
    private UserService userService;
    @Autowired
    private FirebaseUserService firebaseUserService;

    @Autowired
    private DepositService depositService;

    @Autowired
    private LoanService loanService;

    private static long lastTimestamp = 8804175630060020L;

    private static int sequence = 0;

    @Override
    public AccountDetailsOutputDto createAccount(AccountDetailsInputDto accountDetailsInputDto) {
        AccountDetails accountDetails = modelMapper.map(accountDetailsInputDto, AccountDetails.class);
        accountDetails.setUser(userService.addUser(accountDetailsInputDto));
        accountDetails.setAccountNumber(generateUniqueAccountNumber());
        accountDetails.setOpeningDate(LocalDate.now());
        try {
            sendEmail(accountDetails.getUser().getEmail(), accountDetails.getUser().getPassword(), accountDetails.getAccountNumber());
            AccountDetailsOutputDto outputDto = modelMapper.map(accountDetails, AccountDetailsOutputDto.class);
            outputDto.setEmail(accountDetails.getUser().getEmail());

            AccountDetails accountDetails1 = modelMapper.map(accountDetails, AccountDetails.class);
            FirebaseUserInputDto inputDto = new FirebaseUserInputDto();
            inputDto.setAccountNumber(String.valueOf(accountDetails.getAccountNumber()));
            inputDto.setName(accountDetails.getUser().getFirstName());
            inputDto.setPassword(accountDetails.getUser().getPassword());

            UserRecord userInFireBase = firebaseUserService.createUserInFireBase(inputDto);
            accountDetails.getUser().setFirebaseId(userInFireBase.getUid());
            accountDetails = accountRepository.save(accountDetails);
        }catch(Exception e){throw new CustomException("error in creating account");}
        AccountDetailsOutputDto accountDetailsOutputDto =  modelMapper.map(accountDetails,AccountDetailsOutputDto.class);
        accountDetailsOutputDto.setEmail(accountDetails.getUser().getEmail());
        return accountDetailsOutputDto;
    }



//    User user1 = modelMapper.map(user, User.class);

//    FirebaseUserInputDto inputDto = new FirebaseUserInputDto();

//inputDto.setEmail(user1.getEmail()); //setting the login details given by the user to the firebase inputDto

//inputDto.setName(user1.getName());

//    String randomPassword = RandomStringUtils.randomAlphanumeric(9); //generating a random password of length 9 characters
//log.info(randomPassword);
//inputDto.setPassword(randomPassword);
//
//    UserRecord userInFireBase = firebaseUserService.createUserInFireBase(inputDto);
//user1.setFirebaseId(userInFireBase.getUid()); //creating user in firebase
//    user1 = userRepository.save(user1);
//return modelMapper.map(user1, UserOutputDto.class);









    @Override
    public AccountDetailsOutputDto getAccount(Long accNo) {
        AccountDetails accountDetails =  accountRepository.getById(accNo);
        return modelMapper.map(accountDetails,AccountDetailsOutputDto.class);
    }

    @Override
    public Double getBalance(Long accNo) {
        return accountRepository.findById(accNo).get().getBalance();
    }

    @Override
    public AccountDetailsOutputDto getAccountByUser(UUID userId) {
        User user = modelMapper.map(userService.getById(userId),User.class);

        return modelMapper.map(accountRepository.findByUser(user), AccountDetailsOutputDto.class);
    }

    @Override
    public HomePageOutputDto getHomePageDetails(Long accNo) {

        AccountDetails accountDetails = accountRepository.findById(accNo).orElseThrow(()->new AccountNotFoundException("no account found with this id"));
        HomePageOutputDto homePageOutputDto = new HomePageOutputDto();
        homePageOutputDto.setBalance(accountDetails.getBalance());
        FDDetails fdDetails = depositService.getDetails(accNo);
        homePageOutputDto.setDepositAmount(fdDetails.getTotalFdAmount() + fdDetails.getTotalRdAmount());
        homePageOutputDto.setLoanAmount(loanService.getTotalLoanAmount(accNo));
        homePageOutputDto.setKyc(accountDetails.getKyc());
        return homePageOutputDto;
    }

    @Override
    public AccountDetailsOutputDto verifyKyc(Long accNo) {
        AccountDetails accountDetails = accountRepository.findById(accNo).orElseThrow(()->new AccountNotFoundException("no account found with this id"));
        accountDetails.setKyc(Boolean.TRUE);
        accountRepository.save(accountDetails);
        return modelMapper.map(accountDetails,AccountDetailsOutputDto.class);
    }

    public AccountDetailsOutputDto updateAccount(AccountDetailsInputDto accountDetailsInputDto, Long accountNumber){
        AccountDetails accountDetails = accountRepository.findById(accountNumber).orElseThrow(()-> new RuntimeException("account not found"));
        AccountDetails accountDetails1 = modelMapper.map(accountDetailsInputDto, AccountDetails.class);
        modelMapper.map(accountDetails1,accountDetails);
        accountDetails1 = accountRepository.save(accountDetails1);
        return modelMapper.map(accountDetails1,AccountDetailsOutputDto.class);
    }

    private synchronized Long generateUniqueAccountNumber() {
        List<AccountDetails> accountDetailsList = accountRepository.findAll();
        if(!accountDetailsList.isEmpty()) {
            Collections.sort(accountDetailsList, Comparator.comparing(AccountDetails::getAccountNumber));
            lastTimestamp = accountDetailsList.get(accountDetailsList.size() - 1).getAccountNumber();
        }
        return ++lastTimestamp;
    }

    private void sendEmail(String email, String password,Long accountNumber) {
        String subject = "Username and password for your account";
        String text = "Thank you for registering with our bank your account number is "+accountNumber+ " and your password is "+password;
        emailService.sendEmail(email,subject,text);
    }

    private String encode(MultipartFile file) {
        String b64="";
        try {
            byte[] b = file.getBytes();
            b64 = Base64.encodeBase64String(b);
        }
        catch(IOException e){}
        return b64;
    }




}
