package com.pixault.database;

import com.pixault.model.AuditLog;
import com.pixault.model.VaultShare;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for vault sharing and audit logging.
 */
public class ShareDAO {

    private static final Logger log = LoggerFactory.getLogger(ShareDAO.class);

    // =========================================================================
    // Vault Shares
    // =========================================================================

    public void insertShare(VaultShare share) throws SQLException {
        String sql = "INSERT INTO vault_shares "
                + "(share_id, owner_id, encrypted_blob_path, expiry_time, device_fingerprint, consumed, hmac_signature) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, share.getShareId());
            ps.setInt(2, share.getOwnerId());
            ps.setString(3, share.getEncryptedBlobPath());
            ps.setString(4, share.getExpiryTime().toString());
            ps.setString(5, share.getDeviceFingerprint());
            ps.setInt(6, share.isConsumed() ? 1 : 0);
            ps.setString(7, share.getHmacSignature());
            ps.executeUpdate();
            log.info("Share inserted: shareId={}", share.getShareId());
        }
    }

    public Optional<VaultShare> findShare(String shareId) throws SQLException {
        String sql = "SELECT * FROM vault_shares WHERE share_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, shareId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return Optional.of(mapShareRow(rs));
            }
        }
        return Optional.empty();
    }

    public void markConsumed(String shareId, String deviceFingerprint) throws SQLException {
        String sql = "UPDATE vault_shares SET consumed = 1, device_fingerprint = ? WHERE share_id = ?";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, deviceFingerprint);
            ps.setString(2, shareId);
            ps.executeUpdate();
            log.info("Share consumed: shareId={}", shareId);
        }
    }

    public List<VaultShare> findSharesByOwner(int ownerId) throws SQLException {
        String sql = "SELECT * FROM vault_shares WHERE owner_id = ? ORDER BY created_at DESC";
        List<VaultShare> shares = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ownerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    shares.add(mapShareRow(rs));
            }
        }
        return shares;
    }


    // =========================================================================
    // Audit Logs
    // =========================================================================

    public void insertAuditLog(AuditLog entry) throws SQLException {
        String sql = "INSERT INTO audit_logs (user_id, event_type, ip_address, device_info, details) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            if (entry.getUserId() != null)
                ps.setInt(1, entry.getUserId());
            else
                ps.setNull(1, Types.INTEGER);
            ps.setString(2, entry.getEventType());
            ps.setString(3, entry.getIpAddress());
            ps.setString(4, entry.getDeviceInfo());
            ps.setString(5, entry.getDetails());
            ps.executeUpdate();
        }
    }

    public List<AuditLog> findAuditLogs(Integer userId, int limit) throws SQLException {
        String sql = userId != null
                ? "SELECT * FROM audit_logs WHERE user_id = ? ORDER BY timestamp DESC LIMIT ?"
                : "SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT ?";
        List<AuditLog> logs = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId != null) {
                ps.setInt(1, userId);
                ps.setInt(2, limit);
            } else {
                ps.setInt(1, limit);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AuditLog al = new AuditLog();
                    al.setId(rs.getLong("id"));
                    int uid = rs.getInt("user_id");
                    al.setUserId(rs.wasNull() ? null : uid);
                    al.setEventType(rs.getString("event_type"));
                    al.setIpAddress(rs.getString("ip_address"));
                    al.setDeviceInfo(rs.getString("device_info"));
                    al.setDetails(rs.getString("details"));
                    al.setTimestamp(parseDateTime(rs.getString("timestamp")));
                    logs.add(al);
                }
            }
        }
        return logs;
    }

    // =========================================================================
    // Mapping
    // =========================================================================

    private VaultShare mapShareRow(ResultSet rs) throws SQLException {
        VaultShare vs = new VaultShare();
        vs.setShareId(rs.getString("share_id"));
        vs.setOwnerId(rs.getInt("owner_id"));
        vs.setEncryptedBlobPath(rs.getString("encrypted_blob_path"));
        vs.setExpiryTime(parseDateTime(rs.getString("expiry_time")));
        vs.setDeviceFingerprint(rs.getString("device_fingerprint"));
        vs.setConsumed(rs.getInt("consumed") == 1);
        vs.setHmacSignature(rs.getString("hmac_signature"));
        vs.setCreatedAt(parseDateTime(rs.getString("created_at")));
        return vs;
    }

    private LocalDateTime parseDateTime(String dt) {
        if (dt == null) return null;
        return LocalDateTime.parse(dt.replace(" ", "T"));
    }
}
