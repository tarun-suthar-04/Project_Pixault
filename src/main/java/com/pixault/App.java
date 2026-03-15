package com.pixault;

import java.time.LocalDateTime;
import java.util.Scanner;
import com.pixault.model.User;
import com.pixault.dao.OTPDAO;
import com.pixault.dao.UserDAO;
import com.pixault.util.OTPUtil;


public class App {

    public static void main(String[] args) {

        // user registration from console
        Scanner sc = new Scanner(System.in);
        UserDAO dao = new UserDAO();

        System.out.print("Enter email : ");
        String email = sc.nextLine();

        if(dao.emailExists(email)){
            System.out.println("Email already registered");
            return;

        }
        System.out.print("Enter password : ");
        String password = sc.nextLine();

        System.out.print("Confirm password : ");
        String confirmPassword = sc.nextLine();

        if(!password.equals(confirmPassword)){
            System.out.println("Password doesn't match");
            sc.close();
        }

        //generate OTP
        String otp=OTPUtil.generateOTP();

        LocalDateTime expiry=LocalDateTime.now().plusMinutes(1);

        OTPDAO otpDao=new OTPDAO();
        otpDao.saveOTP(email,otp,expiry);
        System.out.println("OTP sent to email : "+otp);

        System.out.print("Enter OTP : ");
        String userOTP=sc.nextLine();

        if(otpDao.verifyOTP(email, userOTP)){
            System.out.println("OTP verified");
        }
        else{
            System.out.println("Invalid OTP");
        }

        User user = new User(email, password);
        //registration tetsing
        if(dao.registerUser(user)){
        System.out.println("Registration Successful");
        }
        else{
        System.out.println("Registration failed");
        }
        sc.close();


        //Login testing
        // boolean success = dao.loginUser(email, password);

        // if (success)
        //     System.out.println("Login Successful");
        // else
        //     System.out.println("Invalid credentials");

    }
}
