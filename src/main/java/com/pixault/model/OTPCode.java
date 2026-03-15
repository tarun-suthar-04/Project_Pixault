package com.pixault.model;

import java.time.LocalDateTime;

public class OTPCode {
    private String email;
    private String otp;
    private LocalDateTime expiry;

    public OTPCode(String email,String otp , LocalDateTime expiry){
        this.email=email;
        this.otp=otp;
        this.expiry=expiry;
    }

    public String getEmail(){
        return email;
    }

    public String getOtp(){
        return otp;
    }

    public LocalDateTime getExpiry(){
        return expiry;
    }
    
}
