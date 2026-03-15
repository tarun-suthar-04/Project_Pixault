package com.pixault.util;

import java.util.Random;

public class OTPUtil {
    public static String generateOTP(){
        Random rand=new Random();
        int otp=1000 + rand.nextInt(9000);
        return String.valueOf(otp);
    }
   
    
}
