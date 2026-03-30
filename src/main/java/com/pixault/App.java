package com.pixault;

import java.util.Scanner;
import com.pixault.service.AuthService;

public class App {

    public static void main(String[] args) throws Exception {

        // Testing Authentication + Session Modulee

        Scanner sc = new Scanner(System.in);

        AuthService auth = new AuthService();
        System.out.print("Enter email: ");
        String email = sc.nextLine();

        System.out.print("Enter password: ");
        String password = sc.nextLine();

        if (!auth.loginStep1(email, password)) {
            System.out.println("Invalid credentials");
            sc.close();
            return;
        }

        System.out.println("OTP sent");

        System.out.print("Enter OTP: ");
        String otp = sc.nextLine();

        String sessionToken = auth.loginStep2(email, otp);

        if (sessionToken != null) {

            System.out.println("Login successful");
            System.out.println("Session Token: " + sessionToken);

        } else {
            System.out.println("Invalid OTP");
        }

        sc.close();

    }
}
