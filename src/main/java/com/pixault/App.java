package com.pixault;

// import java.util.Scanner;
// import com.pixault.service.AuthService;
import javax.crypto.SecretKey;
import com.pixault.security.CryptoUtils;

public class App {

    public static void main(String[] args) throws Exception {

    //Testing CryptoUtils
    String msg="Hello PIXAULT";

    SecretKey key=CryptoUtils.generateKey();

    byte[] encrypted=CryptoUtils.encrypt(msg.getBytes(), key);
    byte[] decrypted=CryptoUtils.decrypt(encrypted, key);

    System.out.println("Original message : "+msg);
    System.out.println("Encrypted message : "+new String(encrypted));
    System.out.println("Decrypted message : "+new String(decrypted));



    //     // user registration from console
    //     Scanner sc = new Scanner(System.in);
        
    //     AuthService auth=new AuthService();

    //     System.out.print("Enter email : ");
    //     String email = sc.nextLine();

    //     if(auth.emailExists(email)){
    //         System.out.println("Email already registered");
    //         sc.close();
    //         return;

    //     }
    //     System.out.print("Enter password : ");
    //     String password = sc.nextLine();

    //     System.out.print("Confirm password : ");
    //     String confirmPassword = sc.nextLine();

    //     if(!password.equals(confirmPassword)){
    //         System.out.println("Password doesn't match");
    //         sc.close();
    //         return;
    //     }

    //    //Send and verify OTP using AuthService
    //    auth.sendRegistrationOTP(email);
    //    System.out.println("OTP sent to email"+email);
       
    //    System.out.print("Enter OTP : ");
    //    String otp=sc.nextLine();

    //    if(auth.verifyOTP(email, otp)){
    //     auth.registerUSer(email, confirmPassword);
    //     System.out.println("Registration Successfully");
    //    }else{
    //     System.out.println("Invalid OTP or Expired");
    //    }
    //    sc.close();
        
    }
}
