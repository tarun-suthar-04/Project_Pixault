package com.pixault.service;

import com.pixault.dao.VaultDAO;
import com.pixault.model.VaultItem;
import com.pixault.security.CryptoUtils;
import com.pixault.security.KeyDerivation;
import javax.crypto.SecretKey;

public class VaultService {

    private final VaultDAO vaultDAO=new VaultDAO();

    //save data
    public void storeData(String email,String password ,String plainText){
        try{
            byte[] salt=KeyDerivation.generateSalt();
            String saltStr=KeyDerivation.encodeSalt(salt);

            SecretKey key=KeyDerivation.deriveKey(password, salt);

            byte[] encrypted=CryptoUtils.encrypt(plainText.getBytes(), key);
            
            vaultDAO.saveData(email, encrypted,saltStr);

        }catch(Exception e){
            e.printStackTrace();

        }
    }
    //Retrieve data
    public String retrieveData(String email,String password){
        try{
            VaultItem item=vaultDAO.getData(email);
        
            if(item==null){ return null; }

            byte[] salt=KeyDerivation.decodeSalt(item.getSalt());
            SecretKey key=KeyDerivation.deriveKey(password, salt);

            byte[] decrypted=CryptoUtils.decrypt(item.getData(), key);
            System.out.println("Salt from DB: " + item.getSalt());

            return new String(decrypted);
        }catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }
    
}
