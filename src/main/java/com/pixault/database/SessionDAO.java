package com.pixault.database;

import com.pixault.model.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Data Access Object for the sessions table.
 */
public class SessionDAO {

    private static final Logger log = LoggerFactory.getLogger(SessionDAO.class);

    public void insertSession(Session session) throws SQLException {
        String sql = "INSERT INTO sessions (session_id, user_id, device_fingerprint, expires_at, hmac_signature, revoked) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, session.getSessionId());
            ps.setInt(2, session.getUserId());
            ps.setString(3, session.getDeviceFingerprint());
            ps.setString(4, session.getExpiresAt().toString());
            ps.setString(5, session.getHmacSignature());
            ps.setInt(6, session.isRevoked() ? 1 : 0);
            ps.executeUpdate();
        }
    }

    public Optional<Session> findSession(String sessionId) throws SQLException {
        String sql = "SELECT * FROM sessions WHERE session_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps =prepareStatement(conn, sql)) {
            ps.setString(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        }
        return Optional.empty();
    }

    public void revokeSession(String sessionId) throws SQLException {
        String sql = "UPDATE sessions SET revoked = 1 WHERE session_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = prepareStatement(conn, sql)) {
            ps.setString(1, sessionId);
            ps.executeUpdate();
        }
    }

    public void revokeAllUserSessions(int userId) throws SQLException {
        String sql = "UPDATE sessions SET revoked = 1 WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = prepareStatement(conn, sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    private PreparedStatement prepareStatement(Connection conn, String sql) throws SQLException {
        return conn.prepareStatement(sql);
    }

    private Session mapRow(ResultSet rs) throws SQLException {
        Session s = new Session();
        s.setSessionId(rs.getString("session_id"));
        s.setUserId(rs.getInt("user_id"));
        s.setDeviceFingerprint(rs.getString("device_fingerprint"));
        s.setExpiresAt(parseDateTime(rs.getString("expires_at")));
        s.setHmacSignature(rs.getString("hmac_signature"));
        s.setRevoked(rs.getInt("revoked") == 1);
        s.setCreatedAt(parseDateTime(rs.getString("created_at")));
        return s;
    }

    private LocalDateTime parseDateTime(String dt) {
        if (dt == null) return null;
        return LocalDateTime.parse(dt.replace(" ", "T"));
    }
}
