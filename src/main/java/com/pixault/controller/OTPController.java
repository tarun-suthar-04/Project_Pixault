package com.pixault.controller;

import com.pixault.service.AuthService;
// import com.pixault.controller.VaultController;

import javafx.fxml.FXML;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.TextField;

public class OTPController {

    @FXML
    private TextField otpField;

    private static String email; // passed from login
    private final AuthService authService = new AuthService();

    // setter to recieve mail
    public static void setEmail(String userEmail) {
        email = userEmail;
    }

    @FXML
    private void handleVerify() {
        try{

            String otp = otpField.getText();
            
            String sessionToken = authService.loginStep2(email, otp);
            
            if (sessionToken != null) {
                System.out.println("Login Success");
                
                // pass session token to vault
                VaultController.setSessionToken(sessionToken);
                
                // Load vault screen
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/vault.fxml"));
                Scene scene = new Scene(loader.load());

                Stage stage=(Stage) otpField.getScene().getWindow();
                stage.setScene(scene);

            } else {
                System.out.println("Invalid OTP");
            }
        }catch(Exception e){
            e.printStackTrace();
        }
    }

}
