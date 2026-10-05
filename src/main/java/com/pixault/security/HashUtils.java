package com.pixault.security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Security;
import java.util.Base64;

/**
 * Cryptographic hash utilities for Pixault.
 *
 * <p>
 * Provides:
 * <ul>
 * <li>SHA-256 and SHA-512 digests</li>
 * <li>HMAC-SHA256 message authentication codes</li>
 * <li>Constant-time comparison to prevent timing attacks</li>
 * </ul>
 */
public final class HashUtils {

    private static final String SHA256 = "SHA-256";
    private static final String SHA512 = "SHA-512";
    private static final String HMAC_ALG = "HmacSHA256";

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private HashUtils() {
        throw new UnsupportedOperationException("HashUtils is a utility class.");
    }

    // =========================================================================
    // SHA-256
    // =========================================================================

    public static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance(SHA256).digest(data);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 failed", e);
        }
    }

    public static String sha256Hex(byte[] data) {
        return bytesToHex(sha256(data));
    }

    public static String sha256Hex(String text) {
        return sha256Hex(text.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256Base64(byte[] data) {
        return Base64.getEncoder().encodeToString(sha256(data));
    }

    // =========================================================================
    // SHA-512
    // =========================================================================

    public static byte[] sha512(byte[] data) {
        try {
            return MessageDigest.getInstance(SHA512).digest(data);
        } catch (Exception e) {
            throw new RuntimeException("SHA-512 failed", e);
        }
    }

    public static String sha512Hex(byte[] data) {
        return bytesToHex(sha512(data));
    }

    public static String sha512Hex(String text) {
        return sha512Hex(text.getBytes(StandardCharsets.UTF_8));
    }

    // =========================================================================
    // HMAC-SHA256
    // =========================================================================

    /**
     * Computes HMAC-SHA256 of the given data with the provided secret key.
     *
     * @param data      Data to authenticate
     * @param secretKey HMAC key bytes
     * @return Raw HMAC bytes
     */
    public static byte[] hmacSha256(byte[] data, byte[] secretKey) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(secretKey, HMAC_ALG));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256 failed", e);
        }
    }

    public static String hmacSha256Hex(byte[] data, byte[] secretKey) {
        return bytesToHex(hmacSha256(data, secretKey));
    }

    public static String hmacSha256Hex(String data, byte[] secretKey) {
        return hmacSha256Hex(data.getBytes(StandardCharsets.UTF_8), secretKey);
    }

    public static String hmacSha256Base64(byte[] data, byte[] secretKey) {
        return Base64.getEncoder().encodeToString(hmacSha256(data, secretKey));
    }

    // =========================================================================
    // Constant-time HMAC verification (prevents timing attacks)
    // =========================================================================

    /**
     * Verifies an HMAC-SHA256 value in constant time.
     * Uses {@link MessageDigest#isEqual} which is timing-safe.
     *
     * @param data         Data that was authenticated
     * @param expectedHmac Expected HMAC (hex string)
     * @param secretKey    HMAC key bytes
     * @return true if HMAC matches
     */
    public static boolean verifyHmac(byte[] data, String expectedHmac, byte[] secretKey) {
        if (expectedHmac == null)
            return false;
        byte[] computed = hmacSha256(data, secretKey);
        byte[] expected;
        try {
            expected = hexToBytes(expectedHmac);
        } catch (Exception e) {
            return false;
        }
        return MessageDigest.isEqual(computed, expected);
    }

    /**
     * Performs constant-time comparison of two byte arrays.
     */
    public static boolean constantTimeEquals(byte[] a, byte[] b) {
        return MessageDigest.isEqual(a, b);
    }

    /**
     * Performs constant-time comparison of two strings.
     */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null)
            return a == b;
        return constantTimeEquals(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }

    // =========================================================================
    // Hex utilities
    // =========================================================================

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hex string: " + hex);
        }
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
