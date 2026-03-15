package com.pixault.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;

import com.pixault.config.DatabaseConfig;
import com.pixault.model.User;
import com.pixault.security.PasswordUtil;

public class UserDAO {

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
}