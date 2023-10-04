package com.tc.training.smallFinance.service.Impl;

import com.tc.training.smallFinance.dtos.inputs.FixedDepositInputDto;
import com.tc.training.smallFinance.dtos.inputs.TransactionInputDto;
import com.tc.training.smallFinance.dtos.outputs.FDDetails;
import com.tc.training.smallFinance.dtos.outputs.FixedDepositOutputDto;
import com.tc.training.smallFinance.exception.AccountNotFoundException;
import com.tc.training.smallFinance.exception.AmountNotSufficientException;
import com.tc.training.smallFinance.exception.KycNotCompletedException;
import com.tc.training.smallFinance.model.AccountDetails;
import com.tc.training.smallFinance.model.FixedDeposit;
import com.tc.training.smallFinance.model.Slabs;
import com.tc.training.smallFinance.repository.AccountRepository;
import com.tc.training.smallFinance.repository.FixedDepositRepository;
import com.tc.training.smallFinance.repository.SlabRepository;
import com.tc.training.smallFinance.service.AccountServiceDetails;
import com.tc.training.smallFinance.service.FixedDepositService;
import com.tc.training.smallFinance.service.TransactionService;
import com.tc.training.smallFinance.utils.Tenures;
import com.tc.training.smallFinance.utils.TypeOfTransaction;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FixedDepositServiceImpl implements FixedDepositService {

    Logger logger = LoggerFactory.getLogger(FixedDeposit.class);
    @Autowired
    private FixedDepositRepository fixedDepositRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private AccountServiceDetails accountServiceDetails;
    @Autowired
    private SlabRepository slabRepository;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private ModelMapper modelMapper;

    public FixedDepositOutputDto createFixedDeposit(FixedDepositInputDto fixedDepositInputDto) {



        Double amount = fixedDepositInputDto.getAmount();
        AccountDetails accountNumber = accountRepository.findById(fixedDepositInputDto.getAccountNumber()).orElseThrow(()->new AccountNotFoundException("account  not found")); // add exception
        //AccountDetails acc = accountServiceDetails.getAccount(fixedDepositInputDto.getAccountNumber());
        Tenures tenures = Tenures.valueOf(fixedDepositInputDto.getTenures());

        if(accountNumber.getKyc()==Boolean.FALSE) throw new KycNotCompletedException("Complete  your kyc");
        if(amount>accountNumber.getBalance()) throw new AmountNotSufficientException("amount exceeds account balance");

        FixedDeposit fixedDeposit = new FixedDeposit();
        fixedDeposit.setAccountNumber(accountNumber);
        fixedDeposit.setAmount(amount);
        fixedDeposit.setSlabs(slabRepository.findByTenuresAndTypeOfTransaction(tenures, TypeOfTransaction.FD));
        fixedDeposit.setDepositedDate(LocalDate.now());
        fixedDeposit.setMaturityDate(getOverDate(fixedDeposit.getSlabs(),fixedDeposit.getDepositedDate()));
        fixedDeposit.setTotalAmount(amount);
        fixedDeposit.setInterestRate(fixedDeposit.getSlabs().getInterestRate());
       // fixedDeposit.getTransactionIds().add();
        performTransaction(fixedDeposit,"DEBIT");
        fixedDepositRepository.save(fixedDeposit);
        FixedDepositOutputDto fixedDepositOutputDto = modelMapper.map(fixedDeposit,FixedDepositOutputDto.class);
        fixedDepositOutputDto.setInterestRate(fixedDeposit.getSlabs().getInterestRate());



        return fixedDepositOutputDto;
    }

    private LocalDate getOverDate(Slabs slabs,LocalDate date) {
        Tenures tenure = slabs.getTenures();
        if(tenure.equals(Tenures.ONE_MONTH)) return date.plusMonths(1);
        else if(tenure.equals(Tenures.THREE_MONTHS))  return date.plusMonths(3);
        else if(tenure.equals(Tenures.SIX_MONTHS))  return date.plusMonths(6);
        else if(tenure.equals(Tenures.ONE_YEAR))  return date.plusYears(1);
        return null;
    }

    @Override
    public List<FixedDepositOutputDto> getAllFixedDeposit(Long accNo) {
        return fixedDepositRepository.findByAccountNumber(accNo).stream().map(fd->modelMapper.map(fd, FixedDepositOutputDto.class)).collect(Collectors.toList());
    }

    @Override
    public FDDetails getFDDetails(Long accNo) {
        Double sum=0D;
        List<FixedDeposit> fd = fixedDepositRepository.findByAccountNumber(accNo);
        for(FixedDeposit f : fd){
            sum+=f.getAmount();
        }
        FDDetails fdDetails = new FDDetails();
        fdDetails.setTotalFdAmount(sum);
        fdDetails.setTotalNoOfFD(fd.size());
        return fdDetails;
    }

    @Override
    public FixedDepositOutputDto breakFixedDeposit(String uid) {
        UUID id = UUID.fromString(uid);
        FixedDeposit fd = fixedDepositRepository.findById(id).orElseThrow(()->new RuntimeException("No fixed repository with that id"));
        fd.setPreMatureWithDrawl(LocalDate.now());
        if(!fd.getIsActive()) throw new RuntimeException("This account is already closed");
        Period period = Period.between(fd.getDepositedDate(),LocalDate.now());

        String interest = "0";
        Double interestAmount=0D;
        if(period.getMonths()<1 && period.getYears()==0 ) {
            interestAmount=0D;
        }
        else if(period.getMonths()>1 && period.getMonths()<3 && period.getYears()==0) {
            interest = slabRepository.findByTenuresAndTypeOfTransaction(Tenures.ONE_MONTH,TypeOfTransaction.FD).getInterestRate();
            interest = String.valueOf(Double.valueOf(interest)-1D);
            interestAmount = (fd.getAmount() * Double.valueOf(interest) * period.getMonths())/100;
        }
        else if(period.getMonths()>3 && period.getMonths()<6 && period.getYears()==0){
            interest = slabRepository.findByTenuresAndTypeOfTransaction(Tenures.THREE_MONTHS,TypeOfTransaction.FD).getInterestRate();
            interest = String.valueOf(Double.valueOf(interest)-1D);
            interestAmount = (fd.getAmount() * Double.valueOf(interest) * (period.getMonths())/3)/100;
        }
        else if(period.getMonths()>6 && period.getYears()==0){
            interest = slabRepository.findByTenuresAndTypeOfTransaction(Tenures.SIX_MONTHS,TypeOfTransaction.FD).getInterestRate();
            interest = String.valueOf(Double.valueOf(interest)-1D);
            interestAmount = (fd.getAmount() * Double.valueOf(interest) * (period.getMonths())/6)/100;
        }


        fd.setInterestAmount(interestAmount);
        fd.getSlabs().setInterestRate(interest);
        fd.setTotalAmount(fd.getAmount()+interestAmount);

      //  fd.getTransactionIds().add();
        closeAccount(fd);
        performTransaction(fd,"CREDIT");
        FixedDepositOutputDto fixedDepositOutputDto = modelMapper.map(fd,FixedDepositOutputDto.class);
        fixedDepositOutputDto.setInterestRate(interest);
        fixedDepositOutputDto.setInterestAmountAdded(interestAmount);
        return fixedDepositOutputDto;
    }

    @Override
    public List<FixedDepositOutputDto>  getAll() {
        return fixedDepositRepository.findAll().stream().map(fd->modelMapper.map(fd, FixedDepositOutputDto.class)).collect(Collectors.toList());
    }

    @Override
    public FixedDepositOutputDto getById(UUID id) {
       FixedDeposit fd = fixedDepositRepository.findById(id).orElseThrow(()-> new AccountNotFoundException("no account found with that id"));
        FixedDepositOutputDto fdout = modelMapper.map(fd,FixedDepositOutputDto.class);
        return fdout;
    }

    @Override
    public List<FixedDepositOutputDto> getAllActive(Long accNo) {
        List<FixedDeposit> fds = fixedDepositRepository.findByAccountNumberAndIsActive(accNo);
        return fds.stream().map(fd->modelMapper.map(fd, FixedDepositOutputDto.class)).collect(Collectors.toList());
    }


    private UUID performTransaction(FixedDeposit fd,String type) {

        TransactionInputDto transactionInputDto = new TransactionInputDto();
        transactionInputDto.setAmount(fd.getTotalAmount());
        transactionInputDto.setTo(String.valueOf(fd.getAccountNumber().getAccountNumber()));
        transactionInputDto.setAccountNumber(String.valueOf(fd.getAccountNumber().getAccountNumber()));
        transactionInputDto.setType("FD");
        transactionInputDto.setPurpose("FD amount "+type);
        transactionInputDto.setTrans(type);
        return transactionService.deposit(transactionInputDto,fd.getAccountNumber().getAccountNumber()).getTransactionID();

    }

    public void closeAccount(FixedDeposit fd){
        fd.setIsActive(Boolean.FALSE);
        fixedDepositRepository.save(fd);
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void mature(){

        logger.info("performing scheduled task");

        List<FixedDeposit> list = fixedDepositRepository.findAllByIsActive();
        LocalDate now = LocalDate.now();

        for(FixedDeposit fd:list){

            LocalDate maturityDate = fd.getMaturityDate();
            if(maturityDate.equals(now)) maturedAccount(fd);

        }
    }
    @Async
    public void maturedAccount(FixedDeposit fd){

        String interest = fd.getSlabs().getInterestRate();
        Double interestAmount = (fd.getAmount() * Double.valueOf(interest) * 1)/100;
        fd.setMaturityDate(LocalDate.now());
        fd.setInterestAmount(interestAmount);
        fd.setTotalAmount(fd.getAmount()+interestAmount);
        closeAccount(fd);

        performTransaction(fd,"credited");

        logger.info("fixed deposit with id "+fd.getFdId()+" is matured");

    }




}

