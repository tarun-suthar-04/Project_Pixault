package com.pixault.auth;

import com.pixault.config.AppConfig;
import com.pixault.database.SessionDAO;
import com.pixault.model.Session;
import com.pixault.security.HashUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages authenticated user sessions.
 *
 * <p>
 * Sessions are:
 * <ul>
 * <li>UUID-based</li>
 * <li>Bound to a device fingerprint</li>
 * <li>HMAC-SHA256 signed (tamper-evident)</li>
 * <li>Expire after a configurable TTL (default: 2 hours)</li>
 * </ul>
 *
 * <p>
 * In-memory store (production deployments should persist to Redis or DB).
 */
public final class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    private static final int SESSION_TTL_HOURS = AppConfig.getInt("session.ttl.hours", 2);

    private static final SessionDAO sessionDAO = new SessionDAO();

    /** HMAC key for signing sessions (loaded from config in production) */
    private static final byte[] HMAC_KEY = loadHmacKey();

    private SessionManager() {
    }

    // =========================================================================
    // Session creation
    // =========================================================================

    /**
     * Creates a new authenticated session for the given user and persists it.
     */
    public static Session createSession(int userId, String username, String deviceFingerprint) {
        String sessionId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(SESSION_TTL_HOURS);

        String dataToSign = sessionId + ":" + userId + ":" + deviceFingerprint + ":" + expiresAt;
        String hmac = HashUtils.hmacSha256Hex(
                dataToSign.getBytes(StandardCharsets.UTF_8), HMAC_KEY);

        Session session = new Session(sessionId, userId, username, deviceFingerprint, expiresAt, hmac);
        
        try {
            sessionDAO.insertSession(session);
        } catch (Exception e) {
            log.error("Failed to persist session for userId={}", userId, e);
        }

        log.info("Session created and persisted: userId={}, sessionId={}", userId, sessionId);
        return session;
    }

    // =========================================================================
    // Session validation
    // =========================================================================

    /**
     * Validates a session: checks expiry, revocation, HMAC integrity, and device binding.
     */
    public static Session validateSession(String sessionId, String deviceFingerprint) {
        try {
            Optional<Session> opt = sessionDAO.findSession(sessionId);
            if (opt.isEmpty()) {
                log.warn("Session not found in DB: {}", sessionId);
                return null;
            }
            Session session = opt.get();
            if (session.isRevoked()) {
                log.warn("Session is revoked: {}", sessionId);
                return null;
            }
            if (session.isExpired()) {
                log.warn("Session expired: {}", sessionId);
                return null;
            }
            if (!HashUtils.constantTimeEquals(session.getDeviceFingerprint(), deviceFingerprint)) {
                log.warn("Device fingerprint mismatch for session: {}", sessionId);
                return null;
            }
            if (!verifySessionHmac(session)) {
                log.warn("HMAC validation failed for session: {}", sessionId);
                return null;
            }
            return session;
        } catch (Exception e) {
            log.error("Error validating session: {}", sessionId, e);
            return null;
        }
    }

    // =========================================================================
    // Session invalidation
    // =========================================================================

    /**
     * Invalidates a specific session (revocation).
     */
    public static void invalidateSession(String sessionId) {
        try {
            sessionDAO.revokeSession(sessionId);
            log.info("Session revoked: sessionId={}", sessionId);
        } catch (Exception e) {
            log.error("Failed to revoke session: {}", sessionId, e);
        }
    }

    /**
     * Invalidates ALL sessions for a given user.
     */
    public static void invalidateAllSessions(int userId) {
        try {
            sessionDAO.revokeAllUserSessions(userId);
            log.info("All sessions revoked for userId={}", userId);
        } catch (Exception e) {
            log.error("Failed to revoke all sessions for userId={}", userId, e);
        }
    }

    public static void purgeExpiredSessions() {
        // Implementation for purging old sessions from DB could go here
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static boolean verifySessionHmac(Session session) {
        String dataToVerify = session.getSessionId() + ":" + session.getUserId()
                + ":" + session.getDeviceFingerprint() + ":" + session.getExpiresAt();
        return HashUtils.verifyHmac(
                dataToVerify.getBytes(StandardCharsets.UTF_8),
                session.getHmacSignature(),
                HMAC_KEY);
    }

    private static byte[] loadHmacKey() {
        return AppConfig.get("token.hmac.secret", "CHANGE_ME_USE_A_LONG_RANDOM_SECRET")
                .getBytes(StandardCharsets.UTF_8);
    }
}
