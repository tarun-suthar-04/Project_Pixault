package com.pixault.security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;
import java.util.Base64;

/**
 * Production-grade AES-256-GCM cryptographic utilities.
 *
 * <p>
 * Features:
 * <ul>
 * <li>AES-256-GCM authenticated encryption (AEAD)</li>
 * <li>12-byte random IV (NIST recommended)</li>
 * <li>128-bit GCM authentication tag</li>
 * <li>Secure in-memory key wiping</li>
 * <li>BouncyCastle provider registration</li>
 * </ul>
 *
 * <p>
 * <b>NEVER store raw passwords. NEVER store raw encryption keys.</b>
 */
public final class CryptoUtils {

    private static final Logger log = LoggerFactory.getLogger(CryptoUtils.class);

    public static final String ALGORITHM = "AES";
    public static final String TRANSFORMATION = "AES/GCM/NoPadding";
    public static final int KEY_SIZE_BITS = 256;
    public static final int KEY_SIZE_BYTES = KEY_SIZE_BITS / 8;
    public static final int IV_SIZE_BYTES = 12; // NIST recommends 12 for GCM
    public static final int TAG_SIZE_BITS = 128; // Full 128-bit auth tag

    private static final SecureRandom SECURE_RANDOM;

    static {
        // Register BouncyCastle as the cryptography provider
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
        SECURE_RANDOM = new SecureRandom();
    }

    // ===== Private constructor – utility class =====
    private CryptoUtils() {
        throw new UnsupportedOperationException("CryptoUtils is a utility class.");
    }

    // =========================================================================
    // Encryption
    // =========================================================================

    /**
     * Encrypts plaintext bytes using AES-256-GCM.
     *
     * @param plaintext The data to encrypt (NOT null/empty)
     * @param keyBytes  32-byte AES key (wiped by caller after use)
     * @return IV (12 bytes) + ciphertext + auth tag, Base64-encoded
     * @throws PixaultCryptoException on any cryptographic failure
     */
    public static String encrypt(byte[] plaintext, byte[] keyBytes) throws PixaultCryptoException {
        if (plaintext == null || plaintext.length == 0) {
            throw new PixaultCryptoException("Plaintext cannot be null or empty.");
        }
        validateKeySize(keyBytes);

        byte[] iv = generateIV();
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            SecretKey secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
            GCMParameterSpec paramSpec = new GCMParameterSpec(TAG_SIZE_BITS, iv);

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, paramSpec);
            byte[] ciphertext = cipher.doFinal(plaintext);

            // Prepend IV to ciphertext: [IV (12)] + [ciphertext + tag]
            byte[] combined = new byte[IV_SIZE_BYTES + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, IV_SIZE_BYTES);
            System.arraycopy(ciphertext, 0, combined, IV_SIZE_BYTES, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new PixaultCryptoException("Encryption failed: " + e.getMessage(), e);
        } finally {
            wipeBytes(iv);
        }
    }

    /**
     * Decrypts data produced by {@link #encrypt(byte[], byte[])}.
     *
     * @param encryptedBase64 Base64-encoded IV + ciphertext
     * @param keyBytes        32-byte AES key
     * @return Original plaintext bytes
     * @throws PixaultCryptoException on failure or authentication tag mismatch
     */
    public static byte[] decrypt(String encryptedBase64, byte[] keyBytes) throws PixaultCryptoException {
        if (encryptedBase64 == null || encryptedBase64.isBlank()) {
            throw new PixaultCryptoException("Encrypted data cannot be null or empty.");
        }
        validateKeySize(keyBytes);

        try {
            byte[] combined = Base64.getDecoder().decode(encryptedBase64);
            if (combined.length <= IV_SIZE_BYTES) {
                throw new PixaultCryptoException("Ciphertext is too short to be valid.");
            }

            byte[] iv = Arrays.copyOfRange(combined, 0, IV_SIZE_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(combined, IV_SIZE_BYTES, combined.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            SecretKey secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
            GCMParameterSpec paramSpec = new GCMParameterSpec(TAG_SIZE_BITS, iv);

            cipher.init(Cipher.DECRYPT_MODE, secretKey, paramSpec);
            return cipher.doFinal(ciphertext);
        } catch (PixaultCryptoException e) {
            throw e;
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new PixaultCryptoException("Decryption failed (possible tampering or wrong key): " + e.getMessage(),
                    e);
        }
    }

    // =========================================================================
    // Raw bytes encryption (used internally by StegoEngine)
    // =========================================================================

    /**
     * Returns raw encrypted bytes (IV + ciphertext), NOT Base64-encoded.
     * Used by StegoEngine for direct byte-level embedding.
     */
    public static byte[] encryptRaw(byte[] plaintext, byte[] keyBytes) throws PixaultCryptoException {
        validateKeySize(keyBytes);
        byte[] iv = generateIV();
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyBytes, ALGORITHM),
                    new GCMParameterSpec(TAG_SIZE_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext);

            byte[] result = new byte[IV_SIZE_BYTES + ciphertext.length];
            System.arraycopy(iv, 0, result, 0, IV_SIZE_BYTES);
            System.arraycopy(ciphertext, 0, result, IV_SIZE_BYTES, ciphertext.length);
            return result;
        } catch (Exception e) {
            throw new PixaultCryptoException("Raw encryption failed: " + e.getMessage(), e);
        } finally {
            wipeBytes(iv);
        }
    }

    /**
     * Decrypts raw bytes (IV + ciphertext) produced by {@link #encryptRaw}.
     */
    public static byte[] decryptRaw(byte[] ivAndCiphertext, byte[] keyBytes) throws PixaultCryptoException {
        validateKeySize(keyBytes);
        try {
            byte[] iv = Arrays.copyOfRange(ivAndCiphertext, 0, IV_SIZE_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(ivAndCiphertext, IV_SIZE_BYTES, ivAndCiphertext.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, ALGORITHM),
                    new GCMParameterSpec(TAG_SIZE_BITS, iv));
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new PixaultCryptoException("Raw decryption failed: " + e.getMessage(), e);
        }
    }

    // =========================================================================
    // Key & IV utilities
    // =========================================================================

    /**
     * Generates a cryptographically secure 32-byte (256-bit) AES key.
     * Caller MUST wipe the returned array after use via {@link #wipeBytes(byte[])}.
     */
    public static byte[] generateKey() {
        byte[] key = new byte[KEY_SIZE_BYTES];
        SECURE_RANDOM.nextBytes(key);
        return key;
    }

    /**
     * Generates a cryptographically secure 12-byte GCM IV.
     */
    public static byte[] generateIV() {
        byte[] iv = new byte[IV_SIZE_BYTES];
        SECURE_RANDOM.nextBytes(iv);
        return iv;
    }

    /**
     * Generates a cryptographically secure salt of the requested length.
     */
    public static byte[] generateSalt(int lengthBytes) {
        byte[] salt = new byte[lengthBytes];
        SECURE_RANDOM.nextBytes(salt);
        return salt;
    }

    /**
     * Returns the shared SecureRandom instance (strong entropy).
     */
    public static SecureRandom getSecureRandom() {
        return SECURE_RANDOM;
    }

    // =========================================================================
    // Secure memory wiping
    // =========================================================================

    /**
     * Overwrites the byte array with zeros to wipe sensitive data from memory.
     * Call this immediately after use of any cryptographic material.
     */
    public static void wipeBytes(byte[] data) {
        if (data != null) {
            Arrays.fill(data, (byte) 0x00);
        }
    }

    /**
     * Overwrites a char array with null characters to wipe passwords from memory.
     */
    public static void wipeChars(char[] data) {
        if (data != null) {
            Arrays.fill(data, '\u0000');
        }
    }

    // =========================================================================
    // Validation
    // =========================================================================

    private static void validateKeySize(byte[] keyBytes) throws PixaultCryptoException {
        if (keyBytes == null || keyBytes.length != KEY_SIZE_BYTES) {
            throw new PixaultCryptoException(
                    "Invalid AES key size. Expected " + KEY_SIZE_BYTES + " bytes, got "
                            + (keyBytes == null ? "null" : keyBytes.length) + ".");
        }
    }

    // =========================================================================
    // Exception
    // =========================================================================

    /**
     * Checked exception for all cryptographic failures.
     */
    public static class PixaultCryptoException extends Exception {
        public PixaultCryptoException(String message) {
            super(message);
        }

        public PixaultCryptoException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
