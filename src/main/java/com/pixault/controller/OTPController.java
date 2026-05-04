package com.pixault.controller;

import com.pixault.service.AuthService;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;


public class OTPController {

    @FXML
    private TextField otpField;

    private static String email; //passed from login
    private final AuthService authService=new AuthService();

    // setter to recieve mail
    public static void setEmail(String userEmail){
        email=userEmail;
    }

    @FXML
    private void handleVerify(){
        String otp=otpField.getText();

        String sessionToken=authService.loginStep2(email, otp);

        if(sessionToken!=null){
            System.out.println("Login Success , Session : "+ sessionToken);
        }
        else{
            System.out.println("Invalid OTP");
        }
    }

    
}
