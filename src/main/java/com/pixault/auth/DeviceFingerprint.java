package com.pixault.auth;

import com.pixault.security.HashUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

/**
 * Captures hardware/OS identifiers and produces a stable device fingerprint.
 *
 * <p>
 * Collected info:
 * <ul>
 * <li>OS name + version</li>
 * <li>MAC address of first non-loopback network interface (SHA-256 hashed)</li>
 * <li>Machine UUID derived from OS-specific identifiers</li>
 * </ul>
 *
 * <p>
 * The fingerprint is a SHA-256 hex digest of all combined attributes.
 */
public final class DeviceFingerprint {

    private static final Logger log = LoggerFactory.getLogger(DeviceFingerprint.class);

    private DeviceFingerprint() {
        throw new UnsupportedOperationException("DeviceFingerprint is a utility class.");
    }

    /**
     * Generates a deterministic SHA-256 device fingerprint.
     *
     * @return 64-character hex string uniquely identifying this device
     */
    public static String generate() {
        StringBuilder raw = new StringBuilder();

        // OS identifiers
        raw.append("OS:").append(System.getProperty("os.name", "unknown")).append("|");
        raw.append("VER:").append(System.getProperty("os.version", "unknown")).append("|");
        raw.append("ARCH:").append(System.getProperty("os.arch", "unknown")).append("|");
        raw.append("USER:").append(System.getProperty("user.name", "unknown")).append("|");

        // MAC address (first non-loopback interface)
        String macHash = getMacAddressHash();
        raw.append("MAC:").append(macHash).append("|");

        // JVM identifier (somewhat stable per installation)
        raw.append("JVM:").append(System.getProperty("java.home", "unknown")).append("|");

        return HashUtils.sha256Hex(raw.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Returns a short 8-character display label for the fingerprint.
     */
    public static String getDisplayLabel() {
        String fp = generate();
        return fp.substring(0, 8).toUpperCase();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private static String getMacAddressHash() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp())
                    continue;
                byte[] mac = ni.getHardwareAddress();
                if (mac != null && mac.length > 0) {
                    return HashUtils.sha256Hex(mac);
                }
            }
        } catch (SocketException e) {
            log.warn("Could not retrieve MAC address: {}", e.getMessage());
        }
        // Fallback: hash of user.name + os.name (less unique but safe)
        String fallback = System.getProperty("user.name", "") + System.getProperty("os.name", "");
        return HashUtils.sha256Hex(fallback.getBytes(StandardCharsets.UTF_8));
    }
}
