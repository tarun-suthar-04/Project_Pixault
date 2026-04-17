package com.pixault;

import com.pixault.service.VaultService;

public class App {

    public static void main(String[] args) throws Exception {

        
        // testing vault 
        VaultService vault=new VaultService();
        vault.storeData("cdstarun1837@gmail.com", "1111", "Hello");
        String data=vault.retrieveData("cdstarun1837@gmail.com", "1111");
        // System.out.println("Salt from DB: " + item.getSalt());
        System.out.println("data : "+data);
       

    }
}
