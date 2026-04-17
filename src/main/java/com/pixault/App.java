package com.pixault;

import com.pixault.service.VaultService;

import java.util.Scanner;

import com.pixault.service.AuthService;

public class App {

    public static void main(String[] args) {

        AuthService auth = new AuthService();
        VaultService vault = new VaultService();
        Scanner sc=new Scanner(System.in);

        try {
            String email="cdstarun1837@gmail.com";
            String password="1234";

            boolean step1=auth.loginStep1(email, password);
            if(!step1){
                System.out.println("Login failed(Wrong email/password)");
                return;
            }
            System.out.println("Enter OTP : ");
            String otp=sc.nextLine();

            String sessionToken=auth.loginStep2(email, otp);

            if(sessionToken==null){
                System.out.println("Invalid OTP");
            }
            System.out.println("Login Successful . Session : "+sessionToken);

            vault.storeData(sessionToken, password, "Hello hii how are you");
            String data=vault.retrieveData(sessionToken, password);
            System.out.println("Decrypted Data : "+data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}