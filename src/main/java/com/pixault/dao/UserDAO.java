package com.pixault.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.pixault.config.DatabaseConfig;
import com.pixault.model.User;
import com.pixault.security.PasswordUtil;

public class UserDAO {

   //Register user
    public boolean registerUser(User user) {

        String sql = "INSERT INTO users(email,password) VALUES(?,?)";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            String hashedPassword = PasswordUtil.hashPassword(user.getPassword());

            ps.setString(1, user.getEmail());
            // ps.setString(2, PasswordUtil.hashPassword(user.getPassword()));
            ps.setString(2, hashedPassword);
            ps.executeUpdate();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    //Login user
    public boolean loginUser(String email , String password){
        String sql="SELECT password FROM users WHERE email=?";
        try(Connection conn=DatabaseConfig.getConnection();
            PreparedStatement ps=conn.prepareStatement(sql)){

                ps.setString(1, email);
                ResultSet rs=ps.executeQuery();
                if(rs.next()){
                    String storedHash=rs.getString("password");
                    return PasswordUtil.verifyPassword(password, storedHash);
                }

            }catch(Exception e){
                e.printStackTrace();
            }
            return false;
        }
    
    //Checkng email exist or not
    public boolean emailExists(String email){
        String sql="SELECT id FROM users WHERE email=?";
        
        try(Connection conn=DatabaseConfig.getConnection();
            PreparedStatement ps=conn.prepareStatement(sql)){

                ps.setString(1,email);
                ResultSet rs=ps.executeQuery();

                return rs.next();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;

    }
}