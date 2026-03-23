package com.pixault.security;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.security.SecureRandom;
import java.util.Arrays;

public class CryptoUtils {
    
    private static final String ALGO="AES/GCM/NoPadding";
    private static final int KEY_SIZE=256;
    private static final int IV_SIZE=12;
    private static final int TAG_SIZE=128;

    //Generate AES key 
public static SecretKey generateKey() throws Exception{
    KeyGenerator keyGen=KeyGenerator.getInstance("AES");
    keyGen.init(KEY_SIZE);
    return keyGen.generateKey();
}

//Encrypt
public static byte[] encrypt(byte[] data, SecretKey key) throws Exception{
    byte[] iv=new byte[IV_SIZE];
    SecureRandom random=new SecureRandom();
    random.nextBytes(iv);

    Cipher cipher=Cipher.getInstance(ALGO);
    GCMParameterSpec spec=new GCMParameterSpec(TAG_SIZE, iv);
    cipher.init(Cipher.ENCRYPT_MODE,key,spec);
    byte[] encrypted=cipher.doFinal(data);

    //combine IV + encrypted
    byte[] result=new byte[IV_SIZE+encrypted.length];

    System.arraycopy(iv, 0, result, 0, IV_SIZE);
    System.arraycopy(encrypted, 0, result, IV_SIZE, encrypted.length);

    return result;

}

//Decrypt
public static byte[] decrypt(byte[] encryptedData,SecretKey key) throws Exception{
    byte[] iv=Arrays.copyOfRange(encryptedData,0,IV_SIZE);
    byte[] cipherText=Arrays.copyOfRange(encryptedData, IV_SIZE, encryptedData.length);

    Cipher cipher=Cipher.getInstance(ALGO);
    GCMParameterSpec spec=new GCMParameterSpec(TAG_SIZE, iv);
    cipher.init(Cipher.DECRYPT_MODE,key,spec);
    return cipher.doFinal(cipherText);

}

}
