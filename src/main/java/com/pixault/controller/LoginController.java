package com.pixault.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;

import com.pixault.service.AuthService;

public class LoginController{
    @FXML
    private TextField emaField;
    
    @FXML
    private PasswordField passwordField;

    private final AuthService auth=new AuthService();

    @FXML
    public void handleLogin(){
        String email=emaField.getText();
        String password=passwordField.getText();

        boolean success=auth.loginStep1(email, password);

        if(success){
            System.out.println("OTP sent to email");
        }
        else{
            System.out.println("Invalid Credentials");
        }
    }

}
