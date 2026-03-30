package com.pixault.service;

import java.time.LocalDateTime;
import com.pixault.dao.UserDAO;
import com.pixault.auth.SessionManager;
import com.pixault.dao.OTPDAO;
import com.pixault.model.User;
import com.pixault.security.PasswordUtil;
import com.pixault.util.OTPUtil;

public class AuthService {

    private final UserDAO userDAO=new UserDAO();
    private final OTPDAO otpDAO=new OTPDAO();
    
    public boolean emailExists(String email){
        return userDAO.emailExists(email);
     }
    
    public void sendRegistrationOTP(String email){
        String otp=OTPUtil.generateOTP();
        LocalDateTime expiry=LocalDateTime.now().plusMinutes(1);
        otpDAO.saveOTP(email, otp, expiry);
        EmailService.sendOTP(email, otp);
    }

    public boolean verifyOTP(String email,String otp){
        return otpDAO.verifyOTP(email,otp);
    }

    public boolean registerUSer(String email , String password){
        User user=new User(email, password);
        return userDAO.registerUser(user);
    }

    //login flow
    public boolean loginStep1(String email,String password){
        if(!userDAO.emailExists(email)){ return false; }
        String storedHash=userDAO.getPasswordHash(email);

        if(!PasswordUtil.verifyPassword(password, storedHash)){
            return false;
        }

        //send OTP 
        sendRegistrationOTP(email);
        return true;
    }

    //OTP verification + session
    public String loginStep2(String email , String otp){
        if(otpDAO.verifyOTP(email, otp)){
            return SessionManager.createSession(email);
        }
        return null;
    }
}
