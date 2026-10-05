package com.pixault.share;

import com.pixault.auth.DeviceFingerprint;
import com.pixault.config.AppConfig;
import com.pixault.database.ShareDAO;
import com.pixault.model.AuditLog;
import com.pixault.model.VaultShare;
import com.pixault.security.HashUtils;
import com.pixault.stego.StegoEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.ArrayList;

/**
 * One-time secure vault sharing service.
 *
 * <p>
 * Security guarantees:
 * <ul>
 * <li>Each share link is single-use (consumed on first access)</li>
 * <li>Links are HMAC-SHA256 signed to prevent forgery</li>
 * <li>Links expire after a configurable TTL</li>
 * <li>Access is bound to the device fingerprint of the first accessor</li>
 * <li>Replay attacks are prevented by the consumed flag</li>
 * </ul>
 */
public class ShareService {

    private static final Logger log = LoggerFactory.getLogger(ShareService.class);
    private static final Path MESSAGE_DIR = Path.of("share_messages");

    private final ShareDAO shareDAO;
    private final byte[] hmacKey;
    private final String shareBaseUrl;

    public ShareService() {
        this.shareDAO = new ShareDAO();
        Properties config = AppConfig.snapshot();
        this.hmacKey = AppConfig.get("token.hmac.secret", "CHANGE_ME_USE_A_LONG_RANDOM_SECRET")
                .getBytes(StandardCharsets.UTF_8);
        this.shareBaseUrl = resolveShareBaseUrl(config);
    }

    // =========================================================================
    // Create Share
    // =========================================================================

    /**
     * Creates a one-time share link for a stego image blob.
     *
     * @param ownerId        User ID of the vault owner
     * @param stegoImagePath Path to the steganography image file
     * @param ttlMinutes     Time-to-live in minutes
     * @return The full share URL containing the share ID
     * @throws IOException  if blob file cannot be read
     * @throws SQLException on DB error
     */
    public String createShare(int ownerId, Path stegoImagePath, int ttlMinutes)
            throws IOException, SQLException {
        return createShare(ownerId, stegoImagePath, ttlMinutes, null);
    }

    /**
     * Creates a one-time share link and optionally attaches a decrypted message
     * preview if a correct stego password is provided.
     */
    public String createShare(int ownerId, Path stegoImagePath, int ttlMinutes, char[] stegoPassword)
            throws IOException, SQLException {
        if (!Files.exists(stegoImagePath)) {
            throw new IOException("Stego image not found: " + stegoImagePath);
        }

        String shareId = UUID.randomUUID().toString();
        LocalDateTime exp = LocalDateTime.now().plusMinutes(ttlMinutes);
        // Sign with stable fields that do not change during DB roundtrips.
        String metadata = shareId + ":" + ownerId;
        String signature = HashUtils.hmacSha256Hex(
                metadata.getBytes(StandardCharsets.UTF_8), hmacKey);

        VaultShare share = new VaultShare(
                shareId, ownerId, stegoImagePath.toAbsolutePath().toString(), exp, signature);
        shareDAO.insertShare(share);

        if (stegoPassword != null && stegoPassword.length > 0) {
            try {
                byte[] stegoBytes = Files.readAllBytes(stegoImagePath);
                String decrypted = StegoEngine.extractAndDecrypt(stegoBytes, stegoPassword);
                saveOneTimeMessage(shareId, decrypted);
            } catch (StegoEngine.StegoException e) {
                throw new IOException("Invalid decryption password for selected stego image.", e);
            }
        }

        String link = shareBaseUrl + "/share?id=" + shareId;
        logAudit(ownerId, AuditLog.SHARE_CREATED, null, "Share created: " + shareId);

        log.info("Share created: shareId={}, ownerId={}, ttl={}min, path={}",
                shareId, ownerId, ttlMinutes, stegoImagePath);
        return link;
    }

    // =========================================================================
    // Access Share
    // =========================================================================

    /**
     * Accesses a one-time share. On first access, binds to device and marks
     * consumed.
     * Subsequent accesses by any device are denied.
     *
     * @param shareId UUID of the share
     * @return Bytes of the stego image
     * @throws ShareException on invalid, expired, consumed, or tampered share
     * @throws IOException    on file read error
     * @throws SQLException   on DB error
     */
    public ShareAccessResult accessShare(String shareId) throws ShareException, IOException, SQLException {
        String deviceFingerprint = DeviceFingerprint.generate();

        Optional<VaultShare> opt = shareDAO.findShare(shareId);
        if (opt.isEmpty()) {
            throw new ShareException("Share not found or has been removed.");
        }

        VaultShare share = opt.get();

        // 1. Check if consumed
        if (share.isConsumed()) {
            logAudit(share.getOwnerId(), AuditLog.SHARE_ACCESSED,
                    deviceFingerprint, "Rejected: share already consumed");
            throw new ShareException("This share link has already been used. Access denied.");
        }

        // 2. Check expiry
        if (share.isExpired()) {
            logAudit(share.getOwnerId(), AuditLog.SHARE_EXPIRED,
                    deviceFingerprint, "Rejected: share expired");
            throw new ShareException("This share link has expired.");
        }

        // 3. Verify HMAC signature
        if (!isValidSignature(shareId, share)) {
            throw new ShareException("Share link signature is invalid. Possible tampering detected.");
        }

        // 4. Read the blob
        Path blobPath = Path.of(share.getEncryptedBlobPath());
        if (!Files.exists(blobPath)) {
            throw new ShareException("Shared file no longer exists on the server.");
        }
        byte[] blob = Files.readAllBytes(blobPath);
        String message = loadAndDeleteOneTimeMessage(shareId);

        // 5. Mark consumed and bind to device (anti-forward)
        shareDAO.markConsumed(shareId, deviceFingerprint);
        logAudit(share.getOwnerId(), AuditLog.SHARE_ACCESSED, deviceFingerprint,
                "Share accessed successfully: " + shareId);

        log.info("Share accessed: shareId={}, device={}", shareId, deviceFingerprint.substring(0, 8));
        return new ShareAccessResult(blob, message);
    }

    // =========================================================================
    // List shares for user
    // =========================================================================

    public List<VaultShare> getSharesForUser(int ownerId) throws SQLException {
        return shareDAO.findSharesByOwner(ownerId);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void logAudit(int userId, String event, String device, String details) {
        try {
            shareDAO.insertAuditLog(new AuditLog(userId, event, null, device, details));
        } catch (Exception e) {
            log.warn("Audit log failed: {}", e.getMessage());
        }
    }

    private String resolveShareBaseUrl(Properties config) {
        String preferred = config.getProperty("share.public.url", "").trim();
        String base = preferred.isBlank() ? config.getProperty("app.base.url", "http://localhost:8088") : preferred;
        try {
            URI uri = URI.create(base);
            String host = uri.getHost();
            if (host == null || host.isBlank()) {
                return base;
            }
            if ("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)) {
                String localIp = detectLocalIpv4();
                if (localIp != null) {
                    URI rebuilt = new URI(
                            uri.getScheme(),
                            uri.getUserInfo(),
                            localIp,
                            uri.getPort(),
                            uri.getPath(),
                            null,
                            null);
                    String url = rebuilt.toString();
                    if (url.endsWith("/")) {
                        url = url.substring(0, url.length() - 1);
                    }
                    log.info("share.public.url not set; using LAN URL {}", url);
                    return url;
                }
            }
            return base;
        } catch (Exception e) {
            return base;
        }
    }

    private boolean isValidSignature(String shareId, VaultShare share) {
        List<String> candidates = new ArrayList<>();

        // Current stable format.
        candidates.add(shareId + ":" + share.getOwnerId());

        // Previous format (epoch-based).
        long expEpoch = share.getExpiryTime().toEpochSecond(ZoneOffset.UTC);
        candidates.add(shareId + ":" + share.getOwnerId() + ":" + expEpoch);

        // Legacy format (string-based LocalDateTime).
        candidates.add(shareId + ":" + share.getOwnerId() + ":" + share.getExpiryTime());

        for (String c : candidates) {
            if (HashUtils.verifyHmac(c.getBytes(StandardCharsets.UTF_8), share.getHmacSignature(), hmacKey)) {
                return true;
            }
        }
        return false;
    }

    private String detectLocalIpv4() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }
                var addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    var addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private void saveOneTimeMessage(String shareId, String message) throws IOException {
        if (message == null) {
            return;
        }
        Files.createDirectories(MESSAGE_DIR);
        Files.writeString(MESSAGE_DIR.resolve(shareId + ".txt"), message, StandardCharsets.UTF_8);
    }

    private String loadAndDeleteOneTimeMessage(String shareId) {
        try {
            Path p = MESSAGE_DIR.resolve(shareId + ".txt");
            if (!Files.exists(p)) {
                return null;
            }
            String msg = Files.readString(p, StandardCharsets.UTF_8);
            Files.deleteIfExists(p);
            return msg;
        } catch (Exception e) {
            return null;
        }
    }

    // =========================================================================
    // Exception
    // =========================================================================

    public static class ShareException extends Exception {
        public ShareException(String message) {
            super(message);
        }
    }

    public static class ShareAccessResult {
        private final byte[] imageBytes;
        private final String decryptedMessage;

        public ShareAccessResult(byte[] imageBytes, String decryptedMessage) {
            this.imageBytes = imageBytes;
            this.decryptedMessage = decryptedMessage;
        }

        public byte[] getImageBytes() {
            return imageBytes;
        }

        public String getDecryptedMessage() {
            return decryptedMessage;
        }
    }
}
