package com.pixault.model;

public class VaultItem {

    private String email;
    private byte[] data;
    private String salt;

    public VaultItem(String email, byte[] data, String salt) {
        this.email = email;
        this.data = data;
        this.salt = salt;

    }

    public String getEmail() {
        return email;
    }

    public byte[] getData() {
        return data;
    }

    public String getSalt() {
        return salt;
    }

}
