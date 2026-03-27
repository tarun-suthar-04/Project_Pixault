package com.pixault;

// import java.util.Scanner;
// import com.pixault.service.AuthService;
// import javax.crypto.SecretKey;
// import com.pixault.security.CryptoUtils;
// import com.pixault.security.KeyDerivation;
import com.pixault.security.PasswordUtil;

public class App {

    public static void main(String[] args) throws Exception {

    //Testing Argon2 
    String password="StrongPassword@1234";
    String hash=PasswordUtil.hashPassword(password);
    System.out.println("Hash : "+hash);
    boolean result=PasswordUtil.verifyPassword(password, hash);
    System.out.println("Match : "+ result);
    



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
