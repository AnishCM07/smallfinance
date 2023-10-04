package com.tc.training.smallFinance.model;

import com.tc.training.smallFinance.utils.Role;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.UniqueElements;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Entity
@Table(name = "\"user\"")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userId;

    @NotBlank
    @Size(min=3,message = "Please enter name with atleast 3 characters")
    private String firstName;

    private String lastName;

    @NotBlank
    private String password;

    private LocalDate dob;

    @Email
    private String email;

    private Integer age;

    private String aadharCardNumber;

    private String panCardNumber;

    private String phoneNumber;

    private String aadharPhoto;

    private String panPhoto;

    private String  userPhoto;
    
    private Role roleName = Role.CUSTOMER;

    private String firebaseId;


}
