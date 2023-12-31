package com.tc.training.smallFinance.service.Impl;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.gson.Gson;
import com.tc.training.smallFinance.dtos.inputs.AccountDetailsInputDto;
import com.tc.training.smallFinance.dtos.inputs.LoginInputDto;
import com.tc.training.smallFinance.dtos.outputs.LoginOutputDto;
import com.tc.training.smallFinance.dtos.outputs.SignInFirebaseOutput;
import com.tc.training.smallFinance.dtos.outputs.UserOutputDto;
import com.tc.training.smallFinance.exception.AccountNotFoundException;
import com.tc.training.smallFinance.exception.GlobalException;
import com.tc.training.smallFinance.exception.ImageNotUploaded;
import com.tc.training.smallFinance.model.AccountDetails;
import com.tc.training.smallFinance.model.User;
import com.tc.training.smallFinance.repository.AccountRepository;
import com.tc.training.smallFinance.repository.TransactionRepository;
import com.tc.training.smallFinance.repository.UserRepository;
import com.tc.training.smallFinance.service.TransactionService;
import com.tc.training.smallFinance.service.UserService;
import com.tc.training.smallFinance.utils.Role;
import lombok.extern.log4j.Log4j2;
import org.apache.tomcat.util.codec.binary.Base64;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Log4j2
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private EmailServiceImpl emailService;

    @Autowired
    private ModelMapper modelMapper;

    @Value("${firebase.storage.bucket-name}")
    private String bucketName;

    @Value("${firebase.config-file}")
    private String FIREBASE_CONFIG_FILE;

    private Storage storage;


    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*";


    public User addUser(AccountDetailsInputDto accountDetailsInputDto) {
        User user = new User();
        user.setFirstName(accountDetailsInputDto.getFirstName());
        user.setLastName(accountDetailsInputDto.getLastName());
        user.setDob(accountDetailsInputDto.getDob());
        user.setEmail(accountDetailsInputDto.getEmail());
        user.setPanCardNumber(accountDetailsInputDto.getPanCardNumber());
        user.setAadharCardNumber(accountDetailsInputDto.getAadharCardNumber());
        user.setPhoneNumber(accountDetailsInputDto.getPhoneNumber());
        user.setAge(calculateAge(accountDetailsInputDto.getDob()));
        user.setPassword(generateRandomPassword());
        return userRepository.save(user);
    }


    @Override
    public void uploadImage(MultipartFile file1, MultipartFile file2, MultipartFile file3, String userName) {
        User user = userRepository.findByEmail(userName);
        user.setAadharPhoto(uploadPic(file1));
        user.setPanPhoto(uploadPic(file2));
        user.setUserPhoto(uploadPic(file3));

        userRepository.save(user);
    }

    @PostConstruct
    public void initialize() throws IOException {
        InputStream firebaseFile = new ClassPathResource(FIREBASE_CONFIG_FILE).getInputStream();

        this.storage = StorageOptions.newBuilder()
                .setCredentials(GoogleCredentials.fromStream(firebaseFile))
                .build()
                .getService();
    }

    @Override
    public String uploadPic(MultipartFile file) {
        String blobName = file.getOriginalFilename();
        BlobId blobId = BlobId.of(bucketName, blobName);
        BlobInfo build = BlobInfo.newBuilder(blobId).build();
        try {
            storage.create(build, file.getBytes());
        } catch (IOException e) {
            throw new ImageNotUploaded("Image not uploaded");
        }
        return String.format("https://firebasestorage.googleapis.com/v0/b/bank-36bf0.appspot.com/o/%s?alt=media", blobName);

    }

    @Override
    public List<UserOutputDto> getAll() {
        List<User> userList =   userRepository.findByCustomer();
        List<UserOutputDto> list =  new ArrayList<>();
        for(User user : userList){
            UserOutputDto userOutputDto = new UserOutputDto();
            userOutputDto = modelMapper.map(user,UserOutputDto.class);
            userOutputDto.setAccountNumber(accountRepository.findByUser(user).getAccountNumber());
            userOutputDto.setKyc(accountRepository.findByUser(user).getKyc());
            list.add(userOutputDto);
        }

        Collections.sort(list, new Comparator<UserOutputDto>() {
            @Override
            public int compare(UserOutputDto o1, UserOutputDto o2) {
                User user1 = userRepository.findById(o1.getUserId()).orElseThrow(()->new AccountNotFoundException("no account with this id"));
                User user2 = userRepository.findById(o2.getUserId()).orElseThrow(()->new AccountNotFoundException("no account with this id"));
                if(accountRepository.findByUser(user1).getOpeningDate().isAfter(accountRepository.findByUser(user2).getOpeningDate()))
                    return 1;
                else if(accountRepository.findByUser(user1).getOpeningDate().isBefore(accountRepository.findByUser(user2).getOpeningDate())) return -1;
                else return 0;
            }
        });
        return list;
    }

    @Override
    public UserOutputDto getById(UUID id) {
        User user = userRepository.findById(id).orElseThrow(()->new AccountNotFoundException("account with this id not found"));
        UserOutputDto userOutputDto = modelMapper.map(user,UserOutputDto.class);
        userOutputDto.setKyc(accountRepository.findByUser(user).getKyc());
        userOutputDto.setAccountNumber(accountRepository.findByUser(user).getAccountNumber());
        return userOutputDto;
    }

    @Override
    public byte[] getImage(String userName) {
        String base = userRepository.findByEmail(userName).getAadharPhoto();
        byte[] b = Base64.decodeBase64(base);
        return b;
    }

    @Override
    public LoginOutputDto login(LoginInputDto loginInputDto) {

        String FIREBASE_URL = "https://identitytoolkit.googleapis.com/v1/accounts";

        String FIREBASE_API_KEY = "AIzaSyCWNd26kOuuzMIZ2pTRu4CnlI69u4zieFY";

        String url = FIREBASE_URL + ":signInWithPassword?key=" + FIREBASE_API_KEY;

//        Long userName = Long.valueOf(loginInputDto.getUserName());
//        User user = accountRepository.findById(userName).get().getUser();
        //  if (user==null) throw new AccountNotFoundException("Wrong account number");
//        if (user == null) {
//            throw new UserNotFound("Incorrect user name");
//        }
//        String pass = user.getPassword();
//        if (!pass.equals(loginInputDto.getPassword())) throw new UserNotFound("Incorrect password");

        Map<String, Object> map = new HashMap<>();
        map.put("email", loginInputDto.getAccountNumber() + "@aba.com");      //getUserName()
        map.put("password", loginInputDto.getPassword());
        map.put("returnSecureToken", true);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        HttpEntity<?> httpEntity = new HttpEntity<>(map, httpHeaders);
        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class);
        String body = exchange.getBody(); // we will get body having local id access tokens in string format
        Gson gson = new Gson(); //it is used to covert string to any object
        SignInFirebaseOutput signInFireBaseOutput = gson.fromJson(body, SignInFirebaseOutput.class);

        LoginOutputDto loginOutputDto = new LoginOutputDto();
        loginOutputDto.setAccessToken(signInFireBaseOutput.getIdToken());
        loginOutputDto.setRefreshToken(signInFireBaseOutput.getRefreshToken());
        loginOutputDto.setExpiresIn(signInFireBaseOutput.getExpiresIn());

        try {
            Long userName = Long.valueOf(loginInputDto.getAccountNumber());
            AccountDetails accountDetails = accountRepository.findById(userName).orElseThrow(() -> new AccountNotFoundException("no account found with that account number"));
            User user = accountDetails.getUser();
            loginOutputDto = modelMapper.map(accountDetails, LoginOutputDto.class);
            loginOutputDto.setAccessToken(signInFireBaseOutput.getIdToken());
            loginOutputDto.setRefreshToken(signInFireBaseOutput.getRefreshToken());
            loginOutputDto.setExpiresIn(signInFireBaseOutput.getExpiresIn());
            loginOutputDto.setFirstName(user.getFirstName());
            loginOutputDto.setLastName(user.getLastName());
            loginOutputDto.setPhoneNumber(user.getPhoneNumber());
            loginOutputDto.setBalance(accountDetails.getBalance());
            loginOutputDto.setAccNo(accountDetails.getAccountNumber());
            loginOutputDto.setKyc(accountDetails.getKyc());
            loginOutputDto.setRoleName(accountDetails.getUser().getRoleName());
            loginOutputDto.setEmail(accountDetails.getUser().getEmail());
            loginOutputDto.setAadharCardNumber(accountDetails.getUser().getAadharCardNumber());
            loginOutputDto.setPanCardNumber(accountDetails.getUser().getPanCardNumber());
        }catch(NumberFormatException e){ loginOutputDto.setRoleName(Role.MANAGER); }


//         List<TransactionOutputDto> list = transactionService.getAllTransactions(null,null, accountDetails.getAccountNumber());
//         loginOutputDto.setTransactions(list);
        return loginOutputDto;
    }


//    public LoginOutputDto login(LoginInputDto loginInputDto) {

//        String url = FIREBASE_URL + ":signInWithPassword?key=" + FIREBASE_API_KEY;
//        Map<String, Object> map = new HashMap<>();
//        map.put("email", loginInputDto.getUsername()); //mapping the username to email
//        map.put("password", loginInputDto.getPassword()); //mapping the password to the password field in firebase
//        map.put("returnSecureToken", true); //setting it to true so firebase sends us token
//
//        RestTemplate restTemplate = new RestTemplate(); // to call apis through java
//        HttpHeaders httpHeaders = new HttpHeaders();
//        HttpEntity<?> httpEntity = new HttpEntity<>(map, httpHeaders);
//        ResponseEntity<String> exchange = restTemplate.exchange(url, HttpMethod.POST, httpEntity, String.class);
//        String body = exchange.getBody(); // we will get body having local id access tokens in string format
//        Gson gson = new Gson(); //it is used to covert string to any object
//        SignInFirebaseOutput signInFireBaseOutput = gson.fromJson(body, SignInFirebaseOutput.class); // body mapped to fields in SignInFieBaseOutputDto


    //        LoginOutputDto loginOutputDto = new LoginOutputDto();
//        loginOutputDto.setAccessToken(signInFireBaseOutput.getIdToken());
//        loginOutputDto.setRefreshToken(signInFireBaseOutput.getRefreshToken());
//        loginOutputDto.setExpiresIn(signInFireBaseOutput.getExpiresIn());
//        loginOutputDto.setUsername(loginInputDto.getUsername());
//
//        return loginOutputDto;
//    }
//
    @Override
    public User getByFirebaseId(String userUid) {
        return userRepository.findByFirebaseId(userUid);
      /*  .orElseThrow(() -> {
            log.error("no such firebase ID found");
            return new ElementNotFound("No such firebase ID");
        });*/
    }

    @Override
    public ResponseEntity<String> resetPassword(Long accNo) {
        String email = accountRepository.findById(accNo).get().getUser().getEmail();

        String firebaseEmail = accNo + "@aba.com";

        try {
            String link = FirebaseAuth.getInstance().generatePasswordResetLink(firebaseEmail);
            emailService.sendEmail(email, "Password Reset", link);

            return ResponseEntity.ok("Password reset email sent");
        } catch (FirebaseAuthException e) {
            return ResponseEntity.badRequest().body("Failed to send password reset email: " + e.getMessage());
        }
    }


    public String convertImage(MultipartFile file) {

        String b64 = "";

        byte[] b;
        try {
            b = file.getBytes();
            b64 = Base64.encodeBase64String(b);
        } catch (IOException e) {
        }

        return b64;
    }


    public static int calculateAge(LocalDate dob) {
        LocalDate currentDate = LocalDate.now();
        Period period = Period.between(dob, currentDate);
        return period.getYears();
    }

    public static String generateRandomPassword() {
        StringBuilder password = new StringBuilder();
        Random random = new SecureRandom();

        for (int i = 0; i < 9; i++) {
            int randomIndex = random.nextInt(CHARACTERS.length());
            password.append(CHARACTERS.charAt(randomIndex));
        }

        return password.toString();
    }
}
