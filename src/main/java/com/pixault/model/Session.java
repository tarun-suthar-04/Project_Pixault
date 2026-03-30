package com.pixault.model;

import java.time.LocalDateTime;

public class Session {
    private String email;
    private String token;
    private LocalDateTime createdAt;

    public Session(String email, String token){
        this.email=email;
        this.token=token;
        this.createdAt=LocalDateTime.now();
    }

    public String getEmail(){
        return email;
    }
    public String getToken(){
        return token;
    }
    public LocalDateTime getCreatedAt(){
        return createdAt;
    }

}
