package com.pixault.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;

import com.pixault.service.AuthService;

public class LoginController {
    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    private final AuthService auth = new AuthService();

    @FXML
    public void handleLogin(){
        try{
            String email=emailField.getText();
            String password=passwordField.getText();

            boolean success=auth.loginStep1(email, password);

            if(success){
                System.out.println("OTP sent to email");

                 //pass email to OTP screen
                 OTPController.setEmail(email);

                //Load OTP screen
                FXMLLoader loader=new FXMLLoader(getClass().getResource("/otp.fxml"));
                Scene scene=new Scene(loader.load());

                // Get current stage
                Stage stage=(Stage) emailField.getScene().getWindow();
                stage.setScene(scene);
          }
            else{
                 System.out.println("Invalid Credentials");
        }
    }catch(Exception e){
        e.printStackTrace();
    }
  } 
}
