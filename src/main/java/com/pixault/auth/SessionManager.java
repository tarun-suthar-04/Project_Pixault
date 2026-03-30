package com.pixault.auth;

import com.pixault.model.Session;
import java.util.HashMap;
import java.util.UUID;

public class SessionManager {
    private static final HashMap<String , Session> sessions=new HashMap<>();

    //create Session
    public static String createSession(String email){
        String token=UUID.randomUUID().toString();
        Session session=new Session(email, token);
        sessions.put(token, session);
        return token;
    }

    //validate session
    public static boolean isValid(String token){
        return sessions.containsKey(token);
    }

    //get Session
    public static Session getSession(String token){
        return sessions.get(token);
    }

    //logout
    public static void invalidate(String token){
        sessions.remove(token);
    }
    
}
