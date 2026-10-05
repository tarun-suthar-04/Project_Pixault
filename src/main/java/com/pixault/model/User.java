package com.pixault.model;

import java.time.LocalDateTime;

/**
 * User domain model.
 */
public class User {

    private int id;
    private String username;
    private String email;
    private String passwordHash;
    private String salt;
    private boolean emailVerified;
    private String otpSecret;
    private int failedLoginAttempts;
    private LocalDateTime loginLockedUntil;
    private LocalDateTime createdAt;

    public User() {
    }

    public User(String username, String email, String passwordHash, String salt) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.salt = salt;
        this.emailVerified = false;
        this.failedLoginAttempts = 0;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getOtpSecret() {
        return otpSecret;
    }

    public void setOtpSecret(String otpSecret) {
        this.otpSecret = otpSecret;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(int failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }

    public LocalDateTime getLoginLockedUntil() {
        return loginLockedUntil;
    }

    public void setLoginLockedUntil(LocalDateTime loginLockedUntil) {
        this.loginLockedUntil = loginLockedUntil;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isLocked() {
        return loginLockedUntil != null && LocalDateTime.now().isBefore(loginLockedUntil);
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', email='" + email
                + "', emailVerified=" + emailVerified + ", failedLoginAttempts=" + failedLoginAttempts + "}";
    }
}
