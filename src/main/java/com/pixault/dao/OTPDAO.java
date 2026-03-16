package com.pixault.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;

import com.pixault.config.DatabaseConfig;

public class OTPDAO {
    public void saveOTP(String email, String otp, LocalDateTime expiry) {
        String sql = "INSERT INTO otp_codes(email,otp,expiry) VALUES(?,?,?)";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, otp);
            ps.setString(3, expiry.toString());

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean verifyOTP(String email, String otp) {
        String sql = "SELECT expiry FROM otp_codes WHERE email=? AND otp=?";

        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, email);
            ps.setString(2, otp);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                LocalDateTime expiry = LocalDateTime.parse(rs.getString("expiry"));
                String update = "UPDATE otp_codes SET used=1 WHERE email=? AND otp=?";
                PreparedStatement upd = conn.prepareStatement(update);
                upd.setString(1, email);
                upd.setString(2, otp);
                upd.executeUpdate();

                return LocalDateTime.now().isBefore(expiry);

            }

        } catch (Exception e) {
            e.printStackTrace();

        }
        return false;
    }

    // private void markOTPUsed(Connection conn , String email, String otp){
    // String update="UPDATE otp_codes SET used=1 WHERE eamil=? AND otp=?";

    // }

    public void deleteExpiredOTP() {
        String sql = "DELETE FROM otp_codes WHERE expiry < datetime('now')";
        try (Connection conn = DatabaseConfig.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
