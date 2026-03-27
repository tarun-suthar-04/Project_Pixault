package com.pixault.security;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class KeyDerivation {
    public static final int ITERATIONS=65536;
    private static final int KEY_LENGTH=256;

    //generate random salt
    public static byte[] generateSalt(){
        byte[] salt=new byte[16];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    //Derive AES key from password + salt
    public static SecretKey derievKey(String password , byte[] salt) throws Exception{
        PBEKeySpec spec=new PBEKeySpec(password.toCharArray(), salt ,ITERATIONS, KEY_LENGTH);
        SecretKeyFactory factory=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        byte[] keyBytes=factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    //Utility (Store salt as string)
    public static String encodeSalt(byte[] salt){
        return Base64.getEncoder().encodeToString(salt);
    }
    public static byte[] decodeSalt(String salt){
        return Base64.getDecoder().decode(salt);
    }

}
