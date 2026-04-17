package com.pixault.dao;

import com.pixault.config.DatabaseConfig;
import com.pixault.model.VaultItem;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;

public class VaultDAO {

    //save encrypted data
    public void saveData(String email,byte[] encryptedData,String salt){
        String sql="INSERT INTO vault(email,data,salt) VALUES(?,?,?)";

        try(Connection conn=DatabaseConfig.getConnection();
            PreparedStatement ps=conn.prepareStatement(sql)){
                ps.setString(1, email);
                ps.setBytes(2, encryptedData);
                ps.setString(3, salt);
                ps.executeUpdate();

            }catch(Exception e){
                e.printStackTrace();
            }
    }

    //Get Latest Data
    public VaultItem getData(String email){
        String sql="SELECT data, salt FROM vault WHERE email=? ORDER BY id DESC LIMIT 1";

        try(Connection conn= DatabaseConfig.getConnection();
            PreparedStatement ps=conn.prepareStatement(sql)){
                ps.setString(1, email);
                ResultSet rs=ps.executeQuery();

                if(rs.next()){
                    byte[] data=rs.getBytes("data");
                    String salt=rs.getString("salt");
                    return new VaultItem(email, data,salt);

                }
            }catch(Exception e){
                e.printStackTrace();
            }
            return null;
    }
    
}
