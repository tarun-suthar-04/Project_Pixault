package com.pixault.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Data Access Object for the upgraded otp_codes table.
 */
public class OTPDAO {

    private static final Logger log = LoggerFactory.getLogger(OTPDAO.class);

    public void insertOtp(int userId, String otpHash, String purpose, LocalDateTime expiresAt) throws SQLException {
        // Invalidate previous unconsumed OTPs for same user and purpose
        String invalidate = "UPDATE otp_codes SET consumed = 1 WHERE user_id = ? AND purpose = ? AND consumed = 0";
        String insert = "INSERT INTO otp_codes (user_id, otp_hash, purpose, expires_at) VALUES (?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(invalidate)) {
                    ps.setInt(1, userId);
                    ps.setString(2, purpose);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = conn.prepareStatement(insert)) {
                    ps.setInt(1, userId);
                    ps.setString(2, otpHash);
                    ps.setString(3, purpose);
                    ps.setString(4, expiresAt.toString());
                    ps.executeUpdate();
                }
                conn.commit();
                log.info("Inserted OTP for userId={} purpose={}", userId, purpose);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public Optional<OtpRecord> findLatestValidOtp(int userId, String purpose) throws SQLException {
        String sql = "SELECT * FROM otp_codes WHERE user_id = ? AND purpose = ? AND consumed = 0 "
                   + "ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, purpose);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public void incrementAttempts(int otpId) throws SQLException {
        String sql = "UPDATE otp_codes SET attempts = attempts + 1 WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, otpId);
            ps.executeUpdate();
        }
    }

    public void markConsumed(int otpId) throws SQLException {
        String sql = "UPDATE otp_codes SET consumed = 1 WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, otpId);
            ps.executeUpdate();
        }
    }

    private OtpRecord mapRow(ResultSet rs) throws SQLException {
        OtpRecord rec = new OtpRecord();
        rec.id = rs.getInt("id");
        rec.userId = rs.getInt("user_id");
        rec.otpHash = rs.getString("otp_hash");
        rec.purpose = rs.getString("purpose");
        rec.expiresAt = parseDateTime(rs.getString("expires_at"));
        rec.attempts = rs.getInt("attempts");
        rec.consumed = rs.getInt("consumed") == 1;
        return rec;
    }

    private LocalDateTime parseDateTime(String dt) {
        if (dt == null) return null;
        return LocalDateTime.parse(dt.replace(" ", "T"));
    }

    public static class OtpRecord {
        public int id;
        public int userId;
        public String otpHash;
        public String purpose;
        public LocalDateTime expiresAt;
        public int attempts;
        public boolean consumed;

        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }
}
