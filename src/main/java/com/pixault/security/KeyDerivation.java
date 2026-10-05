package com.pixault.security;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Advanced;
import de.mkammerer.argon2.Argon2Factory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * Key Derivation Functions for Pixault.
 *
 * <p>
 * Primary KDF: Argon2id (winner of Password Hashing Competition).
 * <p>
 * Fallback KDF: PBKDF2-HMAC-SHA-512 (NIST-approved).
 *
 * <p>
 * Argon2id parameters (OWASP recommended minimums):
 * <ul>
 * <li>Memory: 64 MB</li>
 * <li>Iterations: 3</li>
 * <li>Parallelism: 4</li>
 * <li>Output: 32 bytes (256-bit AES key)</li>
 * </ul>
 *
 * <p>
 * <b>Passwords MUST be passed as char[] and wiped after use.</b>
 */
public final class KeyDerivation {

    private static final Logger log = LoggerFactory.getLogger(KeyDerivation.class);

    // Argon2id parameters
    public static final int ARGON2_MEMORY_KB = 65536; // 64 MB
    public static final int ARGON2_ITERATIONS = 3;
    public static final int ARGON2_PARALLELISM = 4;
    public static final int DERIVED_KEY_BYTES = 32; // 256-bit output

    // PBKDF2 parameters (fallback)
    public static final int PBKDF2_ITERATIONS = 600_000; // OWASP 2023 recommendation
    public static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA512";

    static {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private KeyDerivation() {
        throw new UnsupportedOperationException("KeyDerivation is a utility class.");
    }

    // =========================================================================
    // Argon2id – Primary KDF
    // =========================================================================

    /**
     * Derives a 256-bit AES key from a password using Argon2id.
     *
     * @param password Password as char[] (will NOT be wiped here – caller's
     *                 responsibility)
     * @param salt     32-byte random salt
     * @return 32-byte derived key (caller must wipe after use)
     */
    public static byte[] deriveKeyArgon2id(char[] password, byte[] salt) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password cannot be null or empty.");
        }
        if (salt == null || salt.length < 16) {
            throw new IllegalArgumentException("Salt must be at least 16 bytes.");
        }

        Argon2Advanced argon2 = (Argon2Advanced) Argon2Factory.createAdvanced(Argon2Factory.Argon2Types.ARGON2id);
        byte[] derivedKey;

        try {
            // rawHash(iterations, memory, parallelism, password char[], charset, salt)
            derivedKey = argon2.rawHash(
                    ARGON2_ITERATIONS,
                    ARGON2_MEMORY_KB,
                    ARGON2_PARALLELISM,
                    password,
                    StandardCharsets.UTF_8,
                    salt);
        } catch (Exception e) {
            log.error("Argon2id key derivation failed", e);
            throw new RuntimeException("Argon2id key derivation failed: " + e.getMessage(), e);
        }

        return derivedKey;
    }

    /**
     * Hashes a password for storage using Argon2id (returns encoded string).
     * The encoded string includes the salt – use {@link #verifyArgon2id} to verify.
     *
     * @param password char[] password (will NOT be wiped)
     * @return Argon2id encoded hash string (includes parameters + salt)
     */
    public static String hashPasswordArgon2id(char[] password) {
        Argon2 argon2 = Argon2Factory.createAdvanced(Argon2Factory.Argon2Types.ARGON2id);
        try {
            return argon2.hash(ARGON2_ITERATIONS, ARGON2_MEMORY_KB, ARGON2_PARALLELISM, password);
        } finally {
            // argon2 library handles internal cleanup
        }
    }

    /**
     * Verifies a password against a stored Argon2id hash.
     *
     * @param hash     Encoded hash string from {@link #hashPasswordArgon2id}
     * @param password char[] password to verify
     * @return true if password matches
     */
    public static boolean verifyArgon2id(String hash, char[] password) {
        if (hash == null || password == null)
            return false;
        Argon2 argon2 = Argon2Factory.createAdvanced(Argon2Factory.Argon2Types.ARGON2id);
        return argon2.verify(hash, password);
    }

    // =========================================================================
    // PBKDF2 – Fallback KDF
    // =========================================================================

    /**
     * Derives a 256-bit AES key from a password using PBKDF2-HMAC-SHA-512.
     * Use only as a fallback when Argon2id is unavailable.
     *
     * @param password Password as char[]
     * @param salt     32-byte random salt
     * @return 32-byte derived key (caller must wipe after use)
     */
    public static byte[] deriveKeyPBKDF2(char[] password, byte[] salt) {
        if (password == null || password.length == 0) {
            throw new IllegalArgumentException("Password cannot be null or empty.");
        }
        try {
            KeySpec spec = new PBEKeySpec(
                    password,
                    salt,
                    PBKDF2_ITERATIONS,
                    DERIVED_KEY_BYTES * 8 // bits
            );
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            log.error("PBKDF2 key derivation failed", e);
            throw new RuntimeException("PBKDF2 key derivation failed: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // Salt utilities
    // =========================================================================

    /**
     * Generates a secure 32-byte (256-bit) random salt.
     */
    public static byte[] generateSalt() {
        return CryptoUtils.generateSalt(32);
    }

    /**
     * Encodes salt to Base64 for database storage.
     */
    public static String encodeSalt(byte[] salt) {
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * Decodes salt from Base64 string.
     */
    public static byte[] decodeSalt(String encodedSalt) {
        return Base64.getDecoder().decode(encodedSalt);
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    /**
     * Converts char[] to byte[] using UTF-8 encoding.
     * The returned byte array should be wiped after use.
     */
    @SuppressWarnings("unused")
    private static byte[] charToBytes(char[] chars) {
        java.nio.charset.Charset charset = java.nio.charset.StandardCharsets.UTF_8;
        java.nio.CharBuffer charBuffer = java.nio.CharBuffer.wrap(chars);
        java.nio.ByteBuffer byteBuffer = charset.encode(charBuffer);
        byte[] bytes = Arrays.copyOfRange(
                byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
        // Wipe the ByteBuffer backing array
        Arrays.fill(byteBuffer.array(), (byte) 0);
        return bytes;
    }
}
