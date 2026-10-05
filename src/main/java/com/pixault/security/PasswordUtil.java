package com.pixault.security;

import org.mindrot.jbcrypt.BCrypt;;

public class PasswordUtil {

    public static String hashPassword(String Password) {
        return BCrypt.hashpw(Password, BCrypt.gensalt());
    }

    public static boolean verifyPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }

}
