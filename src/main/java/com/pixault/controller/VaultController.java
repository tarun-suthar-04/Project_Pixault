package com.pixault.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Label;

import com.pixault.service.VaultService;

public class VaultController {
    @FXML
    private TextField dataField;

    @FXML
    private  PasswordField passwordField;
    
    @FXML
    private  Label outpLabel;
    
    private  static String sessionToken ;
    private final VaultService vaultService=new VaultService();

    //receive session token from otp screen
    public static void setSessionToken(String token ){
        sessionToken=token;
    }

    @FXML
    private void handleStore(){
        try{
            String data = dataField.getText();
            String password=passwordField.getText();

            vaultService.storeData(sessionToken, password, data);
            outpLabel.setText("Data Stored Securely");
        }catch(Exception e){
            outpLabel.setText("Error Storing Data");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRetrieve(){
        try{
            String password=passwordField.getText();
            String data=vaultService.retrieveData(sessionToken, password);

            if(data!=null){
                outpLabel.setText("Decrypted : "+data);
            }else{
                outpLabel.setText("No data Found");
            }

        }catch(Exception e){
            outpLabel.setText("Error Retrieving Data");
            e.printStackTrace();
        }
    } 
}
