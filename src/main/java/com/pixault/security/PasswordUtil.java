package com.pixault.security;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;

public class PasswordUtil {

    private static final Argon2 argon2=Argon2Factory.create();

    //Hash Password
    public static String hashPassword(String password){
        return argon2.hash(
                        3,      //iteration
                        65536,  //memory(KB)=64MB
                        1,      //paralellism
                        password.toCharArray());
    }

    //verify password
    public static boolean verifyPassword(String password , String hash){
        return argon2.verify(hash, password.toCharArray());
    }

}
