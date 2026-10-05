package com.pixault.stego;

import com.pixault.security.CryptoUtils;
import com.pixault.security.HashUtils;
import com.pixault.security.KeyDerivation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Pixault Steganography Engine.
 *
 * <p>
 * Security architecture:
 * <ul>
 * <li><b>Encryption</b>: AES-256-GCM with Argon2id-derived key</li>
 * <li><b>Integrity</b>: SHA-256 hash prepended before encryption</li>
 * <li><b>Pixel placement</b>: Deterministic Fisher-Yates PRNG seeded from
 * SHA-256(password + image_width_bytes + image_height_bytes)</li>
 * <li><b>LSB encoding</b>: 3 bits per pixel across R, G, B channels</li>
 * <li><b>Length prefix</b>: 32-bit int embedded at positions [0..10]</li>
 * </ul>
 *
 * <p>
 * <b>NOT sequential</b> – pixels are scattered pseudo-randomly for security.
 * <p>
 * <b>Payload = SHA-256(payload) + payload</b> – integrity verified on
 * extraction.
 */
public final class StegoEngine {

    private static final Logger log = LoggerFactory.getLogger(StegoEngine.class);

    /** Length of SHA-256 hash prefix appended for integrity check (32 bytes). */
    private static final int HASH_PREFIX_SIZE = 32;

    /** Number of bits per pixel (1 bit per R, G, B channel = 3 bits/pixel). */
    private static final int BITS_PER_PIXEL = 3;

    private StegoEngine() {
        throw new UnsupportedOperationException("StegoEngine is a utility class.");
    }

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Encrypts the payload and hides it inside a cover image using random pixel
     * scattering.
     *
     * @param coverImageBytes Raw bytes of a PNG/JPEG cover image
     * @param payload         Plaintext string to hide
     * @param password        Password (char[], wiped internally)
     * @return Modified PNG image bytes containing the hidden data
     * @throws StegoException on insufficient capacity or crypto failure
     */
    public static byte[] encryptAndHide(byte[] coverImageBytes, String payload, char[] password)
            throws StegoException {
        if (coverImageBytes == null || coverImageBytes.length == 0)
            throw new StegoException("Cover image cannot be null or empty.");
        if (payload == null || payload.isBlank())
            throw new StegoException("Payload cannot be null or empty.");
        if (password == null || password.length == 0)
            throw new StegoException("Password cannot be null or empty.");

        byte[] keyBytes = null;
        byte[] saltBytes = null;

        try {
            BufferedImage image = readImage(coverImageBytes);
            int width = image.getWidth();
            int height = image.getHeight();

            // 1. Derive AES key with Argon2id
            saltBytes = KeyDerivation.generateSalt();
            keyBytes = KeyDerivation.deriveKeyArgon2id(password, saltBytes);

            // 2. Compute SHA-256 integrity hash of payload
            byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
            byte[] integrityHash = HashUtils.sha256(payloadBytes);

            // 3. Prepend hash to payload: [32-byte hash] + [payload bytes]
            byte[] withHash = new byte[HASH_PREFIX_SIZE + payloadBytes.length];
            System.arraycopy(integrityHash, 0, withHash, 0, HASH_PREFIX_SIZE);
            System.arraycopy(payloadBytes, 0, withHash, HASH_PREFIX_SIZE, payloadBytes.length);

            // 4. AES-256-GCM encrypt
            byte[] encrypted = CryptoUtils.encryptRaw(withHash, keyBytes);

            // 5. Prepend salt (32 bytes) to encrypted output so it can be recovered
            // Final embed: [32 salt] + [12 IV + ciphertext + 16 tag]
            byte[] saltAndEncrypted = new byte[32 + encrypted.length];
            System.arraycopy(saltBytes, 0, saltAndEncrypted, 0, 32);
            System.arraycopy(encrypted, 0, saltAndEncrypted, 32, encrypted.length);

            // 6. Check image capacity
            int totalBitsNeeded = (4 + saltAndEncrypted.length) * 8; // 4 bytes = length prefix
            int availableBits = width * height * BITS_PER_PIXEL;
            if (totalBitsNeeded > availableBits) {
                throw new StegoException(String.format(
                        "Image too small. Need %d bits, image holds %d bits. Use a larger image.",
                        totalBitsNeeded, availableBits));
            }

            // 7. Build shuffled pixel position list.
            // PRNG seeded from SHA-256(password + width + height) — stable across
            // embed and extract (image pixel hash would change after LSB embedding).
            List<int[]> positions = buildShuffledPositions(width, height, password);

            // 8. Convert payload to bits and embed via LSB
            byte[] dataToEmbed = prependLength(saltAndEncrypted);
            image = embedBits(image, dataToEmbed, positions);

            // 9. Write back to PNG
            return writeImage(image);

        } catch (StegoException e) {
            throw e;
        } catch (Exception e) {
            log.error("encryptAndHide failed", e);
            throw new StegoException("Failed to hide data: " + e.getMessage(), e);
        } finally {
            CryptoUtils.wipeBytes(keyBytes);
            CryptoUtils.wipeBytes(saltBytes);
        }
    }

    /**
     * Extracts and decrypts hidden data from a steganographic image.
     *
     * @param stegoImageBytes Raw bytes of the stego image
     * @param password        Password used during hiding
     * @return Recovered plaintext payload
     * @throws StegoException on decryption failure, integrity mismatch, or wrong
     *                        password
     */
    public static String extractAndDecrypt(byte[] stegoImageBytes, char[] password)
            throws StegoException {
        if (stegoImageBytes == null || stegoImageBytes.length == 0)
            throw new StegoException("Stego image cannot be null or empty.");
        if (password == null || password.length == 0)
            throw new StegoException("Password cannot be null or empty.");

        byte[] keyBytes = null;
        try {
            BufferedImage image = readImage(stegoImageBytes);
            int width = image.getWidth();
            int height = image.getHeight();

            // 1. Rebuild the same shuffled position list.
            // Must use width + height only (same stable inputs as embed).
            List<int[]> positions = buildShuffledPositions(width, height, password);

            // 3. Extract length prefix (first 4 bytes = 32 bits = up to ~11 pixels worth)
            int dataLength = extractLength(image, positions);
            if (dataLength <= 0 || dataLength > width * height * BITS_PER_PIXEL / 8) {
                throw new StegoException("Invalid embedded data length. Wrong image or password.");
            }

            // 4. Extract embedded bytes (after the 4-byte length prefix)
            byte[] saltAndEncrypted = extractBits(image, 4, dataLength, positions);

            // 5. Split salt (first 32 bytes) from ciphertext
            if (saltAndEncrypted.length <= 32)
                throw new StegoException("Extracted data too short. Corrupted image.");
            byte[] salt = Arrays.copyOfRange(saltAndEncrypted, 0, 32);
            byte[] ciphertext = Arrays.copyOfRange(saltAndEncrypted, 32, saltAndEncrypted.length);

            // 6. Derive AES key from password + extracted salt
            keyBytes = KeyDerivation.deriveKeyArgon2id(password, salt);

            // 7. Decrypt
            byte[] decrypted = CryptoUtils.decryptRaw(ciphertext, keyBytes);

            // 8. Verify integrity: first 32 bytes = SHA-256 of the rest
            if (decrypted.length <= HASH_PREFIX_SIZE)
                throw new StegoException("Decrypted data too short.");
            byte[] storedHash = Arrays.copyOfRange(decrypted, 0, HASH_PREFIX_SIZE);
            byte[] payloadBytes = Arrays.copyOfRange(decrypted, HASH_PREFIX_SIZE, decrypted.length);
            byte[] actualHash = HashUtils.sha256(payloadBytes);

            if (!MessageDigest.isEqual(storedHash, actualHash)) {
                throw new StegoException(
                        "⚠ Data Corruption or Tampering Detected – integrity check failed.");
            }

            return new String(payloadBytes, StandardCharsets.UTF_8);

        } catch (StegoException e) {
            throw e;
        } catch (CryptoUtils.PixaultCryptoException e) {
            throw new StegoException("⚠ Decryption failed – wrong password or corrupted data.", e);
        } catch (Exception e) {
            log.error("extractAndDecrypt failed", e);
            throw new StegoException("Failed to extract data: " + e.getMessage(), e);
        } finally {
            CryptoUtils.wipeBytes(keyBytes);
        }
    }

    // =========================================================================
    // PRNG & Pixel Positions
    // =========================================================================

    /**
     * Generates a deterministic shuffled list of pixel (x, y) positions.
     *
     * <p>
     * PRNG seed = SHA-256(password_bytes + width_int_bytes + height_int_bytes).
     * Image dimensions are constant before and after LSB embedding, so the seed
     * is identical during both {@code encryptAndHide} and
     * {@code extractAndDecrypt}.
     *
     * <p>
     * Uses Java's seeded {@link java.util.Random} with Fisher-Yates shuffle for
     * full determinism.
     */
    private static List<int[]> buildShuffledPositions(int width, int height, char[] password) {
        // Build a list of all pixel positions
        List<int[]> positions = new ArrayList<>(width * height);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                positions.add(new int[] { x, y });
            }
        }

        // Compute deterministic seed from password + image dimensions.
        // Dimensions are stable: they do NOT change when LSBs are modified.
        byte[] passBytes = new String(password).getBytes(StandardCharsets.UTF_8);
        byte[] dimBytes = ByteBuffer.allocate(8).putInt(width).putInt(height).array();
        byte[] seedInput = new byte[passBytes.length + dimBytes.length];
        System.arraycopy(passBytes, 0, seedInput, 0, passBytes.length);
        System.arraycopy(dimBytes, 0, seedInput, passBytes.length, dimBytes.length);
        byte[] seedHash = HashUtils.sha256(seedInput);
        Arrays.fill(passBytes, (byte) 0); // wipe

        // Convert first 8 bytes of hash to long seed
        long seed = ByteBuffer.wrap(seedHash).getLong();

        // Fisher-Yates shuffle with seeded java.util.Random (deterministic)
        java.util.Random rng = new java.util.Random(seed);
        for (int i = positions.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int[] tmp = positions.get(i);
            positions.set(i, positions.get(j));
            positions.set(j, tmp);
        }
        return positions;
    }

    // =========================================================================
    // LSB Embedding / Extraction
    // =========================================================================

    /**
     * Prepends a 4-byte big-endian length prefix to the data.
     */
    private static byte[] prependLength(byte[] data) {
        byte[] result = new byte[4 + data.length];
        result[0] = (byte) (data.length >> 24);
        result[1] = (byte) (data.length >> 16);
        result[2] = (byte) (data.length >> 8);
        result[3] = (byte) (data.length);
        System.arraycopy(data, 0, result, 4, data.length);
        return result;
    }

    /**
     * Extracts the 4-byte big-endian length prefix using shuffled positions.
     */
    private static int extractLength(BufferedImage image, List<int[]> positions) {
        byte[] lenBytes = extractBits(image, 0, 4, positions);
        return ByteBuffer.wrap(lenBytes).getInt();
    }

    /**
     * Embeds all bytes of {@code data} into the image using LSB on R, G, B
     * channels.
     * Uses positions[offset..] from the shuffled list.
     */
    private static BufferedImage embedBits(BufferedImage image, byte[] data, List<int[]> positions) {
        int posIdx = 0;
        int bitIdx = 0; // current bit position within current byte
        int dataIdx = 0; // current byte in data

        int totalBits = data.length * 8;
        int bitsWritten = 0;

        while (bitsWritten < totalBits && posIdx < positions.size()) {
            int[] pos = positions.get(posIdx++);
            int x = pos[0], y = pos[1];
            int argb = image.getRGB(x, y);

            int alpha = (argb >> 24) & 0xFF;
            int r = (argb >> 16) & 0xFF;
            int g = (argb >> 8) & 0xFF;
            int b = (argb) & 0xFF;

            // Embed 1 bit in R channel LSB
            if (bitsWritten < totalBits && dataIdx < data.length) {
                int bit = (data[dataIdx] >> (7 - bitIdx)) & 1;
                r = (r & 0xFE) | bit;
                bitsWritten++;
                if (++bitIdx == 8) {
                    bitIdx = 0;
                    dataIdx++;
                }
            }

            // Embed 1 bit in G channel LSB
            if (bitsWritten < totalBits && dataIdx < data.length) {
                int bit = (data[dataIdx] >> (7 - bitIdx)) & 1;
                g = (g & 0xFE) | bit;
                bitsWritten++;
                if (++bitIdx == 8) {
                    bitIdx = 0;
                    dataIdx++;
                }
            }

            // Embed 1 bit in B channel LSB
            if (bitsWritten < totalBits && dataIdx < data.length) {
                int bit = (data[dataIdx] >> (7 - bitIdx)) & 1;
                b = (b & 0xFE) | bit;
                bitsWritten++;
                if (++bitIdx == 8) {
                    bitIdx = 0;
                    dataIdx++;
                }
            }

            image.setRGB(x, y, (alpha << 24) | (r << 16) | (g << 8) | b);
        }
        return image;
    }

    /**
     * Extracts {@code byteCount} bytes starting at byte-offset
     * {@code startOffsetBytes} in the embedded bitstream using shuffled positions
     * and LSB on R, G, B channels.
     */
    private static byte[] extractBits(BufferedImage image, int startOffset, int byteCount,
            List<int[]> positions) {
        byte[] result = new byte[byteCount];
        int totalBits = byteCount * 8;
        int bitsRead = 0;
        int dataIdx = 0;
        int bitIdx = 0;

        // Convert byte offset to bit offset in the embedded stream.
        int startBit = startOffset * 8;
        // We compute which position we start at
        int posIdx = startBit / BITS_PER_PIXEL;
        int channelOffset = startBit % BITS_PER_PIXEL; // 0=R,1=G,2=B

        while (bitsRead < totalBits && posIdx < positions.size()) {
            int[] pos = positions.get(posIdx++);
            int argb = image.getRGB(pos[0], pos[1]);

            int r = (argb >> 16) & 1;
            int g = (argb >> 8) & 1;
            int b = (argb) & 1;
            int[] bits = { r, g, b };

            for (int ch = channelOffset; ch < 3 && bitsRead < totalBits; ch++) {
                result[dataIdx] = (byte) ((result[dataIdx] << 1) | bits[ch]);
                bitsRead++;
                if (++bitIdx == 8) {
                    bitIdx = 0;
                    dataIdx++;
                }
            }
            channelOffset = 0; // only skip channels on the very first pixel
        }
        return result;
    }

    // =========================================================================
    // Image Utilities
    // =========================================================================

    private static BufferedImage readImage(byte[] bytes) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
        if (img == null)
            throw new IOException("Unrecognised image format.");
        
        // Convert paletted/indexed images to TYPE_INT_ARGB to allow arbitrary RGB values.
        // We DO NOT convert standard true-color/alpha formats (like TYPE_4BYTE_ABGR) 
        // using Graphics.drawImage() because alpha-compositing will erase the RGB values
        // of fully transparent pixels, destroying the embedded hidden data!
        int type = img.getType();
        if (type == BufferedImage.TYPE_BYTE_INDEXED || type == BufferedImage.TYPE_BYTE_BINARY || type == BufferedImage.TYPE_CUSTOM) {
            BufferedImage converted = new BufferedImage(
                    img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            converted.getGraphics().drawImage(img, 0, 0, null);
            return converted;
        }
        return img;
    }

    private static byte[] writeImage(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }

    // =========================================================================
    // Exception
    // =========================================================================

    public static class StegoException extends Exception {
        public StegoException(String message) {
            super(message);
        }

        public StegoException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
